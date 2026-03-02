// src/routes/upload.js – File upload endpoints (profile image, generic image)
const express = require('express');
const path = require('path');
const pool = require('../config/db');
const { authenticateToken } = require('../utils/auth');
const { sendResponse } = require('../utils/response');
const { cacheDel } = require('../config/redis');

const router = express.Router();

// POST /api/upload/profile
router.post('/profile', authenticateToken, (req, res) => {
  const uploadSingle = req.app.get('uploadSingle');
  uploadSingle(req, res, async (err) => {
    if (err) return sendResponse(res, false, err.message || 'Upload failed', null, 400);
    try {
      if (!req.file) return sendResponse(res, false, 'File is required', null, 400);
      const serverUrl = process.env.SERVER_URL || `${req.protocol}://${req.get('host')}`;
      const fileUrl = `${serverUrl}/uploads/${req.file.filename}`;
      const result = await pool.query(
        'UPDATE users SET profile_image=$1, updated_at=NOW() WHERE id=$2 RETURNING id, profile_image',
        [fileUrl, req.userId]
      );
      if (!result.rows.length) return sendResponse(res, false, 'User not found', null, 404);
      await cacheDel(`profile:${req.userId}`);
      sendResponse(res, true, 'Profile image uploaded', { profile_image: fileUrl });
    } catch (err) { sendResponse(res, false, 'Internal server error', null, 500); }
  });
});

// POST /api/upload/image  – generic image (wishes, activities, etc.)
router.post('/image', authenticateToken, (req, res) => {
  const uploadSingle = req.app.get('uploadSingle');
  uploadSingle(req, res, (err) => {
    if (err) return sendResponse(res, false, err.message || 'Upload failed', null, 400);
    if (!req.file) return sendResponse(res, false, 'File is required', null, 400);
    sendResponse(res, true, 'Image uploaded', { url: `/uploads/${req.file.filename}` });
  });
});

module.exports = router;
