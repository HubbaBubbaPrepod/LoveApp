// src/routes/calendars.js – Custom calendars & events + real-time fanout
const express = require('express');
const pool = require('../config/db');
const { authenticateToken } = require('../utils/auth');
const { sendResponse } = require('../utils/response');
const { getPartnerId, buildCoupleKey, broadcastChange } = require('../utils/couple');

const router = express.Router();

function normalizeColor(hex) {
  if (!hex || typeof hex !== 'string') return '#000000';
  const s = hex.trim();
  return (s.startsWith('#') ? s : '#' + s).substring(0, 7);
}
async function checkCalendarAccess(calendarId, userId) {
  const r = await pool.query(
    `SELECT cc.id FROM custom_calendars cc
     LEFT JOIN relationship_info ri
       ON (ri.user_id=$2 AND ri.partner_user_id=cc.user_id)
       OR (ri.partner_user_id=$2 AND ri.user_id=cc.user_id)
     WHERE cc.id=$1 AND (cc.user_id=$2 OR ri.id IS NOT NULL) LIMIT 1`,
    [calendarId, userId]
  );
  return r.rows.length > 0;
}

// POST /api/calendars
router.post('/', authenticateToken, async (req, res) => {
  try {
    const { name, description, type, color_hex } = req.body;
    const result = await pool.query(
      `INSERT INTO custom_calendars (user_id, name, description, type, color_hex, server_updated_at)
       VALUES ($1,$2,$3,$4,$5,NOW()) RETURNING *`,
      [req.userId, name || '', description || '', type || 'default', normalizeColor(color_hex)]
    );
    const pid = await getPartnerId(req.userId);
    await broadcastChange(req.app.get('io'), buildCoupleKey(req.userId, pid), req.userId, 'calendar', 'create', result.rows[0]);
    sendResponse(res, true, 'Calendar created', result.rows[0], 201);
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// GET /api/calendars/partner (MUST be before /:id)
router.get('/partner', authenticateToken, async (req, res) => {
  try {
    const relResult = await pool.query(
      `SELECT CASE WHEN user_id=$1 THEN partner_user_id ELSE user_id END AS partner_id
       FROM relationship_info WHERE user_id=$1 OR partner_user_id=$1 LIMIT 1`,
      [req.userId]
    );
    if (!relResult.rows.length) return sendResponse(res, true, 'No partner', { items: [], total: 0, page: 1, page_size: 100 });
    const pid = relResult.rows[0].partner_id;
    const result = await pool.query(
      'SELECT * FROM custom_calendars WHERE user_id=$1 AND deleted_at IS NULL ORDER BY created_at DESC', [pid]
    );
    sendResponse(res, true, 'Partner calendars retrieved', { items: result.rows, total: result.rows.length, page: 1, page_size: 100 });
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// GET /api/calendars
router.get('/', authenticateToken, async (req, res) => {
  try {
    const { type } = req.query;
    const page = parseInt(req.query.page) || 1;
    const pageSize = 20;
    const offset = (page - 1) * pageSize;
    const params = [req.userId]; let p = 2;
    let where = 'WHERE user_id=$1 AND deleted_at IS NULL';
    if (type) { where += ` AND type=$${p++}`; params.push(type); }

    const [countResult, result] = await Promise.all([
      pool.query(`SELECT COUNT(*) AS total FROM custom_calendars ${where}`, params),
      pool.query(`SELECT * FROM custom_calendars ${where} ORDER BY created_at DESC LIMIT $${p++} OFFSET $${p++}`, [...params, pageSize, offset]),
    ]);
    sendResponse(res, true, 'Calendars retrieved', {
      items: result.rows, total: parseInt(countResult.rows[0].total), page, page_size: pageSize,
    });
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// DELETE /api/calendars/events/:eventId  (MUST be before /:id)
router.delete('/events/:eventId', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      `DELETE FROM custom_calendar_events cce
       USING custom_calendars cc
       WHERE cce.id=$1 AND cce.calendar_id=cc.id
         AND (cc.user_id=$2 OR EXISTS (
           SELECT 1 FROM relationship_info ri
           WHERE (ri.user_id=$2 AND ri.partner_user_id=cc.user_id)
              OR (ri.partner_user_id=$2 AND ri.user_id=cc.user_id)
         ))
       RETURNING cce.id, cce.calendar_id`,
      [req.params.eventId, req.userId]
    );
    if (!result.rows.length) return sendResponse(res, false, 'Event not found or access denied', null, 404);
    const pid = await getPartnerId(req.userId);
    await broadcastChange(req.app.get('io'), buildCoupleKey(req.userId, pid), req.userId, 'event', 'delete', result.rows[0]);
    sendResponse(res, true, 'Event deleted', { id: result.rows[0].id });
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// GET /api/calendars/:id
router.get('/:id', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT * FROM custom_calendars WHERE id=$1 AND user_id=$2 AND deleted_at IS NULL', [req.params.id, req.userId]
    );
    if (!result.rows.length) return sendResponse(res, false, 'Calendar not found', null, 404);
    sendResponse(res, true, 'Calendar retrieved', result.rows[0]);
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// DELETE /api/calendars/:id – soft delete
router.delete('/:id', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      `UPDATE custom_calendars SET deleted_at=NOW(), server_updated_at=NOW()
       WHERE id=$1 AND user_id=$2 AND deleted_at IS NULL RETURNING id`, [req.params.id, req.userId]
    );
    if (!result.rows.length) return sendResponse(res, false, 'Calendar not found', null, 404);
    const pid = await getPartnerId(req.userId);
    await broadcastChange(req.app.get('io'), buildCoupleKey(req.userId, pid), req.userId, 'calendar', 'delete', result.rows[0]);
    sendResponse(res, true, 'Calendar deleted', { id: result.rows[0].id });
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// GET /api/calendars/:id/events
router.get('/:id/events', authenticateToken, async (req, res) => {
  try {
    if (!await checkCalendarAccess(req.params.id, req.userId))
      return sendResponse(res, false, 'Access denied', null, 403);
    const result = await pool.query(
      'SELECT * FROM custom_calendar_events WHERE calendar_id=$1 ORDER BY event_date ASC', [req.params.id]
    );
    sendResponse(res, true, 'Events retrieved', { items: result.rows, total: result.rows.length, page: 1, page_size: 1000 });
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// POST /api/calendars/:id/events
router.post('/:id/events', authenticateToken, async (req, res) => {
  try {
    if (!await checkCalendarAccess(req.params.id, req.userId))
      return sendResponse(res, false, 'Access denied', null, 403);
    const { event_date, title, description } = req.body;
    const result = await pool.query(
      `INSERT INTO custom_calendar_events (calendar_id, event_date, title, description, server_updated_at)
       VALUES ($1,$2,$3,$4,NOW()) RETURNING *`,
      [req.params.id, event_date, title || '', description || '']
    );
    const pid = await getPartnerId(req.userId);
    await broadcastChange(req.app.get('io'), buildCoupleKey(req.userId, pid), req.userId, 'event', 'create', result.rows[0]);
    sendResponse(res, true, 'Event created', result.rows[0], 201);
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

module.exports = router;
