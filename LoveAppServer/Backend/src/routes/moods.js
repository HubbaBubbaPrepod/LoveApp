// src/routes/moods.js – Mood entries CRUD + real-time fanout
const express = require('express');
const pool = require('../config/db');
const { authenticateToken } = require('../utils/auth');
const { sendResponse } = require('../utils/response');
const { sendPushToPartner } = require('../utils/fcm');
const { broadcastChange } = require('./notes');

const router = express.Router();

async function getPartnerId(userId) {
  const r = await pool.query('SELECT partner_user_id FROM relationship_info WHERE user_id=$1 LIMIT 1', [userId]);
  return r.rows[0]?.partner_user_id || null;
}
function coupleKey(userId, pid) {
  return pid ? `${Math.min(userId, pid)}_${Math.max(userId, pid)}` : `solo_${userId}`;
}

// POST /api/moods
router.post('/', authenticateToken, async (req, res) => {
  try {
    const { mood_type, date, note } = req.body;
    if (!mood_type) return sendResponse(res, false, 'mood_type is required', null, 400);
    const today = new Date().toISOString().split('T')[0];
    const result = await pool.query(
      `INSERT INTO mood_entries (user_id, mood_type, date, note, server_updated_at)
       VALUES ($1,$2,$3,$4,NOW()) RETURNING *`,
      [req.userId, mood_type, date || today, note || '']
    );
    const row = result.rows[0];
    const pid = await getPartnerId(req.userId);
    await broadcastChange(req.app.get('io'), coupleKey(req.userId, pid), req.userId, 'mood', 'create', row);
    sendPushToPartner(req.userId, { type: 'partner_mood', moodType: mood_type, destination: 'mood_tracker' }).catch(() => {});
    sendResponse(res, true, 'Mood created', row, 201);
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// PUT /api/moods/:id
router.put('/:id', authenticateToken, async (req, res) => {
  try {
    const { mood_type, note } = req.body;
    const result = await pool.query(
      `UPDATE mood_entries SET mood_type=$1, note=$2, server_updated_at=NOW()
       WHERE id=$3 AND user_id=$4 AND deleted_at IS NULL RETURNING *`,
      [mood_type, note || '', req.params.id, req.userId]
    );
    if (!result.rows.length) return sendResponse(res, false, 'Mood not found', null, 404);
    const pid = await getPartnerId(req.userId);
    await broadcastChange(req.app.get('io'), coupleKey(req.userId, pid), req.userId, 'mood', 'update', result.rows[0]);
    sendResponse(res, true, 'Mood updated', result.rows[0]);
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// GET /api/moods/partner  (MUST come before /:id)
router.get('/partner', authenticateToken, async (req, res) => {
  try {
    const pid = await getPartnerId(req.userId);
    if (!pid) return sendResponse(res, true, 'No partner', { items: [], total: 0, page: 1, page_size: 500 });

    const { date, start_date, end_date } = req.query;
    let query = `SELECT me.*, u.display_name, u.profile_image FROM mood_entries me
                 JOIN users u ON me.user_id=u.id
                 WHERE me.user_id=$1 AND me.deleted_at IS NULL`;
    const params = [pid]; let p = 2;
    if (date) { query += ` AND DATE(me.date)=$${p++}`; params.push(date); }
    else if (start_date && end_date) { query += ` AND me.date>=$${p++} AND me.date<=$${p++}`; params.push(start_date, end_date); }
    query += ' ORDER BY me.date DESC, me.created_at DESC LIMIT 500';

    const result = await pool.query(query, params);
    sendResponse(res, true, 'Partner moods retrieved', { items: result.rows, total: result.rows.length, page: 1, page_size: 500 });
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// GET /api/moods
router.get('/', authenticateToken, async (req, res) => {
  try {
    const { date, start_date, end_date } = req.query;
    const page = parseInt(req.query.page) || 1;
    const pageSize = parseInt(req.query.limit) || 100;
    const offset = (page - 1) * pageSize;

    let baseWhere = 'WHERE user_id=$1 AND deleted_at IS NULL';
    const baseParams = [req.userId]; let p = 2;
    if (date) { baseWhere += ` AND DATE(date)=$${p++}`; baseParams.push(date); }
    else if (start_date && end_date) { baseWhere += ` AND date>=$${p++} AND date<=$${p++}`; baseParams.push(start_date, end_date); }

    const [countResult, result] = await Promise.all([
      pool.query(`SELECT COUNT(*) AS total FROM mood_entries ${baseWhere}`, baseParams),
      pool.query(
        `SELECT me.*, u.display_name, u.profile_image FROM mood_entries me JOIN users u ON me.user_id=u.id
         ${baseWhere} ORDER BY me.date DESC, me.created_at DESC LIMIT $${p++} OFFSET $${p++}`,
        [...baseParams, pageSize, offset]
      ),
    ]);

    sendResponse(res, true, 'Moods retrieved', {
      items: result.rows, total: parseInt(countResult.rows[0].total), page, page_size: pageSize,
    });
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// DELETE /api/moods/:id – soft delete
router.delete('/:id', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      `UPDATE mood_entries SET deleted_at=NOW(), server_updated_at=NOW()
       WHERE id=$1 AND user_id=$2 AND deleted_at IS NULL RETURNING id, server_updated_at`,
      [req.params.id, req.userId]
    );
    if (!result.rows.length) return sendResponse(res, false, 'Mood not found', null, 404);
    const pid = await getPartnerId(req.userId);
    await broadcastChange(req.app.get('io'), coupleKey(req.userId, pid), req.userId, 'mood', 'delete', result.rows[0]);
    sendResponse(res, true, 'Mood deleted');
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

module.exports = router;
