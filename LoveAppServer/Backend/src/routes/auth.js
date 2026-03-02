// src/routes/auth.js – Authentication routes with refresh token support
const express = require('express');
const bcryptjs = require('bcryptjs');
const pool = require('../config/db');
const { generateAccessToken, generateRefreshToken, authenticateToken, revokeRefreshTokens, verifyToken } = require('../utils/auth');
const { sendResponse } = require('../utils/response');
const { cacheGet, cacheSet, cacheDel } = require('../config/redis');

const router = express.Router();

// POST /api/auth/signup
router.post('/signup', async (req, res) => {
  try {
    const { username, email, password, display_name, gender } = req.body;
    if (!username || !email || !password || !display_name) {
      return sendResponse(res, false, 'Missing required fields', null, 400);
    }

    const userExists = await pool.query(
      'SELECT id FROM users WHERE email = $1 OR username = $2',
      [email, username]
    );
    if (userExists.rows.length > 0) {
      return sendResponse(res, false, 'User already exists', null, 400);
    }

    const hashedPassword = await bcryptjs.hash(password, 10);
    const result = await pool.query(
      'INSERT INTO users (username, email, password_hash, display_name, gender) VALUES ($1, $2, $3, $4, $5) RETURNING id, username, email, display_name, gender, created_at',
      [username, email, hashedPassword, display_name, gender]
    );

    const user = result.rows[0];
    const accessToken  = generateAccessToken(user.id);
    const refreshToken = await generateRefreshToken(user.id);

    sendResponse(res, true, 'Signup successful', {
      ...user, token: accessToken, refresh_token: refreshToken,
    });
  } catch (err) {
    req.log?.error?.(`Signup error: ${err.message}`);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// POST /api/auth/login
router.post('/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return sendResponse(res, false, 'Email and password required', null, 400);
    }

    const result = await pool.query(
      'SELECT id, username, email, display_name, gender, password_hash, created_at FROM users WHERE email = $1',
      [email]
    );
    if (result.rows.length === 0) {
      return sendResponse(res, false, 'User not found', null, 401);
    }

    const user = result.rows[0];
    const passwordValid = await bcryptjs.compare(password, user.password_hash);
    if (!passwordValid) {
      return sendResponse(res, false, 'Invalid password', null, 401);
    }

    const accessToken  = generateAccessToken(user.id);
    const refreshToken = await generateRefreshToken(user.id);

    sendResponse(res, true, 'Login successful', {
      id: user.id, username: user.username, email: user.email,
      display_name: user.display_name, gender: user.gender,
      token: accessToken, refresh_token: refreshToken,
      created_at: user.created_at,
    });
  } catch (err) {
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// POST /api/auth/refresh – Exchange refresh token for new access token
router.post('/refresh', async (req, res) => {
  try {
    const { refresh_token } = req.body;
    if (!refresh_token) {
      return sendResponse(res, false, 'refresh_token required', null, 400);
    }

    const result = await pool.query(
      `SELECT rt.user_id, u.username, u.email, u.display_name, u.gender
       FROM refresh_tokens rt
       JOIN users u ON u.id = rt.user_id
       WHERE rt.token = $1 AND rt.expires_at > NOW()`,
      [refresh_token]
    );

    if (result.rows.length === 0) {
      return sendResponse(res, false, 'Invalid or expired refresh token', null, 401);
    }

    const user = result.rows[0];
    const newAccessToken  = generateAccessToken(user.user_id);
    const newRefreshToken = await generateRefreshToken(user.user_id);

    // Rotate: delete used refresh token
    await pool.query('DELETE FROM refresh_tokens WHERE token = $1', [refresh_token]);

    sendResponse(res, true, 'Token refreshed', {
      token: newAccessToken, refresh_token: newRefreshToken,
    });
  } catch (err) {
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// POST /api/auth/logout – Revoke all refresh tokens
router.post('/logout', authenticateToken, async (req, res) => {
  try {
    await revokeRefreshTokens(req.userId);
    await cacheDel(`profile:${req.userId}`);
    sendResponse(res, true, 'Logged out successfully');
  } catch (err) {
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// GET /api/auth/profile
router.get('/profile', authenticateToken, async (req, res) => {
  try {
    const cacheKey = `profile:${req.userId}`;
    const cached = await cacheGet(cacheKey);
    if (cached) return sendResponse(res, true, 'Profile retrieved', cached);

    const result = await pool.query(
      'SELECT id, username, email, display_name, gender, profile_image, created_at FROM users WHERE id = $1',
      [req.userId]
    );
    if (result.rows.length === 0) return sendResponse(res, false, 'User not found', null, 404);

    await cacheSet(cacheKey, result.rows[0], 300);
    sendResponse(res, true, 'Profile retrieved', result.rows[0]);
  } catch (err) {
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// PUT /api/auth/profile – Update display_name / gender
router.put('/profile', authenticateToken, async (req, res) => {
  try {
    const { display_name, gender } = req.body;
    const result = await pool.query(
      'UPDATE users SET display_name = COALESCE($1, display_name), gender = COALESCE($2, gender), updated_at = NOW() WHERE id = $3 RETURNING id, username, email, display_name, gender, profile_image, created_at',
      [display_name?.trim() || null, gender || null, req.userId]
    );
    if (result.rows.length === 0) return sendResponse(res, false, 'User not found', null, 404);
    await cacheDel(`profile:${req.userId}`);
    sendResponse(res, true, 'Profile updated', result.rows[0]);
  } catch (err) {
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// PUT /api/auth/setup-profile – First-time profile setup after Google sign-in
router.put('/setup-profile', authenticateToken, async (req, res) => {
  try {
    const { display_name, gender } = req.body;
    if (!display_name?.trim()) return sendResponse(res, false, 'display_name is required', null, 400);
    if (!gender || !['male', 'female', 'other'].includes(gender)) {
      return sendResponse(res, false, 'Valid gender is required', null, 400);
    }
    const result = await pool.query(
      'UPDATE users SET display_name = $1, gender = $2, updated_at = NOW() WHERE id = $3 RETURNING id, username, email, display_name, gender, created_at',
      [display_name.trim(), gender, req.userId]
    );
    if (!result.rows.length) return sendResponse(res, false, 'User not found', null, 404);
    await cacheDel(`profile:${req.userId}`);
    sendResponse(res, true, 'Profile setup complete', { ...result.rows[0], needs_profile_setup: false });
  } catch (err) {
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// POST /api/auth/google
router.post('/google', async (req, res) => {
  try {
    const { id_token } = req.body;
    if (!id_token) return sendResponse(res, false, 'id_token is required', null, 400);

    const GOOGLE_WEB_CLIENT_ID     = '833288193423-pdau5bt5ffa4tjvioss2tut96s8frkd1.apps.googleusercontent.com';
    const GOOGLE_ANDROID_CLIENT_ID = '833288193423-obc5cifc5109pifdcou1cp4s6p01emab.apps.googleusercontent.com';

    const tokenInfoRes = await fetch(
      `https://oauth2.googleapis.com/tokeninfo?id_token=${encodeURIComponent(id_token)}`
    );
    const tokenInfo = await tokenInfoRes.json();
    if (!tokenInfoRes.ok || tokenInfo.error) {
      return sendResponse(res, false, 'Invalid Google ID token', null, 401);
    }
    if (![GOOGLE_WEB_CLIENT_ID, GOOGLE_ANDROID_CLIENT_ID].includes(tokenInfo.aud)) {
      return sendResponse(res, false, 'Google token audience mismatch', null, 401);
    }

    const email = tokenInfo.email;
    const name  = tokenInfo.name || tokenInfo.given_name || '';
    if (!email) return sendResponse(res, false, 'Email not available', null, 400);

    let userResult = await pool.query(
      'SELECT id, username, email, display_name, gender, created_at FROM users WHERE email = $1',
      [email]
    );
    let user;
    if (userResult.rows.length > 0) {
      user = userResult.rows[0];
    } else {
      const base = email.split('@')[0].replace(/[^a-zA-Z0-9_]/g, '_').substring(0, 20);
      let username = base; let suffix = 1;
      while (true) {
        const check = await pool.query('SELECT id FROM users WHERE username = $1', [username]);
        if (!check.rows.length) break;
        username = `${base}_${suffix++}`;
      }
      const r = await pool.query(
        'INSERT INTO users (username, email, password_hash, display_name, gender) VALUES ($1, $2, $3, $4, $5) RETURNING id, username, email, display_name, gender, created_at',
        [username, email, '', name || username, null]
      );
      user = r.rows[0];
    }

    const accessToken  = generateAccessToken(user.id);
    const refreshToken = await generateRefreshToken(user.id);

    sendResponse(res, true, 'Google auth successful', {
      ...user,
      needs_profile_setup: !user.gender,
      token: accessToken,
      refresh_token: refreshToken,
    });
  } catch (err) {
    sendResponse(res, false, 'Google authentication failed', null, 401);
  }
});

// POST /api/auth/fcm-token
router.post('/fcm-token', authenticateToken, async (req, res) => {
  try {
    const { fcm_token } = req.body;
    if (!fcm_token) return sendResponse(res, false, 'fcm_token required', null, 400);
    await pool.query(
      `INSERT INTO fcm_tokens (user_id, fcm_token, updated_at) VALUES ($1, $2, NOW())
       ON CONFLICT (user_id) DO UPDATE SET fcm_token = $2, updated_at = NOW()`,
      [req.userId, fcm_token]
    );
    sendResponse(res, true, 'FCM token registered');
  } catch (err) {
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

module.exports = router;
