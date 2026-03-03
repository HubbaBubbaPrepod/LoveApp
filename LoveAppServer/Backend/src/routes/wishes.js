// src/routes/wishes.js – Wishes CRUD + real-time fanout
const express = require('express');
const pool = require('../config/db');
const { authenticateToken } = require('../utils/auth');
const { sendResponse } = require('../utils/response');
const { getPartnerId, buildCoupleKey, broadcastChange } = require('../utils/couple');

const router = express.Router();

// POST /api/wishes
router.post('/', authenticateToken, async (req, res) => {
  try {
    const { title, description, priority, category, is_completed, is_private, image_urls, emoji } = req.body;
    const validPriority = Math.min(5, Math.max(1, parseInt(priority, 10) || 1));
    const result = await pool.query(
      `INSERT INTO wishes (user_id, title, description, priority, category, is_completed, is_private, image_urls, emoji, server_updated_at)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,NOW()) RETURNING *`,
      [req.userId, title, description, validPriority, category || null, is_completed || false, is_private || false, image_urls || '', emoji || '']
    );
    const userRes = await pool.query('SELECT display_name FROM users WHERE id=$1', [req.userId]);
    const row = { ...result.rows[0], display_name: userRes.rows[0]?.display_name || '' };
    const pid = await getPartnerId(req.userId);
    await broadcastChange(req.app.get('io'), buildCoupleKey(req.userId, pid), req.userId, 'wish', 'create', row);
    sendResponse(res, true, 'Wish created', row, 201);
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// GET /api/wishes
router.get('/', authenticateToken, async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const pageSize = 20;
    const offset = (page - 1) * pageSize;
    const pid = await getPartnerId(req.userId);

    let countQ, listQ, params;
    if (pid) {
      countQ = 'SELECT COUNT(*) AS total FROM wishes WHERE (user_id=$1 OR (user_id=$2 AND is_private=false)) AND deleted_at IS NULL';
      listQ  = `SELECT w.*, u.display_name, u.profile_image FROM wishes w JOIN users u ON w.user_id=u.id
                WHERE (w.user_id=$1 OR (w.user_id=$2 AND w.is_private=false)) AND w.deleted_at IS NULL
                ORDER BY w.created_at DESC LIMIT $3 OFFSET $4`;
      params = [req.userId, pid, pageSize, offset];
    } else {
      countQ = 'SELECT COUNT(*) AS total FROM wishes WHERE user_id=$1 AND deleted_at IS NULL';
      listQ  = `SELECT w.*, u.display_name, u.profile_image FROM wishes w JOIN users u ON w.user_id=u.id
                WHERE w.user_id=$1 AND w.deleted_at IS NULL ORDER BY w.created_at DESC LIMIT $2 OFFSET $3`;
      params = [req.userId, pageSize, offset];
    }

    const [countResult, listResult] = await Promise.all([
      pool.query(countQ, pid ? [req.userId, pid] : [req.userId]),
      pool.query(listQ, params),
    ]);

    sendResponse(res, true, 'Wishes retrieved', {
      items: listResult.rows, total: parseInt(countResult.rows[0].total), page, page_size: pageSize,
    });
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// GET /api/wishes/:id
router.get('/:id', authenticateToken, async (req, res) => {
  try {
    const pid = await getPartnerId(req.userId);
    const result = pid
      ? await pool.query(
          `SELECT w.*, u.display_name FROM wishes w JOIN users u ON w.user_id=u.id
           WHERE w.id=$1 AND (w.user_id=$2 OR (w.user_id=$3 AND w.is_private=false)) AND w.deleted_at IS NULL`,
          [req.params.id, req.userId, pid]
        )
      : await pool.query(
          `SELECT w.*, u.display_name FROM wishes w JOIN users u ON w.user_id=u.id
           WHERE w.id=$1 AND w.user_id=$2 AND w.deleted_at IS NULL`,
          [req.params.id, req.userId]
        );
    if (!result.rows.length) return sendResponse(res, false, 'Wish not found', null, 404);
    sendResponse(res, true, 'Wish retrieved', result.rows[0]);
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// PUT /api/wishes/:id
router.put('/:id', authenticateToken, async (req, res) => {
  try {
    const { title, description, priority, category, is_private, image_urls, emoji } = req.body;
    const validPriority = Math.min(5, Math.max(1, parseInt(priority, 10) || 1));
    const result = await pool.query(
      `UPDATE wishes SET title=$1, description=$2, priority=$3, category=$4, is_private=$5, image_urls=$6, emoji=$7,
       updated_at=NOW(), server_updated_at=NOW()
       WHERE id=$8 AND user_id=$9 AND deleted_at IS NULL RETURNING *`,
      [title, description, validPriority, category || null, is_private || false, image_urls || '', emoji || '', req.params.id, req.userId]
    );
    if (!result.rows.length) return sendResponse(res, false, 'Wish not found', null, 404);
    const userRes = await pool.query('SELECT display_name FROM users WHERE id=$1', [req.userId]);
    const row = { ...result.rows[0], display_name: userRes.rows[0]?.display_name || '' };
    const pid = await getPartnerId(req.userId);
    await broadcastChange(req.app.get('io'), buildCoupleKey(req.userId, pid), req.userId, 'wish', 'update', row);
    sendResponse(res, true, 'Wish updated', row);
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// POST /api/wishes/:id/complete
router.post('/:id/complete', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      `UPDATE wishes SET is_completed=true, completed_at=NOW(), server_updated_at=NOW()
       WHERE id=$1 AND user_id=$2 AND deleted_at IS NULL RETURNING *`,
      [req.params.id, req.userId]
    );
    if (!result.rows.length) return sendResponse(res, false, 'Wish not found', null, 404);
    const userRes = await pool.query('SELECT display_name FROM users WHERE id=$1', [req.userId]);
    const row = { ...result.rows[0], display_name: userRes.rows[0]?.display_name || '' };
    const pid = await getPartnerId(req.userId);
    await broadcastChange(req.app.get('io'), buildCoupleKey(req.userId, pid), req.userId, 'wish', 'update', row);
    sendResponse(res, true, 'Wish completed', row);
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// DELETE /api/wishes/:id – soft delete
router.delete('/:id', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      `UPDATE wishes SET deleted_at=NOW(), server_updated_at=NOW()
       WHERE id=$1 AND user_id=$2 AND deleted_at IS NULL RETURNING id, server_updated_at`,
      [req.params.id, req.userId]
    );
    if (!result.rows.length) return sendResponse(res, false, 'Wish not found', null, 404);
    const pid = await getPartnerId(req.userId);
    await broadcastChange(req.app.get('io'), buildCoupleKey(req.userId, pid), req.userId, 'wish', 'delete', result.rows[0]);
    sendResponse(res, true, 'Wish deleted');
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

module.exports = router;
