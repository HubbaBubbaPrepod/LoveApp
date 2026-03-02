// src/utils/auth.js – JWT helpers
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const pool = require('../config/db');
const logger = require('../config/logger');

const JWT_SECRET         = process.env.JWT_SECRET         || 'your_super_secret_key_change_in_production';
const JWT_ACCESS_EXPIRES  = process.env.JWT_ACCESS_EXPIRES  || '15m';
const JWT_REFRESH_EXPIRES = process.env.JWT_REFRESH_EXPIRES || '30d';

/** Generate a short-lived access token */
function generateAccessToken(userId) {
  return jwt.sign({ userId }, JWT_SECRET, { expiresIn: JWT_ACCESS_EXPIRES });
}

/** Generate a long-lived refresh token + persist it in DB */
async function generateRefreshToken(userId) {
  const token = crypto.randomBytes(64).toString('hex');
  const expiresAt = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000); // 30 days
  await pool.query(
    `INSERT INTO refresh_tokens (user_id, token, expires_at)
     VALUES ($1, $2, $3)
     ON CONFLICT DO NOTHING`,
    [userId, token, expiresAt]
  );
  return token;
}

/** Verify and decode a JWT. Returns payload or null. */
function verifyToken(token) {
  try {
    return jwt.verify(token, JWT_SECRET);
  } catch {
    return null;
  }
}

/** Express middleware – validates Bearer token in Authorization header */
function authenticateToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({ success: false, message: 'Access token required' });
  }

  const decoded = verifyToken(token);
  if (!decoded) {
    return res.status(403).json({ success: false, message: 'Invalid or expired token' });
  }

  req.userId = decoded.userId;
  next();
}

/** Revoke all refresh tokens for a user (on logout/password change) */
async function revokeRefreshTokens(userId) {
  await pool.query('DELETE FROM refresh_tokens WHERE user_id = $1', [userId]);
}

module.exports = {
  generateAccessToken,
  generateRefreshToken,
  verifyToken,
  authenticateToken,
  revokeRefreshTokens,
};
