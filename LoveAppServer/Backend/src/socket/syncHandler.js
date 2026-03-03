// src/socket/syncHandler.js – Full real-time sync via Socket.IO
//
// Protocol:
//  Client → Server: 'data-change'  { entityType, action, data, clientTimestamp }
//  Client → Server: 'sync-request' { sinceTimestamp }
//  Server → Client: 'data-change'  { entityType, action, data, serverTimestamp }
//  Server → Client: 'sync-response'{ events: [...] }
//  Server → Client: 'error'        { message }

const pool = require('../config/db');
const { publish, subscribe } = require('../config/redis');
const logger = require('../config/logger');
const { sendPushToPartner } = require('../utils/fcm');
const { activeSocketConnections } = require('../config/metrics');

// ─── Valid entity types and their DB tables / primary-key columns ────────────
const ENTITY_MAP = {
  note:          { table: 'notes',                  ownerCol: 'user_id', partnerRead: true  },
  wish:          { table: 'wishes',                 ownerCol: 'user_id', partnerRead: true  },
  mood:          { table: 'mood_entries',           ownerCol: 'user_id', partnerRead: true  },
  activity:      { table: 'activity_logs',          ownerCol: 'user_id', partnerRead: true  },
  cycle:         { table: 'menstrual_cycles',       ownerCol: 'user_id', partnerRead: true, femaleOnly: true },
  calendar:      { table: 'custom_calendars',       ownerCol: 'user_id', partnerRead: true  },
  // events live in custom_calendar_events (no direct user_id — owned via calendar)
  event:         { table: 'custom_calendar_events', ownerCol: null,      partnerRead: true, calendarOwned: true },
  relationship:  { table: 'relationship_info',      ownerCol: 'user_id', partnerRead: true  },
};

// ─── Allowed write columns per entity (prevents SQL-injection via column names) ─
const ENTITY_ALLOWED_COLUMNS = {
  note:         new Set(['title', 'content', 'is_private', 'tags', 'due_date', 'image_url']),
  wish:         new Set(['title', 'description', 'priority', 'category', 'is_completed', 'is_private', 'image_urls', 'emoji', 'due_date', 'completed_at']),
  mood:         new Set(['mood_type', 'date', 'note', 'metadata']),
  activity:     new Set(['title', 'description', 'event_date', 'category', 'activity_type', 'duration_minutes', 'start_time', 'note', 'image_urls']),
  cycle:        new Set(['cycle_start_date', 'cycle_duration', 'period_duration', 'symptoms', 'mood', 'notes']),
  calendar:     new Set(['name', 'description', 'type', 'color_hex', 'icon']),
  event:        new Set(['calendar_id', 'event_date', 'title', 'description', 'event_type', 'image_url', 'marked_date']),
  relationship: new Set(['relationship_start_date', 'first_kiss_date', 'anniversary_date', 'my_birthday', 'partner_birthday', 'nickname1', 'nickname2', 'notes']),
};

// ─── Helper: get the couple's room name ─────────────────────────────────────
function coupleRoom(coupleKey) {
  return `couple:${coupleKey}`;
}

// ─── Look up a user's coupleKey from DB ──────────────────────────────────────
async function getCoupleKey(userId) {
  const rel = await pool.query(
    'SELECT partner_user_id FROM relationship_info WHERE user_id = $1 LIMIT 1',
    [userId]
  );
  const partnerId = rel.rows[0]?.partner_user_id;
  if (!partnerId) return `solo_${userId}`;
  const min = Math.min(userId, partnerId);
  const max = Math.max(userId, partnerId);
  return `${min}_${max}`;
}

// ─── Access control: can this userId write to this entity record? ─────────────
async function canWrite(userId, entityType, entityId) {
  const meta = ENTITY_MAP[entityType];
  if (!meta) return false;

  // femaleOnly entities (cycle) require gender='female'
  if (meta.femaleOnly) {
    const res = await pool.query("SELECT gender FROM users WHERE id = $1", [userId]);
    if (res.rows[0]?.gender !== 'female') return false;
  }

  if (!entityId) return true; // creating new record

  // Events are owned via their parent calendar
  if (meta.calendarOwned) {
    const res = await pool.query(
      `SELECT cc.user_id AS owner
       FROM custom_calendar_events cce
       JOIN custom_calendars cc ON cce.calendar_id = cc.id
       WHERE cce.id = $1`,
      [entityId]
    );
    if (!res.rows[0]) return false;
    return res.rows[0].owner === userId;
  }

  const res = await pool.query(
    `SELECT ${meta.ownerCol} AS owner FROM ${meta.table} WHERE id = $1`,
    [entityId]
  );
  if (!res.rows[0]) return false;
  return res.rows[0].owner === userId;
}

