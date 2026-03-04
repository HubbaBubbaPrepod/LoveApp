// src/routes/activities.js – Activity logs CRUD + real-time fanout
const express = require('express');
const pool = require('../config/db');
const { authenticateToken } = require('../utils/auth');
const { sendResponse } = require('../utils/response');
const { sendPushToPartner } = require('../utils/fcm');
const { getPartnerId, buildCoupleKey, broadcastChange } = require('../utils/couple');

const router = express.Router();

const SELECT_ACTIVITY = `SELECT al.id, al.user_id, al.title, al.description,
  TO_CHAR(al.event_date, 'YYYY-MM-DD') AS date, al.category, al.activity_type, al.duration_minutes,
  al.start_time, al.note, al.server_updated_at, al.deleted_at, al.created_at,
  u.display_name, u.profile_image`;

// POST /api/activities
router.post('/', authenticateToken, async (req, res) => {
  try {
    const { title, description, date, category, activity_type, duration_minutes, start_time, note } = req.body;
    const actType = activity_type || category || title || 'other';
    const result = await pool.query(
      `INSERT INTO activity_logs (user_id, title, description, event_date, category, activity_type, duration_minutes, start_time, note, server_updated_at)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,NOW())
       RETURNING id, user_id, title, description, TO_CHAR(event_date, 'YYYY-MM-DD') AS date, category, activity_type, duration_minutes, start_time, note, server_updated_at, created_at`,
      [req.userId, title || actType, note || description || '', date, actType, actType,
       parseInt(duration_minutes, 10) || 0, start_time || '', note || description || '']
    );
    const userRes = await pool.query('SELECT display_name FROM users WHERE id=$1', [req.userId]);
    const row = { ...result.rows[0], display_name: userRes.rows[0]?.display_name || '' };
    const pid = await getPartnerId(req.userId);
    await broadcastChange(req.app.get('io'), buildCoupleKey(req.userId, pid), req.userId, 'activity', 'create', row);
    sendPushToPartner(req.userId, { type: 'partner_activity', count: 1, destination: 'activity_feed' }).catch(() => {});
    sendResponse(res, true, 'Activity created', row, 201);
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// GET /api/activities/partner (MUST be before /:id)
router.get('/partner', authenticateToken, async (req, res) => {
  try {
    const pid = await getPartnerId(req.userId);
    if (!pid) return sendResponse(res, true, 'No partner', { items: [], total: 0, page: 1, page_size: 500 });

    const { date, start_date, end_date } = req.query;
    let query = `${SELECT_ACTIVITY} FROM activity_logs al JOIN users u ON al.user_id=u.id
                 WHERE al.user_id=$1 AND al.deleted_at IS NULL`;
    const params = [pid]; let p = 2;
    if (date) { query += ` AND DATE(al.event_date)=$${p++}`; params.push(date); }
    else if (start_date && end_date) { query += ` AND al.event_date>=$${p++} AND al.event_date<=$${p++}`; params.push(start_date, end_date); }
    query += ' ORDER BY al.event_date DESC, al.created_at DESC LIMIT 500';
    const result = await pool.query(query, params);
    sendResponse(res, true, 'Partner activities retrieved', { items: result.rows, total: result.rows.length, page: 1, page_size: 500 });
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// GET /api/activities
router.get('/', authenticateToken, async (req, res) => {
  try {
    const { date, start_date, end_date } = req.query;
    const page = parseInt(req.query.page) || 1;
    const pageSize = parseInt(req.query.limit) || 100;
    const offset = (page - 1) * pageSize;

    let baseWhere = 'WHERE al.user_id=$1 AND al.deleted_at IS NULL';
    const base = [req.userId]; let p = 2;
    if (date) { baseWhere += ` AND DATE(al.event_date)=$${p++}`; base.push(date); }
    else if (start_date && end_date) { baseWhere += ` AND al.event_date>=$${p++} AND al.event_date<=$${p++}`; base.push(start_date, end_date); }

    const [countResult, result] = await Promise.all([
      pool.query(`SELECT COUNT(*) AS total FROM activity_logs al ${baseWhere}`, base),
      pool.query(
        `${SELECT_ACTIVITY} FROM activity_logs al JOIN users u ON al.user_id=u.id
         ${baseWhere} ORDER BY al.event_date DESC, al.created_at DESC LIMIT $${p++} OFFSET $${p++}`,
        [...base, pageSize, offset]
      ),
    ]);
    sendResponse(res, true, 'Activities retrieved', {
      items: result.rows, total: parseInt(countResult.rows[0].total), page, page_size: pageSize,
    });
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// PUT /api/activities/:id
router.put('/:id', authenticateToken, async (req, res) => {
  try {
    const { title, description, date, category, activity_type, duration_minutes, start_time, note } = req.body;
    const actType = activity_type || category || title || 'other';
    const result = await pool.query(
      `UPDATE activity_logs
       SET title=$1, description=$2, event_date=$3, category=$4, activity_type=$5,
           duration_minutes=$6, start_time=$7, note=$8, server_updated_at=NOW()
       WHERE id=$9 AND user_id=$10 AND deleted_at IS NULL
       RETURNING id, user_id, title, description, TO_CHAR(event_date, 'YYYY-MM-DD') AS date, category, activity_type,
                 duration_minutes, start_time, note, server_updated_at, created_at`,
      [title || actType, note || description || '', date, actType, actType,
       parseInt(duration_minutes, 10) || 0, start_time || '', note || description || '',
       req.params.id, req.userId]
    );
    if (!result.rows.length) return sendResponse(res, false, 'Activity not found', null, 404);
    const userRes = await pool.query('SELECT display_name FROM users WHERE id=$1', [req.userId]);
    const row = { ...result.rows[0], display_name: userRes.rows[0]?.display_name || '' };
    const pid = await getPartnerId(req.userId);
    await broadcastChange(req.app.get('io'), buildCoupleKey(req.userId, pid), req.userId, 'activity', 'update', row);
    sendResponse(res, true, 'Activity updated', row);
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// DELETE /api/activities/:id – soft delete
router.delete('/:id', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      `UPDATE activity_logs SET deleted_at=NOW(), server_updated_at=NOW()
       WHERE id=$1 AND user_id=$2 AND deleted_at IS NULL RETURNING id, server_updated_at`,
      [req.params.id, req.userId]
    );
    if (!result.rows.length) return sendResponse(res, false, 'Activity not found', null, 404);
    const pid = await getPartnerId(req.userId);
    await broadcastChange(req.app.get('io'), buildCoupleKey(req.userId, pid), req.userId, 'activity', 'delete', result.rows[0]);
    sendResponse(res, true, 'Activity deleted');
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// ── Custom activity types ────────────────────────────────────────────────────

// POST /api/activities/types
router.post('/types', authenticateToken, async (req, res) => {
  try {
    const { name, emoji, color_hex } = req.body;
    if (!name?.trim()) return sendResponse(res, false, 'Name is required', null, 400);
    const result = await pool.query(
      `INSERT INTO custom_activity_types (user_id, name, emoji, color_hex)
       VALUES ($1,$2,$3,$4) RETURNING id, user_id, name, emoji, color_hex, created_at`,
      [req.userId, name.trim(), (emoji || '✨').trim(), color_hex || '#FF6B9D']
    );
    sendResponse(res, true, 'Custom type created', { ...result.rows[0], is_mine: true }, 201);
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// GET /api/activities/types
router.get('/types', authenticateToken, async (req, res) => {
  try {
    const pid = await getPartnerId(req.userId);
    const result = pid
      ? await pool.query(
          `SELECT id, user_id, name, emoji, color_hex, created_at, (user_id=$1) AS is_mine
           FROM custom_activity_types WHERE user_id=$1 OR user_id=$2 ORDER BY created_at ASC`,
          [req.userId, pid]
        )
      : await pool.query(
          `SELECT id, user_id, name, emoji, color_hex, created_at, true AS is_mine
           FROM custom_activity_types WHERE user_id=$1 ORDER BY created_at ASC`,
          [req.userId]
        );
    sendResponse(res, true, 'Custom types retrieved', result.rows);
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// DELETE /api/activities/types/:id
router.delete('/types/:id', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      'DELETE FROM custom_activity_types WHERE id=$1 AND user_id=$2 RETURNING id',
      [req.params.id, req.userId]
    );
    if (!result.rows.length) return sendResponse(res, false, 'Not found', null, 404);
    sendResponse(res, true, 'Custom type deleted');
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

module.exports = router;
