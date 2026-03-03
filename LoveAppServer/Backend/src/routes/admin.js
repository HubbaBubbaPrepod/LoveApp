// src/routes/admin.js – Admin panel API routes
const express = require('express');
const jwt = require('jsonwebtoken');
const bcryptjs = require('bcryptjs');
const pool = require('../config/db');
const { sendResponse } = require('../utils/response');

const router = express.Router();

const ADMIN_SECRET = process.env.ADMIN_JWT_SECRET || process.env.JWT_SECRET + '_admin_panel';

function generateAdminToken(userId, username) {
  return jwt.sign({ admin: true, userId, username }, ADMIN_SECRET, { expiresIn: '12h' });
}
function authenticateAdmin(req, res, next) {
  const token = (req.headers['authorization'] || '').replace('Bearer ', '');
  if (!token) return res.status(401).json({ success: false, message: 'Admin token required' });
  try {
    const dec = jwt.verify(token, ADMIN_SECRET);
    if (!dec.admin) throw new Error('not admin');
    req.adminUser = dec;
    next();
  } catch {
    res.status(403).json({ success: false, message: 'Invalid or expired admin token' });
  }
}

// POST /api/admin/login
router.post('/login', async (req, res) => {
  const { username, password } = req.body || {};
  if (!username || !password) return sendResponse(res, false, 'Username and password required', null, 400);
  try {
    const r = await pool.query(
      `SELECT id, username, display_name, password_hash, COALESCE(role,'user') AS role
       FROM users WHERE username=$1 OR email=$1 LIMIT 1`,
      [username]
    );
    const user = r.rows[0];
    if (!user || !['admin', 'superadmin'].includes(user.role)) return sendResponse(res, false, 'Access denied', null, 403);
    if (!await bcryptjs.compare(password, user.password_hash)) return sendResponse(res, false, 'Invalid credentials', null, 401);
    sendResponse(res, true, 'Logged in', { token: generateAdminToken(user.id, user.username), username: user.username });
  } catch (err) { sendResponse(res, false, 'Server error', null, 500); }
});

// GET /api/admin/stats
router.get('/stats', authenticateAdmin, async (req, res) => {
  try {
    const results = await Promise.all([
      pool.query('SELECT COUNT(*) FROM users'),
      pool.query('SELECT COUNT(*) FROM activity_logs'),
      pool.query('SELECT COUNT(*) FROM mood_entries'),
      pool.query('SELECT COUNT(*) FROM notes'),
      pool.query('SELECT COUNT(*) FROM wishes'),
      pool.query('SELECT COUNT(*) FROM relationship_info WHERE partner_user_id IS NOT NULL'),
      pool.query('SELECT COUNT(*) FROM custom_activity_types'),
    ]);
    sendResponse(res, true, 'Stats', {
      users:        parseInt(results[0].rows[0].count),
      activities:   parseInt(results[1].rows[0].count),
      moods:        parseInt(results[2].rows[0].count),
      notes:        parseInt(results[3].rows[0].count),
      wishes:       parseInt(results[4].rows[0].count),
      couples:      parseInt(results[5].rows[0].count),
      custom_types: parseInt(results[6].rows[0].count),
    });
  } catch (err) { sendResponse(res, false, 'Server error', null, 500); }
});

// GET /api/admin/stats/timeline
router.get('/stats/timeline', authenticateAdmin, async (req, res) => {
  try {
    const result = await pool.query(`
      SELECT date_trunc('day', created_at)::date AS day, COUNT(*)::int AS count
      FROM users WHERE created_at >= NOW() - INTERVAL '30 days'
      GROUP BY day ORDER BY day
    `);
    sendResponse(res, true, 'Timeline', result.rows);
  } catch (err) { sendResponse(res, false, 'Server error', null, 500); }
});

// GET /api/admin/stats/activities-by-type
router.get('/stats/activities-by-type', authenticateAdmin, async (req, res) => {
  try {
    const result = await pool.query(`
      SELECT activity_type, COUNT(*)::int AS count
      FROM activity_logs GROUP BY activity_type ORDER BY count DESC LIMIT 10
    `);
    sendResponse(res, true, 'By type', result.rows);
  } catch (err) { sendResponse(res, false, 'Server error', null, 500); }
});

// GET /api/admin/users
router.get('/users', authenticateAdmin, async (req, res) => {
  try {
    const { _start = 0, _end = 50, _sort = 'created_at', _order = 'DESC', q = '', id } = req.query;
    if (id !== undefined) {
      const r = await pool.query(
        `SELECT u.*, r.partner_user_id, p.username AS partner_username, p.display_name AS partner_display_name,
                (SELECT COUNT(*) FROM activity_logs WHERE user_id=u.id)::int AS activity_count,
                (SELECT COUNT(*) FROM mood_entries WHERE user_id=u.id)::int AS mood_count,
                (SELECT COUNT(*) FROM notes WHERE user_id=u.id)::int AS note_count,
                (SELECT COUNT(*) FROM wishes WHERE user_id=u.id)::int AS wish_count
         FROM users u LEFT JOIN relationship_info r ON r.user_id=u.id LEFT JOIN users p ON p.id=r.partner_user_id
         WHERE u.id=$1`, [id]
      );
      return sendResponse(res, true, 'User', r.rows[0] || null);
    }
    const limit = parseInt(_end) - parseInt(_start);
    const offset = parseInt(_start);
    const search = `%${q}%`;
    const count = await pool.query(
      `SELECT COUNT(*) FROM users WHERE display_name ILIKE $1 OR email ILIKE $1 OR username ILIKE $1`, [search]
    );
    const rows = await pool.query(
      `SELECT u.id, u.username, u.email, u.display_name, u.gender, u.created_at, u.role,
              r.partner_user_id, p.username AS partner_username,
              (SELECT COUNT(*) FROM activity_logs WHERE user_id=u.id)::int AS activity_count,
              (SELECT COUNT(*) FROM mood_entries WHERE user_id=u.id)::int AS mood_count
       FROM users u LEFT JOIN relationship_info r ON r.user_id=u.id LEFT JOIN users p ON p.id=r.partner_user_id
       WHERE u.display_name ILIKE $1 OR u.email ILIKE $1 OR u.username ILIKE $1
       ORDER BY u.${['id', 'created_at', 'display_name'].includes(_sort) ? _sort : 'created_at'} ${_order === 'ASC' ? 'ASC' : 'DESC'}
       LIMIT $2 OFFSET $3`, [search, limit, offset]
    );
    res.set('X-Total-Count', count.rows[0].count);
    res.set('Access-Control-Expose-Headers', 'X-Total-Count');
    res.json(rows.rows);
  } catch (err) { res.status(500).json({ error: 'Server error' }); }
});

