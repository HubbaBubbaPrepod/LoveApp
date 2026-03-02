// src/routes/relationship.js – Relationship info + partner pairing
const express = require('express');
const pool = require('../config/db');
const { authenticateToken } = require('../utils/auth');
const { sendResponse } = require('../utils/response');
const { broadcastChange } = require('./notes');
const crypto = require('crypto');

const router = express.Router();

function coupleKey(userId, pid) {
  return pid ? `${Math.min(userId, pid)}_${Math.max(userId, pid)}` : `solo_${userId}`;
}

// GET /api/relationship
router.get('/', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT ri.*, u.display_name AS partner_display_name, u.profile_image AS partner_avatar,
              me.profile_image AS my_avatar
       FROM relationship_info ri
       LEFT JOIN users u ON u.id=ri.partner_user_id
       JOIN users me ON me.id=ri.user_id
       WHERE ri.user_id=$1 LIMIT 1`,
      [req.userId]
    );
    if (!result.rows.length) return sendResponse(res, false, 'Relationship not found', null, 404);
    sendResponse(res, true, 'Relationship retrieved', result.rows[0]);
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// PUT /api/relationship
router.put('/', authenticateToken, async (req, res) => {
  try {
    const { relationship_start_date, first_kiss_date, anniversary_date, my_birthday, partner_birthday } = req.body;

    let result = await pool.query(
      `UPDATE relationship_info
       SET relationship_start_date=$1, first_kiss_date=$2, anniversary_date=$3,
           my_birthday=$4, partner_birthday=$5, updated_at=NOW(), server_updated_at=NOW()
       WHERE user_id=$6 RETURNING *`,
      [relationship_start_date, first_kiss_date, anniversary_date,
       my_birthday || null, partner_birthday || null, req.userId]
    );

    if (!result.rows.length) {
      result = await pool.query(
        `INSERT INTO relationship_info (user_id, relationship_start_date, first_kiss_date, anniversary_date, my_birthday, partner_birthday, server_updated_at)
         VALUES ($1,$2,$3,$4,$5,$6,NOW()) RETURNING *`,
        [req.userId, relationship_start_date, first_kiss_date, anniversary_date,
         my_birthday || null, partner_birthday || null]
      );
    }

    const full = await pool.query(
      `SELECT ri.*, u.display_name AS partner_display_name, u.profile_image AS partner_avatar, me.profile_image AS my_avatar
       FROM relationship_info ri
       LEFT JOIN users u ON u.id=ri.partner_user_id
       JOIN users me ON me.id=ri.user_id
       WHERE ri.user_id=$1 LIMIT 1`,
      [req.userId]
    );

    const row = full.rows[0] || result.rows[0];
    const pid = row.partner_user_id || null;
    await broadcastChange(req.app.get('io'), coupleKey(req.userId, pid), req.userId, 'relationship', 'update', row);
    sendResponse(res, true, 'Relationship updated', row);
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// POST /api/partner/generate-code
router.post('/generate-code', authenticateToken, async (req, res) => {
  try {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    let code = '';
    for (let i = 0; i < 6; i++) code += chars[Math.floor(Math.random() * chars.length)];

    await pool.query(
      `UPDATE users SET pairing_code=$1, pairing_code_expires_at=NOW() + INTERVAL '30 minutes' WHERE id=$2`,
      [code, req.userId]
    );
    sendResponse(res, true, 'Code generated', { code, expires_minutes: 30 });
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

// POST /api/partner/link
router.post('/link', authenticateToken, async (req, res) => {
  try {
    const { code } = req.body;
    if (!code) return sendResponse(res, false, 'Code is required', null, 400);

    const codeResult = await pool.query(
      `SELECT id, display_name, username FROM users
       WHERE pairing_code=$1 AND pairing_code_expires_at > NOW()`,
      [code.toUpperCase().trim()]
    );
    if (!codeResult.rows.length) return sendResponse(res, false, 'Code is invalid or expired', null, 404);

    const partner = codeResult.rows[0];
    if (partner.id === req.userId) return sendResponse(res, false, 'Cannot pair with yourself', null, 400);

    // Bidirectional relationship
    await pool.query(
      `INSERT INTO relationship_info (user_id, partner_user_id, server_updated_at)
       VALUES ($1,$2,NOW()) ON CONFLICT (user_id) DO UPDATE SET partner_user_id=$2, server_updated_at=NOW()`,
      [req.userId, partner.id]
    );
    await pool.query(
      `INSERT INTO relationship_info (user_id, partner_user_id, server_updated_at)
       VALUES ($1,$2,NOW()) ON CONFLICT (user_id) DO UPDATE SET partner_user_id=$2, server_updated_at=NOW()`,
      [partner.id, req.userId]
    );
    await pool.query('UPDATE users SET pairing_code=NULL, pairing_code_expires_at=NULL WHERE id=$1', [partner.id]);

    // Notify partner of the link
    const ck = coupleKey(req.userId, partner.id);
    await broadcastChange(req.app.get('io'), ck, req.userId, 'relationship', 'update', {
      user_id: req.userId, partner_user_id: partner.id,
    });

    sendResponse(res, true, 'Partner linked successfully', {
      partner_id: partner.id, partner_name: partner.display_name, partner_username: partner.username,
    });
  } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
});

module.exports = router;
