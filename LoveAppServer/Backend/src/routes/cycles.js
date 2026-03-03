// src/routes/cycles.js – Menstrual cycle CRUD + real-time fanout
const express = require('express');
const pool = require('../config/db');
const { authenticateToken } = require('../utils/auth');
const { sendResponse } = require('../utils/response');
const { sendPushToPartner } = require('../utils/fcm');
const { getPartnerId, buildCoupleKey, broadcastChange } = require('../utils/couple');

const router = express.Router();

function cycleJson(val) {
  if (val == null || val === '') return JSON.stringify({});
  if (typeof val === 'string') return val;
  return JSON.stringify(val);
}

// POST /api/cycles
router.post('/', authenticateToken, async (req, res) => {
  try {
    const { cycle_start_date, cycle_duration, period_duration, symptoms, mood, notes } = req.body;
    const result = await pool.query(
      `INSERT INTO menstrual_cycles (user_id, cycle_start_date, cycle_duration, period_duration, symptoms, mood, notes, server_updated_at)
       VALUES ($1,$2,$3,$4,$5::jsonb,$6::jsonb,$7,NOW()) RETURNING *`,
      [req.userId, cycle_start_date, cycle_duration || 28, period_duration || 5,
       cycleJson(symptoms), cycleJson(mood), notes || '']
    );
    const pid = await getPartnerId(req.userId);
    await broadcastChange(req.app.get('io'), buildCoupleKey(req.userId, pid), req.userId, 'cycle', 'create', result.rows[0]);
    sendPushToPartner(req.userId, { type: 'partner_cycle', isNewCycle: true, destination: 'menstrual_calendar' }).catch(() => {});
    sendResponse(res, true, 'Cycle created', result.rows[0], 201);
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// GET /api/cycles/partner (MUST be before /:id)
router.get('/partner', authenticateToken, async (req, res) => {
  try {
    const relResult = await pool.query(
      `SELECT COALESCE(partner_user_id, NULL) AS partner_id FROM relationship_info WHERE user_id=$1
       UNION SELECT user_id FROM relationship_info WHERE partner_user_id=$1 LIMIT 1`,
      [req.userId]
    );
    if (!relResult.rows.length) return sendResponse(res, true, 'No partner', { items: [], total: 0, page: 1, page_size: 100 });
    const pid = relResult.rows[0].partner_id;
    const limit = parseInt(req.query.limit) || 100;
    const [countRes, result] = await Promise.all([
      pool.query('SELECT COUNT(*) AS total FROM menstrual_cycles WHERE user_id=$1', [pid]),
      pool.query('SELECT * FROM menstrual_cycles WHERE user_id=$1 ORDER BY cycle_start_date DESC LIMIT $2', [pid, limit]),
    ]);
    sendResponse(res, true, 'Partner cycles retrieved', {
      items: result.rows, total: parseInt(countRes.rows[0].total), page: 1, page_size: limit,
    });
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// GET /api/cycles/latest (MUST be before /:id)
router.get('/latest', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT * FROM menstrual_cycles WHERE user_id=$1 ORDER BY cycle_start_date DESC LIMIT 1',
      [req.userId]
    );
    if (!result.rows.length) return sendResponse(res, false, 'No cycle data found', null, 404);
    sendResponse(res, true, 'Latest cycle retrieved', result.rows[0]);
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// GET /api/cycles
router.get('/', authenticateToken, async (req, res) => {
  try {
    const limit = parseInt(req.query.limit) || 100;
    const [countRes, result] = await Promise.all([
      pool.query('SELECT COUNT(*) AS total FROM menstrual_cycles WHERE user_id=$1', [req.userId]),
      pool.query('SELECT * FROM menstrual_cycles WHERE user_id=$1 ORDER BY cycle_start_date DESC LIMIT $2', [req.userId, limit]),
    ]);
    sendResponse(res, true, 'Cycles retrieved', {
      items: result.rows, total: parseInt(countRes.rows[0].total), page: 1, page_size: limit,
    });
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// PUT /api/cycles/:id
router.put('/:id', authenticateToken, async (req, res) => {
  try {
    const { cycle_start_date, cycle_duration, period_duration, symptoms, mood, notes } = req.body;
    const result = await pool.query(
      `UPDATE menstrual_cycles
       SET cycle_start_date=COALESCE($2,cycle_start_date), cycle_duration=COALESCE($3,cycle_duration),
           period_duration=COALESCE($4,period_duration), symptoms=COALESCE($5::jsonb,symptoms),
           mood=COALESCE($6::jsonb,mood), notes=COALESCE($7,notes), server_updated_at=NOW()
       WHERE id=$1 AND user_id=$8 RETURNING *`,
      [req.params.id, cycle_start_date, cycle_duration, period_duration,
       cycleJson(symptoms), cycleJson(mood), notes, req.userId]
    );
    if (!result.rows.length) return sendResponse(res, false, 'Cycle not found', null, 404);
    const pid = await getPartnerId(req.userId);
    await broadcastChange(req.app.get('io'), buildCoupleKey(req.userId, pid), req.userId, 'cycle', 'update', result.rows[0]);
    sendResponse(res, true, 'Cycle updated', result.rows[0]);
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// PATCH /api/cycles/:id – per-day symptoms/mood
router.patch('/:id', authenticateToken, async (req, res) => {
  try {
    const { date, symptoms_day, mood_day } = req.body;
    if (symptoms_day !== undefined) {
      await pool.query(
        `UPDATE menstrual_cycles SET symptoms=jsonb_set(COALESCE(symptoms,'{}'), $2::text[],$3::jsonb,true), server_updated_at=NOW()
         WHERE id=$1 AND user_id=$4`,
        [req.params.id, `{${date}}`, JSON.stringify(symptoms_day), req.userId]
      );
    }
    if (mood_day !== undefined) {
      await pool.query(
        `UPDATE menstrual_cycles SET mood=jsonb_set(COALESCE(mood,'{}'), $2::text[],$3::jsonb,true), server_updated_at=NOW()
         WHERE id=$1 AND user_id=$4`,
        [req.params.id, `{${date}}`, JSON.stringify(mood_day), req.userId]
      );
    }
    const result = await pool.query('SELECT * FROM menstrual_cycles WHERE id=$1 AND user_id=$2', [req.params.id, req.userId]);
    if (!result.rows.length) return sendResponse(res, false, 'Cycle not found', null, 404);
    const pid = await getPartnerId(req.userId);
    await broadcastChange(req.app.get('io'), buildCoupleKey(req.userId, pid), req.userId, 'cycle', 'update', result.rows[0]);
    if (symptoms_day !== undefined || mood_day !== undefined) {
      sendPushToPartner(req.userId, { type: 'partner_cycle', isNewCycle: false, destination: 'menstrual_calendar' }).catch(() => {});
    }
    sendResponse(res, true, 'Cycle day updated', result.rows[0]);
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// DELETE /api/cycles/:id – soft delete
router.delete('/:id', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      `UPDATE menstrual_cycles SET deleted_at=NOW(), server_updated_at=NOW()
       WHERE id=$1 AND user_id=$2 AND deleted_at IS NULL RETURNING id, server_updated_at`,
      [req.params.id, req.userId]
    );
    if (!result.rows.length) return sendResponse(res, false, 'Cycle not found', null, 404);
    const pid = await getPartnerId(req.userId);
    await broadcastChange(req.app.get('io'), buildCoupleKey(req.userId, pid), req.userId, 'cycle', 'delete', result.rows[0]);
    sendResponse(res, true, 'Cycle deleted');
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

module.exports = router;
