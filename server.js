// server.js - Express Backend for LoveApp

const express = require('express');
const pg = require('pg');
const cors = require('cors');
const dotenv = require('dotenv');
const bcryptjs = require('bcryptjs');
const jwt = require('jsonwebtoken');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const admin = require('firebase-admin');

dotenv.config();

// Force pg to return PostgreSQL `date` columns as plain 'YYYY-MM-DD' strings
// instead of JavaScript Date objects (which cause timezone offset shifts)
pg.types.setTypeParser(1082, val => val);

const app = express();

// Middleware
app.use(cors());
app.use(express.json());

// Static file serving for uploaded files
const storagePath = process.env.STORAGE_PATH || path.join(__dirname, 'uploads');
fs.mkdirSync(storagePath, { recursive: true }); // ensure uploads dir exists
app.use('/uploads', express.static(storagePath));

// Multer setup for file uploads
const upload = multer({
  storage: multer.diskStorage({
    destination: (req, file, cb) => cb(null, storagePath),
    filename: (req, file, cb) => {
      const ext = path.extname(file.originalname);
      const name = path.basename(file.originalname, ext).replace(/[^a-zA-Z0-9-_]/g, '_');
      cb(null, `${Date.now()}_${name}${ext}`);
    }
  }),
  limits: { fileSize: parseInt(process.env.MAX_FILE_SIZE || String(20 * 1024 * 1024)) }
});

// PostgreSQL Connection Pool
const pool = new pg.Pool({
  host: process.env.PGHOST || '195.2.71.218',
  user: process.env.PGUSER || 'spyuser',
  password: process.env.PGPASSWORD || '0451',
  database: process.env.PGDATABASE || 'loveapp_db',
  port: process.env.PGPORT || 5432,
});

pool.on('error', (err) => {
  console.error('Unexpected error on idle client', err);
});

// JWT Secret
const JWT_SECRET = process.env.JWT_SECRET || 'your_super_secret_key_change_in_production';

// Helper Functions
const generateToken = (userId) => {
  return jwt.sign({ userId }, JWT_SECRET, { expiresIn: '7d' });
};

const verifyToken = (token) => {
  try {
    return jwt.verify(token, JWT_SECRET);
  } catch (err) {
    return null;
  }
};

const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({
      success: false,
      message: 'Access token required',
    });
  }

  const decoded = verifyToken(token);
  if (!decoded) {
    return res.status(403).json({
      success: false,
      message: 'Invalid or expired token',
    });
  }

  req.userId = decoded.userId;
  next();
};

// API Response Helper
const sendResponse = (res, success, message, data = null, statusCode = 200) => {
  const response = {
    success,
    message,
  };
  if (data) response.data = data;
  res.status(statusCode).json(response);
};

// ── Firebase Admin SDK init ─────────────────────────────────────────────────
// Set the FIREBASE_SERVICE_ACCOUNT env var to the full JSON of your service-account key,
// OR place service-account.json next to server.js.
try {
  let serviceAccount;
  if (process.env.FIREBASE_SERVICE_ACCOUNT) {
    serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
  } else {
    const saPath = path.join(__dirname, 'service-account.json');
    serviceAccount = JSON.parse(fs.readFileSync(saPath, 'utf8'));
  }
  admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
  console.log('Firebase Admin SDK initialised — push notifications enabled');
} catch (e) {
  console.warn('Firebase Admin SDK NOT initialised:', e.message);
  console.warn('Push notifications disabled. Add service-account.json to enable them.');
}

// ── FCM helpers ──────────────────────────────────────────────────────────────
function moodToEmoji(type) {
  const map = { great:'😄', отлично:'😄', good:'🙂', хорошо:'🙂',
                okay:'😐', нормально:'😐', bad:'😔', плохо:'😔',
                terrible:'😢', ужасно:'😢' };
  return map[(type||'').toLowerCase()] || '💬';
}

async function sendPushToPartner(userId, data) {
  if (!admin.apps.length) return;
  try {
    // 1. Find partner id
    const relRes = await pool.query(
      'SELECT partner_user_id FROM relationship_info WHERE user_id = $1 LIMIT 1',
      [userId]
    );
    const partnerId = relRes.rows[0]?.partner_user_id;
    if (!partnerId) return;

    // 2. Get sender display name
    const userRes = await pool.query('SELECT display_name FROM users WHERE id = $1', [userId]);
    const senderName = userRes.rows[0]?.display_name || 'Партнёр';

    // 3. Get partner FCM token
    const tokenRes = await pool.query('SELECT fcm_token FROM fcm_tokens WHERE user_id = $1', [partnerId]);
    const fcmToken = tokenRes.rows[0]?.fcm_token;
    if (!fcmToken) return;

    // 4. Build message
    let title, body;
    if (data.type === 'partner_mood') {
      const emoji = moodToEmoji(data.moodType);
      title = `${senderName} поделился(ась) настроением ${emoji}`;
      body  = 'Открой приложение, чтобы увидеть как дела';
    } else if (data.type === 'partner_cycle') {
      title = data.isNewCycle === true || data.isNewCycle === 'true'
        ? `${senderName} начал(а) новый цикл 🌸`
        : `${senderName} обновил(а) данные цикла 🌸`;
      body  = 'Открой приложение, чтобы посмотреть';
    } else {
      const c = data.count || 1;
      title = `${senderName} добавил(а) ${c === 1 ? '1 активность' : c + ' активностей'} 🏃`;
      body  = 'Посмотри, чем занимался(ась) партнёр';
    }

    // 5. Send via FCM (data-only — no `notification` block so onMessageReceived fires
    //    even in background, letting the device respect the user's toggle)
    await admin.messaging().send({
      token: fcmToken,
      data: {
        type:        data.type,
        partnerName: senderName,
        title,
        body,
        destination: data.destination || '',
        ...(data.moodType ? { moodType: data.moodType, moodEmoji: moodToEmoji(data.moodType) } : {}),
        ...(data.count != null ? { count: String(data.count) } : {}),
        ...(data.isNewCycle != null ? { isNewCycle: String(data.isNewCycle) } : {}),
      },
      android: { priority: 'high' },
    });
  } catch (e) {
    console.error('FCM send error:', e.message);
  }
}

// ==================== HEALTH CHECK ====================
app.get('/api/health', (req, res) => {
  res.json({ status: 'Backend is running' });
});

// ==================== AUTHENTICATION ====================