// ─── Fetch all changes since a given timestamp ───────────────────────────────
async function fetchChangesSince(userId, coupleKey, sinceTimestamp) {
  const events = [];
  const since  = sinceTimestamp ? new Date(sinceTimestamp) : new Date(0);

  // We need partner id to fetch partner data
  const relRes = await pool.query(
    'SELECT partner_user_id FROM relationship_info WHERE user_id = $1 LIMIT 1',
    [userId]
  );
  const partnerId = relRes.rows[0]?.partner_user_id;
  const userIds   = partnerId ? [userId, partnerId] : [userId];

  for (const [entityType, meta] of Object.entries(ENTITY_MAP)) {
    try {
      let queryResult;

      if (meta.calendarOwned) {
        // custom_calendar_events has no user_id — join via custom_calendars
        queryResult = await pool.query(
          `SELECT cce.* FROM custom_calendar_events cce
           JOIN custom_calendars cc ON cce.calendar_id = cc.id
           WHERE cc.user_id = ANY($2::bigint[])
             AND cce.server_updated_at > $1
           ORDER BY cce.server_updated_at ASC
           LIMIT 500`,
          [since, userIds]
        );
      } else {
        const placeholders = userIds.map((_, i) => `$${i + 2}`).join(', ');
        queryResult = await pool.query(
          `SELECT * FROM ${meta.table}
           WHERE ${meta.ownerCol} IN (${placeholders})
             AND server_updated_at > $1
           ORDER BY server_updated_at ASC
           LIMIT 500`,
          [since, ...userIds]
        );
      }

      for (const row of queryResult.rows) {
        events.push({
          entityType,
          action:          row.deleted_at ? 'delete' : 'upsert',
          data:            row,
          serverTimestamp: row.server_updated_at?.toISOString?.() || new Date().toISOString(),
        });
      }
    } catch (err) {
      // Column may not exist yet on older tables — skip gracefully
      logger.warn(`fetchChangesSince: skipping ${entityType}: ${err.message}`);
    }
  }

  return events.sort((a, b) => new Date(a.serverTimestamp) - new Date(b.serverTimestamp));
}

// ─── Emit a data-change to all clients in the couple room ────────────────────
function emitToCouple(io, coupleKey, senderId, payload) {
  io.to(coupleRoom(coupleKey))
    .except(senderId)       // do NOT echo back to the sender's own socket
    .emit('data-change', payload);
}

// ─── Core initialisation – called once from server entry point ───────────────
function initSyncHandler(io) {

  // Subscribe to Redis channel for cross-instance fan-out
  subscribe('loveapp:data-changes', ({ coupleKey, senderId, payload }) => {
    io.to(coupleRoom(coupleKey))
      .except(senderId)
      .emit('data-change', payload);
  });

  io.on('connection', async (socket) => {
    activeSocketConnections.inc();
    const userId = socket.userId;

    // Ensure we have the couple room
    if (!socket.coupleKey) {
      try {
        socket.coupleKey = await getCoupleKey(userId);
      } catch {
        socket.coupleKey = `solo_${userId}`;
      }
    }
    socket.join(coupleRoom(socket.coupleKey));

    logger.info(`Socket connected: userId=${userId} coupleKey=${socket.coupleKey}`);

    // ── data-change ─────────────────────────────────────────────────────────
    socket.on('data-change', async (msg) => {
      try {
        const { entityType, action, data, clientTimestamp } = msg || {};

        if (!ENTITY_MAP[entityType]) {
          return socket.emit('error', { message: `Unknown entity type: ${entityType}` });
        }

        // Authorization check
        const entityId = data?.id || null;
        if (action !== 'create') {
          const ok = await canWrite(userId, entityType, entityId);
          if (!ok) return socket.emit('error', { message: 'Forbidden' });
        }

        // Persist change to PostgreSQL
        let savedData;
        try {
          savedData = await persistChange(userId, entityType, action, data);
        } catch (dbErr) {
          logger.error(`DB persist error: ${dbErr.message}`);
          return socket.emit('error', { message: 'Failed to save change' });
        }

        const payload = {
          entityType,
          action,
          data:            savedData,
          serverTimestamp: savedData?.server_updated_at?.toISOString?.() || new Date().toISOString(),
          senderId:        userId,
        };

        // Fan-out via Redis (for multi-instance deployments)
        await publish('loveapp:data-changes', {
          coupleKey: socket.coupleKey,
          senderId:  socket.id,
          payload,
        });

        // Also fan-out directly on this instance
        emitToCouple(io, socket.coupleKey, socket.id, payload);

        // Silent push fallback for offline partner
        sendPushToPartner(userId, {
          type:    'data_change',
          destination: entityType,
        }).catch(() => {});

      } catch (err) {
        logger.error(`data-change handler error: ${err.message}`);
        socket.emit('error', { message: 'Internal server error' });
      }
    });

    // ── sync-request ─────────────────────────────────────────────────────────
    socket.on('sync-request', async ({ sinceTimestamp } = {}) => {
      try {
        const events = await fetchChangesSince(userId, socket.coupleKey, sinceTimestamp);
        socket.emit('sync-response', { events });
      } catch (err) {
        logger.error(`sync-request error: ${err.message}`);
        socket.emit('error', { message: 'Sync request failed' });
      }
    });

    // ── Drawing (existing canvas events) ────────────────────────────────────
    socket.on('join-canvas', (canvasId) => {
      socket.currentCanvasId = canvasId;
      socket.join(`canvas:${canvasId}`);
    });

    socket.on('leave-canvas', (canvasId) => {
      socket.leave(`canvas:${canvasId}`);
    });

    socket.on('draw-action', (data) => {
      if (!data?.canvasId) return;
      socket.to(`canvas:${data.canvasId}`).emit('draw-action', {
        ...data,
        senderId: userId,
      });
    });

    socket.on('disconnect', () => {
      activeSocketConnections.dec();
      logger.info(`Socket disconnected: userId=${userId}`);
    });
  });
}

