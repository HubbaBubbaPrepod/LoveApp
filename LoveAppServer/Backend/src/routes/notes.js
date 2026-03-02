// src/routes/notes.js – Notes CRUD + real-time fanout
const express = require('express');
const pool = require('../config/db');
const { authenticateToken } = require('../utils/auth');
const { sendResponse } = require('../utils/response');
const { publish } = require('../config/redis');

const router = express.Router();

/** Broadcast a data-change event for the couple (used by REST endpoints too) */
async function broadcastChange(io, coupleKey, senderId, entityType, action, data) {
  if (!io) return;
  const payload = {
    entityType, action, data,
    serverTimestamp: data?.server_updated_at?.toISOString?.() || new Date().toISOString(),
    senderId,
  };
  io.to(`couple:${coupleKey}`).except(String(senderId)).emit('data-change', payload);
  await publish('loveapp:data-changes', { coupleKey, senderId: String(senderId), payload });
}

function parseTags(tags) {
  if (!tags || tags === '') return [];
  if (Array.isArray(tags)) return tags;
  return String(tags).split(',').map((s) => s.trim()).filter(Boolean);
}

function formatNote(row) {
  if (!row) return row;
  return { ...row, tags: Array.isArray(row.tags) ? row.tags.join(',') : (row.tags || '') };
}

async function getPartnerId(userId) {
  const r = await pool.query(
    'SELECT partner_user_id FROM relationship_info WHERE user_id = $1 LIMIT 1', [userId]
  );
  return r.rows[0]?.partner_user_id || null;
}

// POST /api/notes
router.post('/', authenticateToken, async (req, res) => {
  const { title, content, is_private, tags } = req.body;
  try {
    const tagsArr = parseTags(tags);
    const result = await pool.query(
      `INSERT INTO notes (user_id, title, content, is_private, tags, server_updated_at)
       VALUES ($1, $2, $3, $4, $5, NOW()) RETURNING *`,
      [req.userId, title, content, is_private || false, tagsArr]
    );
    const row = formatNote(result.rows[0]);
    const partnerId = await getPartnerId(req.userId);
    const coupleKey = partnerId
      ? `${Math.min(req.userId, partnerId)}_${Math.max(req.userId, partnerId)}`
      : `solo_${req.userId}`;
    await broadcastChange(req.app.get('io'), coupleKey, req.userId, 'note', 'create', row);
    sendResponse(res, true, 'Note created', row, 201);
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// GET /api/notes
router.get('/', authenticateToken, async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const pageSize = 20;
    const offset = (page - 1) * pageSize;
    const partnerId = await getPartnerId(req.userId);

    let countQ, listQ, params;
    if (partnerId) {
      params = [req.userId, partnerId, pageSize, offset];
      countQ = 'SELECT COUNT(*) AS total FROM notes WHERE (user_id=$1 OR (user_id=$2 AND is_private=false)) AND deleted_at IS NULL';
      listQ  = `SELECT n.*, u.display_name, u.profile_image FROM notes n
                JOIN users u ON n.user_id=u.id
                WHERE (n.user_id=$1 OR (n.user_id=$2 AND n.is_private=false)) AND n.deleted_at IS NULL
                ORDER BY n.created_at DESC LIMIT $3 OFFSET $4`;
    } else {
      params = [req.userId, pageSize, offset];
      countQ = 'SELECT COUNT(*) AS total FROM notes WHERE user_id=$1 AND deleted_at IS NULL';
      listQ  = `SELECT n.*, u.display_name, u.profile_image FROM notes n JOIN users u ON n.user_id=u.id
                WHERE n.user_id=$1 AND n.deleted_at IS NULL ORDER BY n.created_at DESC LIMIT $2 OFFSET $3`;
    }

    const [countResult, listResult] = await Promise.all([
      pool.query(countQ, partnerId ? [req.userId, partnerId] : [req.userId]),
      pool.query(listQ, params),
    ]);

    sendResponse(res, true, 'Notes retrieved', {
      items: listResult.rows.map(formatNote),
      total: parseInt(countResult.rows[0].total),
      page, page_size: pageSize,
    });
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// GET /api/notes/:id
router.get('/:id', authenticateToken, async (req, res) => {
  try {
    const partnerId = await getPartnerId(req.userId);
    let result;
    if (partnerId) {
      result = await pool.query(
        `SELECT n.*, u.display_name FROM notes n JOIN users u ON n.user_id=u.id
         WHERE n.id=$1 AND (n.user_id=$2 OR (n.user_id=$3 AND n.is_private=false)) AND n.deleted_at IS NULL`,
        [req.params.id, req.userId, partnerId]
      );
    } else {
      result = await pool.query(
        `SELECT n.*, u.display_name FROM notes n JOIN users u ON n.user_id=u.id
         WHERE n.id=$1 AND n.user_id=$2 AND n.deleted_at IS NULL`,
        [req.params.id, req.userId]
      );
    }
    if (!result.rows.length) return sendResponse(res, false, 'Note not found', null, 404);
    sendResponse(res, true, 'Note retrieved', formatNote(result.rows[0]));
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// PUT /api/notes/:id
router.put('/:id', authenticateToken, async (req, res) => {
  try {
    const { title, content, is_private, tags } = req.body;
    const tagsArr = parseTags(tags);
    const result = await pool.query(
      `UPDATE notes SET title=$1, content=$2, is_private=$3, tags=$4, updated_at=NOW(), server_updated_at=NOW()
       WHERE id=$5 AND user_id=$6 AND deleted_at IS NULL RETURNING *`,
      [title, content, is_private, tagsArr, req.params.id, req.userId]
    );
    if (!result.rows.length) return sendResponse(res, false, 'Note not found', null, 404);
    const row = formatNote(result.rows[0]);
    const partnerId = await getPartnerId(req.userId);
    const coupleKey = partnerId
      ? `${Math.min(req.userId, partnerId)}_${Math.max(req.userId, partnerId)}`
      : `solo_${req.userId}`;
    await broadcastChange(req.app.get('io'), coupleKey, req.userId, 'note', 'update', row);
    sendResponse(res, true, 'Note updated', row);
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// DELETE /api/notes/:id  – soft delete
router.delete('/:id', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      `UPDATE notes SET deleted_at=NOW(), server_updated_at=NOW()
       WHERE id=$1 AND user_id=$2 AND deleted_at IS NULL RETURNING id, server_updated_at`,
      [req.params.id, req.userId]
    );
    if (!result.rows.length) return sendResponse(res, false, 'Note not found', null, 404);
    const partnerId = await getPartnerId(req.userId);
    const coupleKey = partnerId
      ? `${Math.min(req.userId, partnerId)}_${Math.max(req.userId, partnerId)}`
      : `solo_${req.userId}`;
    await broadcastChange(req.app.get('io'), coupleKey, req.userId, 'note', 'delete', result.rows[0]);
    sendResponse(res, true, 'Note deleted');
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

module.exports = router;
module.exports.broadcastChange = broadcastChange;