// Signup
app.post('/api/auth/signup', async (req, res) => {
  try {
    const { username, email, password, display_name, gender } = req.body;

    // Validate input
    if (!username || !email || !password || !display_name) {
      return sendResponse(res, false, 'Missing required fields', null, 400);
    }

    // Check if user exists
    const userExists = await pool.query(
      'SELECT id FROM users WHERE email = $1 OR username = $2',
      [email, username]
    );

    if (userExists.rows.length > 0) {
      return sendResponse(res, false, 'User already exists', null, 400);
    }

    // Hash password
    const hashedPassword = await bcryptjs.hash(password, 10);

    // Create user
    const result = await pool.query(
      'INSERT INTO users (username, email, password_hash, display_name, gender) VALUES ($1, $2, $3, $4, $5) RETURNING id, username, email, display_name, gender, created_at',
      [username, email, hashedPassword, display_name, gender]
    );

    const user = result.rows[0];
    const token = generateToken(user.id);

    sendResponse(res, true, 'Signup successful', {
      id: user.id,
      username: user.username,
      email: user.email,
      display_name: user.display_name,
      gender: user.gender,
      token,
      created_at: user.created_at,
    });
  } catch (err) {
    console.error('Signup error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Login
app.post('/api/auth/login', async (req, res) => {
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

    const token = generateToken(user.id);

    sendResponse(res, true, 'Login successful', {
      id: user.id,
      username: user.username,
      email: user.email,
      display_name: user.display_name,
      gender: user.gender,
      token,
      created_at: user.created_at,
    });
  } catch (err) {
    console.error('Login error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Google Sign-In
app.post('/api/auth/google', async (req, res) => {
  try {
    const { id_token } = req.body;
    if (!id_token) return sendResponse(res, false, 'id_token is required', null, 400);

    // Verify Google ID token via Google's tokeninfo endpoint (no Firebase needed)
    const GOOGLE_WEB_CLIENT_ID = '833288193423-pdau5bt5ffa4tjvioss2tut96s8frkd1.apps.googleusercontent.com';
    const tokenInfoRes = await fetch(
      `https://oauth2.googleapis.com/tokeninfo?id_token=${encodeURIComponent(id_token)}`
    );
    const tokenInfo = await tokenInfoRes.json();

    if (!tokenInfoRes.ok || tokenInfo.error) {
      console.error('Google tokeninfo error:', tokenInfo.error);
      return sendResponse(res, false, 'Invalid Google ID token', null, 401);
    }

    // Verify audience matches our Web Client ID (or Android Client ID)
    const validAudiences = [
      GOOGLE_WEB_CLIENT_ID,
      '833288193423-obc5cifc5109pifdcou1cp4s6p01emab.apps.googleusercontent.com', // Android client
    ];
    if (!validAudiences.includes(tokenInfo.aud)) {
      console.error('Google token audience mismatch:', tokenInfo.aud);
      return sendResponse(res, false, 'Google token audience mismatch', null, 401);
    }

    const email = tokenInfo.email;
    const name  = tokenInfo.name || tokenInfo.given_name || '';
    if (!email) return sendResponse(res, false, 'Email not available from Google account', null, 400);

    // Find or create user
    let userResult = await pool.query(
      'SELECT id, username, email, display_name, gender, created_at FROM users WHERE email = $1',
      [email]
    );

    let user;
    if (userResult.rows.length > 0) {
      // Existing user — return as-is
      user = userResult.rows[0];
    } else {
      // New user — generate unique username from email prefix
      const base = email.split('@')[0].replace(/[^a-zA-Z0-9_]/g, '_').substring(0, 20);
      let username = base;
      let suffix = 1;
      while (true) {
        const check = await pool.query('SELECT id FROM users WHERE username = $1', [username]);
        if (check.rows.length === 0) break;
        username = `${base}_${suffix++}`;
      }
      const displayName = name || username;
      const result = await pool.query(
        'INSERT INTO users (username, email, password_hash, display_name, gender) VALUES ($1, $2, $3, $4, $5) RETURNING id, username, email, display_name, gender, created_at',
        [username, email, '', displayName, 'other']
      );
      user = result.rows[0];
    }

    const token = generateToken(user.id);
    sendResponse(res, true, 'Google auth successful', {
      id: user.id,
      username: user.username,
      email: user.email,
      display_name: user.display_name,
      gender: user.gender,
      token,
      created_at: user.created_at,
    });
  } catch (err) {
    console.error('Google auth error:', err.message);
    sendResponse(res, false, 'Google authentication failed', null, 401);
  }
});

// Get Profile
app.get('/api/auth/profile', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT id, username, email, display_name, gender, profile_image, created_at FROM users WHERE id = $1',
      [req.userId]
    );

    if (result.rows.length === 0) {
      return sendResponse(res, false, 'User not found', null, 404);
    }

    sendResponse(res, true, 'Profile retrieved', result.rows[0]);
  } catch (err) {
    console.error('Get profile error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Upload profile image
app.post('/api/upload/profile', authenticateToken, upload.single('file'), async (req, res) => {
  try {
    if (!req.file) return sendResponse(res, false, 'File is required', null, 400);

    const fileUrl = `${req.protocol}://${req.get('host')}/uploads/${req.file.filename}`;

    const result = await pool.query(
      'UPDATE users SET profile_image = $1, updated_at = NOW() WHERE id = $2 RETURNING id, profile_image',
      [fileUrl, req.userId]
    );

    if (result.rows.length === 0) return sendResponse(res, false, 'User not found', null, 404);

    sendResponse(res, true, 'Profile image uploaded', { profile_image: fileUrl });
  } catch (err) {
    console.error('Upload profile error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Generic image upload (for wishes, activities, etc.)
app.post('/api/upload/image', authenticateToken, (req, res, next) => {
  upload.single('file')(req, res, (err) => {
    if (err) {
      console.error('Upload multer error:', err);
      return sendResponse(res, false, err.message || 'File upload failed', null, 400);
    }
    next();
  });
}, async (req, res) => {
  try {
    if (!req.file) return sendResponse(res, false, 'File is required', null, 400);
    const fileUrl = `${req.protocol}://${req.get('host')}/uploads/${req.file.filename}`;
    sendResponse(res, true, 'Image uploaded', { url: fileUrl });
  } catch (err) {
    console.error('Upload image error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// ==================== NOTES ====================

// Приложение ждёт tags как строку, в БД — массив text[]
function formatNoteForApi(row) {
  if (!row) return row;
  const r = { ...row };
  r.tags = Array.isArray(r.tags) ? r.tags.join(',') : (r.tags || '');
  return r;
}

// Create Note (tags в БД — массив text[], пустая строка недопустима)
function parseTags(tags) {
  if (tags == null || tags === '') return [];
  if (Array.isArray(tags)) return tags;
  if (typeof tags === 'string') return tags.split(',').map(s => s.trim()).filter(Boolean);
  return [String(tags)];
}

// Create Note
app.post('/api/notes', authenticateToken, async (req, res) => {
  try {
    const { title, content, is_private, tags } = req.body;
    const tagsArr = parseTags(tags);

    const result = await pool.query(
      'INSERT INTO notes (user_id, title, content, is_private, tags) VALUES ($1, $2, $3, $4, $5) RETURNING *',
      [req.userId, title, content, is_private || false, tagsArr]
    );

    sendResponse(res, true, 'Note created', formatNoteForApi(result.rows[0]), 201);
  } catch (err) {
    console.error('Create note error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Get Notes (own notes + partner's public notes)
app.get('/api/notes', authenticateToken, async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const pageSize = 20;
    const offset = (page - 1) * pageSize;

    // Look up partner
    const relResult = await pool.query(
      'SELECT partner_user_id FROM relationship_info WHERE user_id = $1 LIMIT 1',
      [req.userId]
    );
    const partnerId = relResult.rows[0]?.partner_user_id || null;

    let countResult, result;
    if (partnerId) {
      countResult = await pool.query(
        'SELECT COUNT(*) as total FROM notes n WHERE n.user_id = $1 OR (n.user_id = $2 AND n.is_private = false)',
        [req.userId, partnerId]
      );
      result = await pool.query(
        `SELECT n.*, u.display_name FROM notes n
         JOIN users u ON n.user_id = u.id
         WHERE n.user_id = $1 OR (n.user_id = $2 AND n.is_private = false)
         ORDER BY n.created_at DESC LIMIT $3 OFFSET $4`,
        [req.userId, partnerId, pageSize, offset]
      );
    } else {
      countResult = await pool.query(
        'SELECT COUNT(*) as total FROM notes n WHERE n.user_id = $1',
        [req.userId]
      );
      result = await pool.query(
        `SELECT n.*, u.display_name FROM notes n
         JOIN users u ON n.user_id = u.id
         WHERE n.user_id = $1 ORDER BY n.created_at DESC LIMIT $2 OFFSET $3`,
        [req.userId, pageSize, offset]
      );
    }

    sendResponse(res, true, 'Notes retrieved', {
      items: result.rows.map(formatNoteForApi),
      total: parseInt(countResult.rows[0].total),
      page,
      page_size: pageSize,
    });
  } catch (err) {
    console.error('Get notes error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Get Note by ID (own note, or partner's public note)
app.get('/api/notes/:id', authenticateToken, async (req, res) => {
  try {
    // Allow fetching partner's public note too
    const relResult = await pool.query(
      'SELECT partner_user_id FROM relationship_info WHERE user_id = $1 LIMIT 1',
      [req.userId]
    );
    const partnerId = relResult.rows[0]?.partner_user_id || null;

    let result;
    if (partnerId) {
      result = await pool.query(
        `SELECT n.*, u.display_name FROM notes n
         JOIN users u ON n.user_id = u.id
         WHERE n.id = $1 AND (n.user_id = $2 OR (n.user_id = $3 AND n.is_private = false))`,
        [req.params.id, req.userId, partnerId]
      );
    } else {
      result = await pool.query(
        `SELECT n.*, u.display_name FROM notes n
         JOIN users u ON n.user_id = u.id
         WHERE n.id = $1 AND n.user_id = $2`,
        [req.params.id, req.userId]
      );
    }

    if (result.rows.length === 0) {
      return sendResponse(res, false, 'Note not found', null, 404);
    }

    sendResponse(res, true, 'Note retrieved', formatNoteForApi(result.rows[0]));
  } catch (err) {
    console.error('Get note error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Update Note
app.put('/api/notes/:id', authenticateToken, async (req, res) => {
  try {
    const { title, content, is_private, tags } = req.body;
    const tagsArr = parseTags(tags);

    const result = await pool.query(
      'UPDATE notes SET title = $1, content = $2, is_private = $3, tags = $4, updated_at = NOW() WHERE id = $5 AND user_id = $6 RETURNING *',
      [title, content, is_private, tagsArr, req.params.id, req.userId]
    );

    if (result.rows.length === 0) {
      return sendResponse(res, false, 'Note not found', null, 404);
    }

    sendResponse(res, true, 'Note updated', formatNoteForApi(result.rows[0]));
  } catch (err) {
    console.error('Update note error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Delete Note
app.delete('/api/notes/:id', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      'DELETE FROM notes WHERE id = $1 AND user_id = $2 RETURNING id',
      [req.params.id, req.userId]
    );

    if (result.rows.length === 0) {
      return sendResponse(res, false, 'Note not found', null, 404);
    }

    sendResponse(res, true, 'Note deleted');
  } catch (err) {
    console.error('Delete note error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// ==================== WISHES ====================

// Create Wish (wishes_priority_check: priority обычно 1-5, 0 недопустим)
app.post('/api/wishes', authenticateToken, async (req, res) => {
  try {
    const { title, description, priority, category, is_completed, is_private, image_urls, emoji } = req.body;
    const validPriority = Math.min(5, Math.max(1, parseInt(priority, 10) || 1));

    const result = await pool.query(
      'INSERT INTO wishes (user_id, title, description, priority, category, is_completed, is_private, image_urls, emoji) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9) RETURNING *',
      [req.userId, title, description, validPriority, category || null, is_completed || false, is_private || false, image_urls || '', emoji || '']
    );

    // Attach display_name
    const userRes = await pool.query('SELECT display_name FROM users WHERE id = $1', [req.userId]);
    const row = { ...result.rows[0], display_name: userRes.rows[0]?.display_name || '' };
    sendResponse(res, true, 'Wish created', row, 201);
  } catch (err) {
    console.error('Create wish error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Get Wishes (own + partner's public)
app.get('/api/wishes', authenticateToken, async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const pageSize = 20;
    const offset = (page - 1) * pageSize;

    const relResult = await pool.query(
      'SELECT partner_user_id FROM relationship_info WHERE user_id = $1 LIMIT 1',
      [req.userId]
    );
    const partnerId = relResult.rows[0]?.partner_user_id || null;

    let countResult, result;
    if (partnerId) {
      countResult = await pool.query(
        'SELECT COUNT(*) as total FROM wishes w WHERE w.user_id = $1 OR (w.user_id = $2 AND w.is_private = false)',
        [req.userId, partnerId]
      );
      result = await pool.query(
        `SELECT w.*, u.display_name FROM wishes w
         JOIN users u ON w.user_id = u.id
         WHERE w.user_id = $1 OR (w.user_id = $2 AND w.is_private = false)
         ORDER BY w.created_at DESC LIMIT $3 OFFSET $4`,
        [req.userId, partnerId, pageSize, offset]
      );
    } else {
      countResult = await pool.query(
        'SELECT COUNT(*) as total FROM wishes w WHERE w.user_id = $1',
        [req.userId]
      );
      result = await pool.query(
        `SELECT w.*, u.display_name FROM wishes w
         JOIN users u ON w.user_id = u.id
         WHERE w.user_id = $1 ORDER BY w.created_at DESC LIMIT $2 OFFSET $3`,
        [req.userId, pageSize, offset]
      );
    }

    sendResponse(res, true, 'Wishes retrieved', {
      items: result.rows,
      total: parseInt(countResult.rows[0].total),
      page,
      page_size: pageSize,
    });
  } catch (err) {
    console.error('Get wishes error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Get Wish by ID (own or partner's public)
app.get('/api/wishes/:id', authenticateToken, async (req, res) => {
  try {
    const relResult = await pool.query(
      'SELECT partner_user_id FROM relationship_info WHERE user_id = $1 LIMIT 1',
      [req.userId]
    );
    const partnerId = relResult.rows[0]?.partner_user_id || null;
    let result;
    if (partnerId) {
      result = await pool.query(
        `SELECT w.*, u.display_name FROM wishes w JOIN users u ON w.user_id = u.id
         WHERE w.id = $1 AND (w.user_id = $2 OR (w.user_id = $3 AND w.is_private = false))`,
        [req.params.id, req.userId, partnerId]
      );
    } else {
      result = await pool.query(
        `SELECT w.*, u.display_name FROM wishes w JOIN users u ON w.user_id = u.id
         WHERE w.id = $1 AND w.user_id = $2`,
        [req.params.id, req.userId]
      );
    }
    if (result.rows.length === 0) return sendResponse(res, false, 'Wish not found', null, 404);
    sendResponse(res, true, 'Wish retrieved', result.rows[0]);
  } catch (err) {
    console.error('Get wish error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Update Wish
app.put('/api/wishes/:id', authenticateToken, async (req, res) => {
  try {
    const { title, description, priority, category, is_private, image_urls, emoji } = req.body;
    const validPriority = Math.min(5, Math.max(1, parseInt(priority, 10) || 1));
    const result = await pool.query(
      `UPDATE wishes SET title = $1, description = $2, priority = $3, category = $4,
       is_private = $5, image_urls = $6, emoji = $7, updated_at = NOW()
       WHERE id = $8 AND user_id = $9 RETURNING *`,
      [title, description, validPriority, category || null, is_private || false, image_urls || '', emoji || '', req.params.id, req.userId]
    );
    if (result.rows.length === 0) return sendResponse(res, false, 'Wish not found', null, 404);
    const userRes = await pool.query('SELECT display_name FROM users WHERE id = $1', [req.userId]);
    const row = { ...result.rows[0], display_name: userRes.rows[0]?.display_name || '' };
    sendResponse(res, true, 'Wish updated', row);
  } catch (err) {
    console.error('Update wish error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Complete Wish
app.post('/api/wishes/:id/complete', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      'UPDATE wishes SET is_completed = true, completed_at = NOW() WHERE id = $1 AND user_id = $2 RETURNING *',
      [req.params.id, req.userId]
    );

    if (result.rows.length === 0) {
      return sendResponse(res, false, 'Wish not found', null, 404);
    }

    const userRes = await pool.query('SELECT display_name FROM users WHERE id = $1', [req.userId]);
    const row = { ...result.rows[0], display_name: userRes.rows[0]?.display_name || '' };
    sendResponse(res, true, 'Wish completed', row);
  } catch (err) {
    console.error('Complete wish error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Delete Wish
app.delete('/api/wishes/:id', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      'DELETE FROM wishes WHERE id = $1 AND user_id = $2 RETURNING id',
      [req.params.id, req.userId]
    );

    if (result.rows.length === 0) {
      return sendResponse(res, false, 'Wish not found', null, 404);
    }

    sendResponse(res, true, 'Wish deleted');
  } catch (err) {
    console.error('Delete wish error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// ==================== FCM TOKEN ====================
app.post('/api/fcm-token', authenticateToken, async (req, res) => {
  try {
    const { fcm_token } = req.body;
    if (!fcm_token) return sendResponse(res, false, 'fcm_token required', null, 400);
    await pool.query(
      `INSERT INTO fcm_tokens (user_id, fcm_token, updated_at)
       VALUES ($1, $2, NOW())
       ON CONFLICT (user_id) DO UPDATE SET fcm_token = $2, updated_at = NOW()`,
      [req.userId, fcm_token]
    );
    sendResponse(res, true, 'FCM token registered', null);
  } catch (err) {
    console.error('Register FCM token error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// ==================== MOODS ====================

// Create Mood
app.post('/api/moods', authenticateToken, async (req, res) => {
  try {
    const { mood_type, date, note } = req.body;
    if (!mood_type) return sendResponse(res, false, 'mood_type is required', null, 400);
    const today = new Date().toISOString().split('T')[0];
    const result = await pool.query(
      'INSERT INTO mood_entries (user_id, mood_type, date, note) VALUES ($1, $2, $3, $4) RETURNING *',
      [req.userId, mood_type, date || today, note || '']
    );
    // Fire-and-forget push to partner
    sendPushToPartner(req.userId, { type: 'partner_mood', moodType: mood_type, destination: 'mood_tracker' }).catch(() => {});
    sendResponse(res, true, 'Mood created', result.rows[0], 201);
  } catch (err) {
    console.error('Create mood error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Update Mood
app.put('/api/moods/:id', authenticateToken, async (req, res) => {
  try {
    const { mood_type, note } = req.body;
    const result = await pool.query(
      'UPDATE mood_entries SET mood_type = $1, note = $2 WHERE id = $3 AND user_id = $4 RETURNING *',
      [mood_type, note || '', req.params.id, req.userId]
    );
    if (result.rows.length === 0) return sendResponse(res, false, 'Mood not found', null, 404);
    sendResponse(res, true, 'Mood updated', result.rows[0]);
  } catch (err) {
    console.error('Update mood error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Get Partner Moods (must be before /:id to avoid route conflict)
app.get('/api/moods/partner', authenticateToken, async (req, res) => {
  try {
    const relResult = await pool.query(
      'SELECT partner_user_id FROM relationship_info WHERE user_id = $1 LIMIT 1',
      [req.userId]
    );
    const partnerId = relResult.rows[0]?.partner_user_id;
    if (!partnerId) {
      return sendResponse(res, true, 'No partner', { items: [], total: 0, page: 1, page_size: 500 });
    }
    const { date, start_date, end_date } = req.query;
    let query = `SELECT me.*, u.display_name FROM mood_entries me
      JOIN users u ON me.user_id = u.id WHERE me.user_id = $1`;
    const params = [partnerId];
    let p = 2;
    if (date) {
      query += ` AND DATE(me.date) = $${p++}`;
      params.push(date);
    } else if (start_date && end_date) {
      query += ` AND me.date >= $${p++} AND me.date <= $${p++}`;
      params.push(start_date, end_date);
    }
    query += ' ORDER BY me.date DESC, me.created_at DESC LIMIT 500';
    const result = await pool.query(query, params);
    sendResponse(res, true, 'Partner moods retrieved', {
      items: result.rows,
      total: result.rows.length,
      page: 1,
      page_size: 500,
    });
  } catch (err) {
    console.error('Get partner moods error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Get My Moods (supports date, start_date+end_date range)
app.get('/api/moods', authenticateToken, async (req, res) => {
  try {
    const { date, start_date, end_date } = req.query;
    const page = parseInt(req.query.page) || 1;
    const pageSize = parseInt(req.query.limit) || 100;
    const offset = (page - 1) * pageSize;

    let countQuery = 'SELECT COUNT(*) as total FROM mood_entries WHERE user_id = $1';
    const countParams = [req.userId];
    let p = 2;
    if (date) {
      countQuery += ` AND DATE(date) = $${p++}`;
      countParams.push(date);
    } else if (start_date && end_date) {
      countQuery += ` AND date >= $${p++} AND date <= $${p++}`;
      countParams.push(start_date, end_date);
    }
    const countResult = await pool.query(countQuery, countParams);

    let query = 'SELECT me.*, u.display_name FROM mood_entries me JOIN users u ON me.user_id = u.id WHERE me.user_id = $1';
    const params = [req.userId];
    p = 2;
    if (date) {
      query += ` AND DATE(date) = $${p++}`;
      params.push(date);
    } else if (start_date && end_date) {
      query += ` AND date >= $${p++} AND date <= $${p++}`;
      params.push(start_date, end_date);
    }
    query += ` ORDER BY date DESC, created_at DESC LIMIT $${p++} OFFSET $${p++}`;
    params.push(pageSize, offset);

    const result = await pool.query(query, params);
    sendResponse(res, true, 'Moods retrieved', {
      items: result.rows,
      total: parseInt(countResult.rows[0].total),
      page,
      page_size: pageSize,
    });
  } catch (err) {
    console.error('Get moods error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Delete Mood
app.delete('/api/moods/:id', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      'DELETE FROM mood_entries WHERE id = $1 AND user_id = $2 RETURNING id',
      [req.params.id, req.userId]
    );
    if (result.rows.length === 0) {
      return sendResponse(res, false, 'Mood not found', null, 404);
    }
    sendResponse(res, true, 'Mood deleted');
  } catch (err) {
    console.error('Delete mood error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// ==================== ACTIVITIES ====================

// Create Activity
app.post('/api/activities', authenticateToken, async (req, res) => {
  try {
    const { title, description, date, category, activity_type, duration_minutes, start_time, note } = req.body;
    const actType = activity_type || category || title || 'other';
    const durMins = parseInt(duration_minutes, 10) || 0;
    const sTime   = start_time || '';
    const noteVal = note || description || '';
    const label   = title || actType;

    const result = await pool.query(
      `INSERT INTO activity_logs
         (user_id, title, description, event_date, category, activity_type, duration_minutes, start_time, note)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
       RETURNING id, user_id, title, description, event_date AS date, category,
                 activity_type, duration_minutes, start_time, note, created_at`,
      [req.userId, label, noteVal, date, actType, actType, durMins, sTime, noteVal]
    );

    const userRes = await pool.query('SELECT display_name FROM users WHERE id = $1', [req.userId]);
    const row = { ...result.rows[0], display_name: userRes.rows[0]?.display_name || '' };
    // Fire-and-forget push to partner
    sendPushToPartner(req.userId, { type: 'partner_activity', count: 1, destination: 'activity_feed' }).catch(() => {});
    sendResponse(res, true, 'Activity created', row, 201);
  } catch (err) {
    console.error('Create activity error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Get Partner Activities — must be before /:id
app.get('/api/activities/partner', authenticateToken, async (req, res) => {
  try {
    const relResult = await pool.query(
      'SELECT partner_user_id FROM relationship_info WHERE user_id = $1 LIMIT 1',
      [req.userId]
    );
    const partnerId = relResult.rows[0]?.partner_user_id;
    if (!partnerId) {
      return sendResponse(res, true, 'No partner', { items: [], total: 0, page: 1, page_size: 500 });
    }
    const { date, start_date, end_date } = req.query;
    let query = `SELECT al.id, al.user_id, al.title, al.description,
                        al.event_date AS date, al.category,
                        al.activity_type, al.duration_minutes, al.start_time, al.note,
                        al.created_at, u.display_name
                 FROM activity_logs al
                 JOIN users u ON al.user_id = u.id
                 WHERE al.user_id = $1`;
    const params = [partnerId];
    let p = 2;
    if (date) {
      query += ` AND DATE(al.event_date) = $${p++}`;
      params.push(date);
    } else if (start_date && end_date) {
      query += ` AND al.event_date >= $${p++} AND al.event_date <= $${p++}`;
      params.push(start_date, end_date);
    }
    query += ' ORDER BY al.event_date DESC, al.created_at DESC LIMIT 500';
    const result = await pool.query(query, params);
    sendResponse(res, true, 'Partner activities retrieved', {
      items: result.rows,
      total: result.rows.length,
      page: 1,
      page_size: 500,
    });
  } catch (err) {
    console.error('Get partner activities error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Get My Activities
app.get('/api/activities', authenticateToken, async (req, res) => {
  try {
    const { date, start_date, end_date } = req.query;
    const page = parseInt(req.query.page) || 1;
    const pageSize = parseInt(req.query.limit) || 100;
    const offset = (page - 1) * pageSize;

    let countQuery = 'SELECT COUNT(*) as total FROM activity_logs WHERE user_id = $1';
    const countParams = [req.userId];
    let p = 2;
    if (date) {
      countQuery += ` AND DATE(event_date) = $${p++}`;
      countParams.push(date);
    } else if (start_date && end_date) {
      countQuery += ` AND event_date >= $${p++} AND event_date <= $${p++}`;
      countParams.push(start_date, end_date);
    }
    const countResult = await pool.query(countQuery, countParams);

    let query = `SELECT al.id, al.user_id, al.title, al.description,
                        al.event_date AS date, al.category,
                        al.activity_type, al.duration_minutes, al.start_time, al.note,
                        al.created_at, u.display_name
                 FROM activity_logs al
                 JOIN users u ON al.user_id = u.id
                 WHERE al.user_id = $1`;
    const params = [req.userId];
    p = 2;
    if (date) {
      query += ` AND DATE(al.event_date) = $${p++}`;
      params.push(date);
    } else if (start_date && end_date) {
      query += ` AND al.event_date >= $${p++} AND al.event_date <= $${p++}`;
      params.push(start_date, end_date);
    }
    query += ` ORDER BY al.event_date DESC, al.created_at DESC LIMIT $${p++} OFFSET $${p++}`;
    params.push(pageSize, offset);

    const result = await pool.query(query, params);
    sendResponse(res, true, 'Activities retrieved', {
      items: result.rows,
      total: parseInt(countResult.rows[0].total),
      page,
      page_size: pageSize,
    });
  } catch (err) {
    console.error('Get activities error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Delete Activity
app.delete('/api/activities/:id', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      'DELETE FROM activity_logs WHERE id = $1 AND user_id = $2 RETURNING id',
      [req.params.id, req.userId]
    );
    if (result.rows.length === 0) {
      return sendResponse(res, false, 'Activity not found', null, 404);
    }
    sendResponse(res, true, 'Activity deleted');
  } catch (err) {
    console.error('Delete activity error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// ==================== CYCLES ====================

// helper – stringify only if the value is an object (so plain strings pass through as NULL)
function cycleJsonParam(val) {
  if (val === null || val === undefined) return JSON.stringify({});
  if (typeof val === 'string') {
    // already a JSON string from the client – just return it
    return val;
  }
  return JSON.stringify(val);
}

// Create Cycle
app.post('/api/cycles', authenticateToken, async (req, res) => {
  try {
    const { cycle_start_date, cycle_duration, period_duration, symptoms, mood, notes } = req.body;
    const symptomsJson = cycleJsonParam(symptoms);
    const moodJson     = cycleJsonParam(mood);

    const result = await pool.query(
      `INSERT INTO menstrual_cycles
         (user_id, cycle_start_date, cycle_duration, period_duration, symptoms, mood, notes)
       VALUES ($1, $2, $3, $4, $5::jsonb, $6::jsonb, $7)
       RETURNING *`,
      [req.userId, cycle_start_date, cycle_duration || 28, period_duration || 5,
       symptomsJson, moodJson, notes || '']
    );
    // Fire-and-forget push to partner
    sendPushToPartner(req.userId, { type: 'partner_cycle', isNewCycle: true, destination: 'menstrual_calendar' }).catch(() => {});
    sendResponse(res, true, 'Cycle created', result.rows[0], 201);
  } catch (err) {
    console.error('Create cycle error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Get Cycles (all, for calendar view)
app.get('/api/cycles', authenticateToken, async (req, res) => {
  try {
    const limit = parseInt(req.query.limit) || 100;

    const countResult = await pool.query(
      'SELECT COUNT(*) as total FROM menstrual_cycles WHERE user_id = $1',
      [req.userId]
    );

    const result = await pool.query(
      'SELECT * FROM menstrual_cycles WHERE user_id = $1 ORDER BY cycle_start_date DESC LIMIT $2',
      [req.userId, limit]
    );

    sendResponse(res, true, 'Cycles retrieved', {
      items: result.rows,
      total: parseInt(countResult.rows[0].total),
      page: 1,
      page_size: limit,
    });
  } catch (err) {
    console.error('Get cycles error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Get Latest Cycle
app.get('/api/cycles/latest', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT * FROM menstrual_cycles WHERE user_id = $1 ORDER BY cycle_start_date DESC LIMIT 1',
      [req.userId]
    );

    if (result.rows.length === 0) {
      return sendResponse(res, false, 'No cycle data found', null, 404);
    }

    sendResponse(res, true, 'Latest cycle retrieved', result.rows[0]);
  } catch (err) {
    console.error('Get latest cycle error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Get Partner Cycles (read-only – the second user views the girl's data)
app.get('/api/cycles/partner', authenticateToken, async (req, res) => {
  try {
    // Find partner user_id via relationship_info
    const relResult = await pool.query(
      `SELECT partner_user_id FROM relationship_info WHERE user_id = $1
       UNION
       SELECT user_id FROM relationship_info WHERE partner_user_id = $1
       LIMIT 1`,
      [req.userId]
    );

    if (relResult.rows.length === 0) {
      return sendResponse(res, true, 'No partner found', { items: [], total: 0, page: 1, page_size: 100 });
    }

    const partnerId = relResult.rows[0].partner_user_id || relResult.rows[0].user_id;
    const limit = parseInt(req.query.limit) || 100;

    const countResult = await pool.query(
      'SELECT COUNT(*) as total FROM menstrual_cycles WHERE user_id = $1',
      [partnerId]
    );

    const result = await pool.query(
      'SELECT * FROM menstrual_cycles WHERE user_id = $1 ORDER BY cycle_start_date DESC LIMIT $2',
      [partnerId, limit]
    );

    sendResponse(res, true, 'Partner cycles retrieved', {
      items: result.rows,
      total: parseInt(countResult.rows[0].total),
      page: 1,
      page_size: limit,
    });
  } catch (err) {
    console.error('Get partner cycles error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Update Cycle (full update)
app.put('/api/cycles/:id', authenticateToken, async (req, res) => {
  try {
    const { id } = req.params;
    const { cycle_start_date, cycle_duration, period_duration, symptoms, mood, notes } = req.body;
    const symptomsJson = cycleJsonParam(symptoms);
    const moodJson     = cycleJsonParam(mood);

    const result = await pool.query(
      `UPDATE menstrual_cycles
       SET cycle_start_date = COALESCE($2, cycle_start_date),
           cycle_duration   = COALESCE($3, cycle_duration),
           period_duration  = COALESCE($4, period_duration),
           symptoms         = COALESCE($5::jsonb, symptoms),
           mood             = COALESCE($6::jsonb, mood),
           notes            = COALESCE($7, notes)
       WHERE id = $1 AND user_id = $8
       RETURNING *`,
      [id, cycle_start_date, cycle_duration, period_duration,
       symptomsJson, moodJson, notes, req.userId]
    );

    if (result.rows.length === 0) {
      return sendResponse(res, false, 'Cycle not found or not authorised', null, 404);
    }

    sendResponse(res, true, 'Cycle updated', result.rows[0]);
  } catch (err) {
    console.error('Update cycle error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Patch Cycle symptoms/mood for a specific date
app.patch('/api/cycles/:id', authenticateToken, async (req, res) => {
  try {
    const { id } = req.params;
    const { date, symptoms_day, mood_day } = req.body;
    // Merge the per-day data into the jsonb maps

    if (symptoms_day !== undefined) {
      await pool.query(
        `UPDATE menstrual_cycles
         SET symptoms = jsonb_set(COALESCE(symptoms,'{}'), $2::text[], $3::jsonb, true)
         WHERE id = $1 AND user_id = $4`,
        [id, `{${date}}`, JSON.stringify(symptoms_day), req.userId]
      );
    }

    if (mood_day !== undefined) {
      await pool.query(
        `UPDATE menstrual_cycles
         SET mood = jsonb_set(COALESCE(mood,'{}'), $2::text[], $3::jsonb, true)
         WHERE id = $1 AND user_id = $4`,
        [id, `{${date}}`, JSON.stringify(mood_day), req.userId]
      );
    }

    const result = await pool.query(
      'SELECT * FROM menstrual_cycles WHERE id = $1 AND user_id = $2',
      [id, req.userId]
    );

    if (result.rows.length === 0) {
      return sendResponse(res, false, 'Cycle not found', null, 404);
    }
    // Fire-and-forget push to partner (only when actual data was logged)
    if (symptoms_day !== undefined || mood_day !== undefined) {
      sendPushToPartner(req.userId, { type: 'partner_cycle', isNewCycle: false, destination: 'menstrual_calendar' }).catch(() => {});
    }
    sendResponse(res, true, 'Cycle day updated', result.rows[0]);
  } catch (err) {
    console.error('Patch cycle error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Delete Cycle
app.delete('/api/cycles/:id', authenticateToken, async (req, res) => {
  try {
    const { id } = req.params;
    const result = await pool.query(
      'DELETE FROM menstrual_cycles WHERE id = $1 AND user_id = $2 RETURNING id',
      [id, req.userId]
    );

    if (result.rows.length === 0) {
      return sendResponse(res, false, 'Cycle not found', null, 404);
    }

    sendResponse(res, true, 'Cycle deleted', null);
  } catch (err) {
    console.error('Delete cycle error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// ==================== CALENDARS ====================

// Create Calendar (color_hex varchar(7) — максимум 7 символов, например #RRGGBB)
function normalizeColorHex(hex) {
  if (!hex || typeof hex !== 'string') return '#000000';
  const s = hex.trim();
  if (s.startsWith('#')) return s.substring(0, 7);
  return ('#' + s).substring(0, 7);
}

// Create Calendar
app.post('/api/calendars', authenticateToken, async (req, res) => {
  try {
    const { name, description, type, color_hex } = req.body;
    const color = normalizeColorHex(color_hex);

    const result = await pool.query(
      'INSERT INTO custom_calendars (user_id, name, description, type, color_hex) VALUES ($1, $2, $3, $4, $5) RETURNING *',
      [req.userId, name || '', description || '', type || 'default', color]
    );

    sendResponse(res, true, 'Calendar created', result.rows[0], 201);
  } catch (err) {
    console.error('Create calendar error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Get Calendars
app.get('/api/calendars', authenticateToken, async (req, res) => {
  try {
    const { type } = req.query;
    const page = parseInt(req.query.page) || 1;
    const pageSize = 20;
    const offset = (page - 1) * pageSize;

    let query = 'SELECT COUNT(*) as total FROM custom_calendars WHERE user_id = $1';
    let countParams = [req.userId];

    if (type) {
      query += ' AND type = $2';
      countParams.push(type);
    }

    const countResult = await pool.query(query, countParams);

    query = 'SELECT * FROM custom_calendars WHERE user_id = $1';
    let params = [req.userId];
    let paramCount = 2;

    if (type) {
      query += ` AND type = $${paramCount}`;
      params.push(type);
      paramCount++;
    }

    query += ` ORDER BY created_at DESC LIMIT $${paramCount} OFFSET $${paramCount + 1}`;
    params.push(pageSize, offset);

    const result = await pool.query(query, params);

    sendResponse(res, true, 'Calendars retrieved', {
      items: result.rows,
      total: parseInt(countResult.rows[0].total),
      page,
      page_size: pageSize,
    });
  } catch (err) {
    console.error('Get calendars error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Get Partner Calendars — must be declared BEFORE /:id to avoid Express matching 'partner' as id
app.get('/api/calendars/partner', authenticateToken, async (req, res) => {
  try {
    const relResult = await pool.query(
      `SELECT CASE WHEN user_id = $1 THEN partner_user_id ELSE user_id END AS partner_id
         FROM relationship_info WHERE user_id = $1 OR partner_user_id = $1 LIMIT 1`,
      [req.userId]
    );
    if (relResult.rows.length === 0) {
      return sendResponse(res, true, 'No partner', { items: [], total: 0, page: 1, page_size: 100 });
    }
    const partnerId = relResult.rows[0].partner_id;
    const result = await pool.query(
      'SELECT * FROM custom_calendars WHERE user_id = $1 ORDER BY created_at DESC',
      [partnerId]
    );
    sendResponse(res, true, 'Partner calendars retrieved', {
      items: result.rows, total: result.rows.length, page: 1, page_size: 100
    });
  } catch (err) {
    console.error('Get partner calendars error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Get Calendar by ID
app.get('/api/calendars/:id', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT * FROM custom_calendars WHERE id = $1 AND user_id = $2',
      [req.params.id, req.userId]
    );

    if (result.rows.length === 0) {
      return sendResponse(res, false, 'Calendar not found', null, 404);
    }

    sendResponse(res, true, 'Calendar retrieved', result.rows[0]);
  } catch (err) {
    console.error('Get calendar error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Delete Calendar
app.delete('/api/calendars/:id', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      'DELETE FROM custom_calendars WHERE id = $1 AND user_id = $2 RETURNING id',
      [req.params.id, req.userId]
    );
    if (result.rows.length === 0) return sendResponse(res, false, 'Calendar not found', null, 404);
    sendResponse(res, true, 'Calendar deleted', { id: result.rows[0].id });
  } catch (err) {
    console.error('Delete calendar error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Helper: check if user has access to a calendar (owns it or is partner of owner)
async function checkCalendarAccess(calendarId, userId) {
  const result = await pool.query(
    `SELECT cc.id FROM custom_calendars cc
     LEFT JOIN relationship_info ri
       ON (ri.user_id = $2 AND ri.partner_user_id = cc.user_id)
       OR (ri.partner_user_id = $2 AND ri.user_id = cc.user_id)
     WHERE cc.id = $1 AND (cc.user_id = $2 OR ri.id IS NOT NULL)
     LIMIT 1`,
    [calendarId, userId]
  );
  return result.rows.length > 0;
}

// Get Events for Calendar
app.get('/api/calendars/:id/events', authenticateToken, async (req, res) => {
  try {
    if (!(await checkCalendarAccess(req.params.id, req.userId)))
      return sendResponse(res, false, 'Access denied', null, 403);
    const result = await pool.query(
      'SELECT * FROM custom_calendar_events WHERE calendar_id = $1 ORDER BY event_date ASC',
      [req.params.id]
    );
    sendResponse(res, true, 'Events retrieved', {
      items: result.rows, total: result.rows.length, page: 1, page_size: 1000
    });
  } catch (err) {
    console.error('Get events error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Mark a Day (create event)
app.post('/api/calendars/:id/events', authenticateToken, async (req, res) => {
  try {
    if (!(await checkCalendarAccess(req.params.id, req.userId)))
      return sendResponse(res, false, 'Access denied', null, 403);
    const { event_date, title, description } = req.body;
    const result = await pool.query(
      'INSERT INTO custom_calendar_events (calendar_id, event_date, title, description) VALUES ($1, $2, $3, $4) RETURNING *',
      [req.params.id, event_date, title || '', description || '']
    );
    sendResponse(res, true, 'Event created', result.rows[0], 201);
  } catch (err) {
    console.error('Create event error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Delete Event (unmark a day) — must be declared BEFORE DELETE /:id to avoid conflict
app.delete('/api/calendars/events/:eventId', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      `DELETE FROM custom_calendar_events cce
       USING custom_calendars cc
       WHERE cce.id = $1 AND cce.calendar_id = cc.id
         AND (cc.user_id = $2 OR EXISTS (
           SELECT 1 FROM relationship_info ri
           WHERE (ri.user_id = $2 AND ri.partner_user_id = cc.user_id)
              OR (ri.partner_user_id = $2 AND ri.user_id = cc.user_id)
         ))
       RETURNING cce.id`,
      [req.params.eventId, req.userId]
    );
    if (result.rows.length === 0)
      return sendResponse(res, false, 'Event not found or access denied', null, 404);
    sendResponse(res, true, 'Event deleted', { id: result.rows[0].id });
  } catch (err) {
    console.error('Delete event error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// ==================== RELATIONSHIP ====================

// Create or Get Relationship
app.get('/api/relationship', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT ri.*, u.display_name AS partner_display_name
       FROM relationship_info ri
       LEFT JOIN users u ON u.id = ri.partner_user_id
       WHERE ri.user_id = $1 LIMIT 1`,
      [req.userId]
    );

    if (result.rows.length === 0) {
      return sendResponse(res, false, 'Relationship not found', null, 404);
    }

    sendResponse(res, true, 'Relationship retrieved', result.rows[0]);
  } catch (err) {
    console.error('Get relationship error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Update Relationship
app.put('/api/relationship', authenticateToken, async (req, res) => {
  try {
    const { relationship_start_date, first_kiss_date, anniversary_date, my_birthday, partner_birthday } = req.body;

    // Try to update existing
    let result = await pool.query(
      `UPDATE relationship_info
       SET relationship_start_date = $1, first_kiss_date = $2, anniversary_date = $3,
           my_birthday = $4, partner_birthday = $5, updated_at = NOW()
       WHERE user_id = $6 RETURNING *`,
      [relationship_start_date, first_kiss_date, anniversary_date, my_birthday || null, partner_birthday || null, req.userId]
    );

    // If no rows updated, create new
    if (result.rows.length === 0) {
      result = await pool.query(
        `INSERT INTO relationship_info (user_id, relationship_start_date, first_kiss_date, anniversary_date, my_birthday, partner_birthday)
         VALUES ($1, $2, $3, $4, $5, $6) RETURNING *`,
        [req.userId, relationship_start_date, first_kiss_date, anniversary_date, my_birthday || null, partner_birthday || null]
      );
    }

    // Re-fetch with partner display_name joined
    const full = await pool.query(
      `SELECT ri.*, u.display_name AS partner_display_name
       FROM relationship_info ri
       LEFT JOIN users u ON u.id = ri.partner_user_id
       WHERE ri.user_id = $1 LIMIT 1`,
      [req.userId]
    );

    sendResponse(res, true, 'Relationship updated', full.rows[0] || result.rows[0]);
  } catch (err) {
    console.error('Update relationship error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// ==================== PARTNER PAIRING ====================

// Generate a 6-char alphanumeric pairing code (valid 30 min)
app.post('/api/partner/generate-code', authenticateToken, async (req, res) => {
  try {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    let code = '';
    for (let i = 0; i < 6; i++) code += chars[Math.floor(Math.random() * chars.length)];

    await pool.query(
      `UPDATE users SET pairing_code = $1, pairing_code_expires_at = NOW() + INTERVAL '30 minutes' WHERE id = $2`,
      [code, req.userId]
    );

    sendResponse(res, true, 'Code generated', { code, expires_minutes: 30 });
  } catch (err) {
    console.error('Generate code error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Link to a partner using their code — creates bidirectional relationship_info entries
app.post('/api/partner/link', authenticateToken, async (req, res) => {
  try {
    const { code } = req.body;
    if (!code) return sendResponse(res, false, 'Code is required', null, 400);

    // Find the user who generated this code
    const codeResult = await pool.query(
      `SELECT id, display_name, username FROM users
       WHERE pairing_code = $1 AND pairing_code_expires_at > NOW()`,
      [code.toUpperCase().trim()]
    );

    if (codeResult.rows.length === 0) {
      return sendResponse(res, false, 'Code is invalid or expired', null, 404);
    }

    const partner = codeResult.rows[0];
    if (partner.id === req.userId) {
      return sendResponse(res, false, 'Cannot pair with yourself', null, 400);
    }

    // Create/update relationship_info for current user → partner
    await pool.query(
      `INSERT INTO relationship_info (user_id, partner_user_id)
       VALUES ($1, $2)
       ON CONFLICT (user_id) DO UPDATE SET partner_user_id = $2`,
      [req.userId, partner.id]
    );

    // Create/update relationship_info for partner → current user (bidirectional)
    await pool.query(
      `INSERT INTO relationship_info (user_id, partner_user_id)
       VALUES ($1, $2)
       ON CONFLICT (user_id) DO UPDATE SET partner_user_id = $2`,
      [partner.id, req.userId]
    );

    // Invalidate the used code
    await pool.query(
      `UPDATE users SET pairing_code = NULL, pairing_code_expires_at = NULL WHERE id = $1`,
      [partner.id]
    );

    sendResponse(res, true, 'Partner linked successfully', {
      partner_id: partner.id,
      partner_name: partner.display_name,
      partner_username: partner.username
    });
  } catch (err) {
    console.error('Link partner error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// ==================== ERROR HANDLING ====================

app.use((req, res) => {
  sendResponse(res, false, 'Endpoint not found', null, 404);
});

// ==================== SERVER START ====================

const PORT = process.env.API_PORT || 3005;

// Auto-migrate: ensure is_private column exists on wishes
pool.query(`ALTER TABLE wishes ADD COLUMN IF NOT EXISTS is_private BOOLEAN NOT NULL DEFAULT false`)
  .then(() => console.log('wishes.is_private column ready'))
  .catch(err => console.error('Migration error:', err));
pool.query(`ALTER TABLE wishes ADD COLUMN IF NOT EXISTS emoji TEXT NOT NULL DEFAULT ''`)
  .then(() => console.log('wishes.emoji column ready'))
  .catch(err => console.error('Migration error (emoji):', err));
pool.query(`ALTER TABLE wishes ADD COLUMN IF NOT EXISTS image_urls TEXT NOT NULL DEFAULT ''`)
  .then(() => console.log('wishes.image_urls column ready'))
  .catch(err => console.error('Migration error (image_urls):', err));
pool.query(`ALTER TABLE activity_logs ADD COLUMN IF NOT EXISTS activity_type VARCHAR(50) NOT NULL DEFAULT ''`)
  .then(() => console.log('activity_logs.activity_type column ready'))
  .catch(err => console.error('Migration error (activity_type):', err));
pool.query(`ALTER TABLE activity_logs ADD COLUMN IF NOT EXISTS duration_minutes INTEGER NOT NULL DEFAULT 0`)
  .then(() => console.log('activity_logs.duration_minutes column ready'))
  .catch(err => console.error('Migration error (duration_minutes):', err));
pool.query(`ALTER TABLE activity_logs ADD COLUMN IF NOT EXISTS start_time VARCHAR(5) NOT NULL DEFAULT ''`)
  .then(() => console.log('activity_logs.start_time column ready'))
  .catch(err => console.error('Migration error (start_time):', err));
pool.query(`ALTER TABLE activity_logs ADD COLUMN IF NOT EXISTS note TEXT NOT NULL DEFAULT ''`)
  .then(() => console.log('activity_logs.note column ready'))
  .catch(err => console.error('Migration error (note):', err));

pool.query(`
  CREATE TABLE IF NOT EXISTS fcm_tokens (
    user_id    INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    fcm_token  TEXT NOT NULL,
    updated_at TIMESTAMP DEFAULT NOW()
  )
`).then(() => console.log('fcm_tokens table ready'))
  .catch(err => console.error('Migration error (fcm_tokens):', err));

pool.query(`ALTER TABLE users ADD COLUMN IF NOT EXISTS pairing_code VARCHAR(6)`)
  .then(() => console.log('users.pairing_code column ready'))
  .catch(err => console.error('Migration error (pairing_code):', err));
pool.query(`ALTER TABLE users ADD COLUMN IF NOT EXISTS pairing_code_expires_at TIMESTAMPTZ`)
  .then(() => console.log('users.pairing_code_expires_at column ready'))
  .catch(err => console.error('Migration error (pairing_code_expires_at):', err));
pool.query(`ALTER TABLE relationship_info ADD COLUMN IF NOT EXISTS partner_user_id INTEGER REFERENCES users(id)`)
  .then(() => console.log('relationship_info.partner_user_id column ready'))
  .catch(err => console.error('Migration error (partner_user_id):', err));
pool.query(`ALTER TABLE relationship_info ADD COLUMN IF NOT EXISTS my_birthday DATE`)
  .then(() => console.log('relationship_info.my_birthday column ready'))
  .catch(err => console.error('Migration error (my_birthday):', err));
pool.query(`ALTER TABLE relationship_info ADD COLUMN IF NOT EXISTS partner_birthday DATE`)
  .then(() => console.log('relationship_info.partner_birthday column ready'))
  .catch(err => console.error('Migration error (partner_birthday):', err));
pool.query(`DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'relationship_info_user_id_key') THEN ALTER TABLE relationship_info ADD CONSTRAINT relationship_info_user_id_key UNIQUE (user_id); END IF; END $$`)
  .then(() => console.log('relationship_info unique user_id constraint ready'))
  .catch(err => console.error('Migration error (relationship_info unique):', err));

app.listen(PORT, () => {
  console.log(`LoveApp API Server running on port ${PORT}`);
  console.log(`Database: ${process.env.PGHOST}:${process.env.PGPORT}/${process.env.PGDATABASE}`);
});