// PUT /api/admin/users/:id
router.put('/users/:id', authenticateAdmin, async (req, res) => {
  try {
    const { role, display_name, email } = req.body || {};
    const fields = []; const params = [];
    if (role !== undefined) { if (!['user', 'admin'].includes(role)) return sendResponse(res, false, 'Invalid role', null, 400); params.push(role); fields.push(`role=$${params.length}`); }
    if (display_name !== undefined) { params.push(display_name); fields.push(`display_name=$${params.length}`); }
    if (email !== undefined)        { params.push(email);        fields.push(`email=$${params.length}`); }
    if (!fields.length) return sendResponse(res, false, 'Nothing to update', null, 400);
    params.push(req.params.id);
    const r = await pool.query(
      `UPDATE users SET ${fields.join(', ')}, updated_at=NOW() WHERE id=$${params.length} RETURNING id, username, display_name, email, role`, params
    );
    if (!r.rows.length) return sendResponse(res, false, 'User not found', null, 404);
    sendResponse(res, true, 'Updated', r.rows[0]);
  } catch (err) { sendResponse(res, false, 'Server error', null, 500); }
});

// DELETE /api/admin/users/:id
router.delete('/users/:id', authenticateAdmin, async (req, res) => {
  try {
    await pool.query('DELETE FROM users WHERE id=$1', [req.params.id]);
    sendResponse(res, true, 'User deleted', { id: parseInt(req.params.id) });
  } catch (err) { sendResponse(res, false, 'Server error', null, 500); }
});

// GET /api/admin/activities
router.get('/activities', authenticateAdmin, async (req, res) => {
  try {
    const { _start = 0, _end = 50, q = '', user_id } = req.query;
    const limit = parseInt(_end) - parseInt(_start); const offset = parseInt(_start);
    const cond = []; const params = [];
    if (q)       { params.push(`%${q}%`); cond.push(`(a.title ILIKE $${params.length} OR a.activity_type ILIKE $${params.length})`); }
    if (user_id) { params.push(user_id); cond.push(`a.user_id=$${params.length}`); }
    const where = cond.length ? 'WHERE ' + cond.join(' AND ') : '';
    const countRes = await pool.query(`SELECT COUNT(*) FROM activity_logs a ${where}`, params);
    params.push(limit, offset);
    const rows = await pool.query(
      `SELECT a.*, u.display_name, u.username FROM activity_logs a JOIN users u ON u.id=a.user_id ${where} ORDER BY a.created_at DESC LIMIT $${params.length-1} OFFSET $${params.length}`, params
    );
    res.set('X-Total-Count', countRes.rows[0].count); res.set('Access-Control-Expose-Headers', 'X-Total-Count');
    res.json(rows.rows);
  } catch (err) { res.status(500).json({ error: 'Server error' }); }
});

// DELETE /api/admin/activities/:id
router.delete('/activities/:id', authenticateAdmin, async (req, res) => {
  try {
    await pool.query('DELETE FROM activity_logs WHERE id=$1', [req.params.id]);
    sendResponse(res, true, 'Deleted', { id: parseInt(req.params.id) });
  } catch (err) { sendResponse(res, false, 'Server error', null, 500); }
});

['moods', 'notes', 'wishes'].forEach((entity) => {
  const table = entity === 'moods' ? 'mood_entries' : entity;
  router.get(`/${entity}`, authenticateAdmin, async (req, res) => {
    try {
      const { _start = 0, _end = 50, q = '' } = req.query;
      const limit = parseInt(_end) - parseInt(_start); const offset = parseInt(_start);
      const search = `%${q}%`;
      const col = entity === 'moods' ? 'mood_type' : (entity === 'notes' ? 'title' : 'title');
      const countRes = await pool.query(`SELECT COUNT(*) FROM ${table} WHERE ${col} ILIKE $1`, [search]);
      const rows = await pool.query(
        `SELECT m.*, u.display_name, u.username FROM ${table} m JOIN users u ON u.id=m.user_id
         WHERE ${col} ILIKE $1 ORDER BY m.created_at DESC LIMIT $2 OFFSET $3`, [search, limit, offset]
      );
      res.set('X-Total-Count', countRes.rows[0].count); res.set('Access-Control-Expose-Headers', 'X-Total-Count');
      res.json(rows.rows);
    } catch (err) { res.status(500).json({ error: 'Server error' }); }
  });
  router.delete(`/${entity}/:id`, authenticateAdmin, async (req, res) => {
    try {
      await pool.query(`DELETE FROM ${table} WHERE id=$1`, [req.params.id]);
      sendResponse(res, true, 'Deleted', { id: parseInt(req.params.id) });
    } catch (err) { sendResponse(res, false, 'Server error', null, 500); }
  });
});

module.exports = router;
