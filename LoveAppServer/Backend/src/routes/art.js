// src/routes/art.js – Art canvases CRUD (drawing rooms handled by Socket.IO)
const express = require('express');
const pool = require('../config/db');
const { authenticateToken } = require('../utils/auth');
const { sendResponse } = require('../utils/response');

const router = express.Router();

async function getCoupleKey(userId) {
  const rel = await pool.query(
    'SELECT partner_user_id FROM relationship_info WHERE user_id=$1 LIMIT 1', [userId]
  );
  const pid = rel.rows[0]?.partner_user_id;
  const min = pid ? Math.min(userId, pid) : userId;
  const max = pid ? Math.max(userId, pid) : userId;
  return `${min}_${max}`;
}

// GET /api/art/canvases
router.get('/canvases', authenticateToken, async (req, res) => {
  try {
    const ck = await getCoupleKey(req.userId);
    const result = await pool.query(
      `SELECT id, couple_key, title, created_by, thumbnail_url, updated_at, created_at
       FROM art_canvases WHERE couple_key=$1 ORDER BY updated_at DESC`, [ck]
    );
    sendResponse(res, true, 'OK', result.rows);
  } catch (err) { sendResponse(res, false, 'Server error', null, 500); }
});

// POST /api/art/canvases
router.post('/canvases', authenticateToken, async (req, res) => {
  try {
    const { title = 'Без названия' } = req.body;
    const ck = await getCoupleKey(req.userId);
    const result = await pool.query(
      `INSERT INTO art_canvases (couple_key, title, created_by)
       VALUES ($1,$2,$3) RETURNING id, couple_key, title, created_by, thumbnail_url, updated_at, created_at`,
      [ck, title, req.userId]
    );
    sendResponse(res, true, 'Canvas created', result.rows[0], 201);
  } catch (err) { sendResponse(res, false, 'Server error', null, 500); }
});

// GET /api/art/canvases/:id
router.get('/canvases/:id', authenticateToken, async (req, res) => {
  try {
    const ck = await getCoupleKey(req.userId);
    const result = await pool.query(
      `SELECT id, couple_key, title, created_by, thumbnail_url, updated_at, created_at
       FROM art_canvases WHERE id=$1 AND couple_key=$2`, [req.params.id, ck]
    );
    if (!result.rows.length) return sendResponse(res, false, 'Not found', null, 404);
    sendResponse(res, true, 'OK', result.rows[0]);
  } catch (err) { sendResponse(res, false, 'Server error', null, 500); }
});

// PUT /api/art/canvases/:id
router.put('/canvases/:id', authenticateToken, async (req, res) => {
  try {
    const { title } = req.body;
    const ck = await getCoupleKey(req.userId);
    const result = await pool.query(
      `UPDATE art_canvases SET title=$1, updated_at=NOW()
       WHERE id=$2 AND couple_key=$3 RETURNING id, couple_key, title, created_by, thumbnail_url, updated_at, created_at`,
      [title, req.params.id, ck]
    );
    if (!result.rows.length) return sendResponse(res, false, 'Not found', null, 404);
    sendResponse(res, true, 'Updated', result.rows[0]);
  } catch (err) { sendResponse(res, false, 'Server error', null, 500); }
});

// DELETE /api/art/canvases/:id
router.delete('/canvases/:id', authenticateToken, async (req, res) => {
  try {
    const ck = await getCoupleKey(req.userId);
    await pool.query('DELETE FROM art_canvases WHERE id=$1 AND couple_key=$2', [req.params.id, ck]);
    sendResponse(res, true, 'Deleted');
  } catch (err) { sendResponse(res, false, 'Server error', null, 500); }
});

// POST /api/art/canvases/:id/thumbnail
router.post('/canvases/:id/thumbnail', authenticateToken, async (req, res) => {
  // upload middleware injected by main router
  const uploadSingle = req.app.get('uploadSingle');
  if (!uploadSingle) return sendResponse(res, false, 'Upload not configured', null, 500);

  uploadSingle(req, res, async (err) => {
    if (err) return sendResponse(res, false, err.message || 'Upload failed', null, 400);
    try {
      if (!req.file) return sendResponse(res, false, 'No file', null, 400);
      const ck = await getCoupleKey(req.userId);
      const serverUrl = process.env.SERVER_URL || 'http://195.2.71.218:3005';
      const url = `${serverUrl}/uploads/${req.file.filename}`;
      const result = await pool.query(
        `UPDATE art_canvases SET thumbnail_url=$1, updated_at=NOW()
         WHERE id=$2 AND couple_key=$3 RETURNING id, thumbnail_url, updated_at`,
        [url, req.params.id, ck]
      );
      if (!result.rows.length) return sendResponse(res, false, 'Not found', null, 404);
      sendResponse(res, true, 'Thumbnail updated', result.rows[0]);
    } catch (e) { sendResponse(res, false, 'Server error', null, 500); }
  });
});

module.exports = router;