// ─── Persist a data-change to PostgreSQL ────────────────────────────────────
async function persistChange(userId, entityType, action, data) {
  const meta = ENTITY_MAP[entityType];

  if (action === 'delete') {
    // Soft delete — check ownership appropriately
    if (meta.calendarOwned) {
      const res = await pool.query(
        `UPDATE custom_calendar_events cce
         SET server_updated_at = NOW()
         FROM custom_calendars cc
         WHERE cce.calendar_id = cc.id
           AND cc.user_id = $2
           AND cce.id = $1
         RETURNING cce.*`,
        [data.id, userId]
      );
      return res.rows[0] || { id: data.id, server_updated_at: new Date() };
    }
    const res = await pool.query(
      `UPDATE ${meta.table}
       SET deleted_at = NOW(), server_updated_at = NOW()
       WHERE id = $1 AND ${meta.ownerCol} = $2
       RETURNING *`,
      [data.id, userId]
    );
    return res.rows[0] || { id: data.id, deleted_at: new Date(), server_updated_at: new Date() };
  }

  // For upserts we delegate to entity-specific handlers
  return await upsertEntity(userId, entityType, meta, data);
}

// ─── Per-entity upsert logic ─────────────────────────────────────────────────
async function upsertEntity(userId, entityType, meta, data) {
  // Only write columns that are explicitly allowed for this entity type.
  // This prevents SQL-injection via attacker-controlled column names.
  const EXCLUDED = new Set(['id', 'user_id', 'server_updated_at', 'created_at', 'deleted_at']);
  const allowed  = ENTITY_ALLOWED_COLUMNS[entityType]; // Set<string> | undefined

  const columns = Object.keys(data).filter(
    (k) => !EXCLUDED.has(k) && (allowed ? allowed.has(k) : false)
  );
  const values = columns.map((k) => data[k]);

  // Events are not owned by user_id directly — skip owner column in INSERT
  const ownerInsert = meta.calendarOwned ? [] : [meta.ownerCol];
  const ownerValues = meta.calendarOwned ? [] : [userId];

  if (data.id) {
    // UPDATE
    if (columns.length === 0) return data;
    const setClause = columns.map((c, i) => `${c} = $${i + 3}`).join(', ');
    const ownerCheck = meta.calendarOwned
      ? `AND calendar_id IN (SELECT id FROM custom_calendars WHERE user_id = $2)`
      : `AND ${meta.ownerCol} = $2`;
    const res = await pool.query(
      `UPDATE ${meta.table}
       SET ${setClause}, server_updated_at = NOW()
       WHERE id = $1 ${ownerCheck}
       RETURNING *`,
      [data.id, userId, ...values]
    );
    return res.rows[0];
  } else {
    // INSERT
    const allColumns = [...ownerInsert, ...columns];
    const allValues  = [...ownerValues, ...values];
    const colList         = allColumns.join(', ');
    const valPlaceholders = allValues.map((_, i) => `$${i + 1}`).join(', ');

    const res = await pool.query(
      `INSERT INTO ${meta.table} (${colList}, server_updated_at)
       VALUES (${valPlaceholders}, NOW())
       RETURNING *`,
      allValues
    );
    return res.rows[0];
  }
}

module.exports = { initSyncHandler, getCoupleKey };
