// server.js - Express Backend for LoveApp

const express = require('express');
const pg = require('pg');
const cors = require('cors');
const dotenv = require('dotenv');
const bcryptjs = require('bcryptjs');
const jwt = require('jsonwebtoken');
const multer = require('multer');
const path = require('path');

dotenv.config();

const app = express();

// Middleware
app.use(cors());
app.use(express.json());

// Static file serving for uploaded files
const storagePath = process.env.STORAGE_PATH || path.join(__dirname, 'uploads');
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
  limits: { fileSize: parseInt(process.env.MAX_FILE_SIZE || String(5 * 1024 * 1024)) }
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

// ==================== NOTES ====================

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

    sendResponse(res, true, 'Note created', result.rows[0], 201);
  } catch (err) {
    console.error('Create note error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Get Notes
app.get('/api/notes', authenticateToken, async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const pageSize = 20;
    const offset = (page - 1) * pageSize;

    const countResult = await pool.query(
      'SELECT COUNT(*) as total FROM notes WHERE user_id = $1',
      [req.userId]
    );

    const result = await pool.query(
      'SELECT * FROM notes WHERE user_id = $1 ORDER BY created_at DESC LIMIT $2 OFFSET $3',
      [req.userId, pageSize, offset]
    );

    sendResponse(res, true, 'Notes retrieved', {
      items: result.rows,
      total: parseInt(countResult.rows[0].total),
      page,
      page_size: pageSize,
    });
  } catch (err) {
    console.error('Get notes error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Get Note by ID
app.get('/api/notes/:id', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT * FROM notes WHERE id = $1 AND user_id = $2',
      [req.params.id, req.userId]
    );

    if (result.rows.length === 0) {
      return sendResponse(res, false, 'Note not found', null, 404);
    }

    sendResponse(res, true, 'Note retrieved', result.rows[0]);
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

    sendResponse(res, true, 'Note updated', result.rows[0]);
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

// Create Wish
app.post('/api/wishes', authenticateToken, async (req, res) => {
  try {
    const { title, description, priority, category, is_completed } = req.body;

    const result = await pool.query(
      'INSERT INTO wishes (user_id, title, description, priority, category, is_completed) VALUES ($1, $2, $3, $4, $5, $6) RETURNING *',
      [req.userId, title, description, priority, category, is_completed || false]
    );

    sendResponse(res, true, 'Wish created', result.rows[0], 201);
  } catch (err) {
    console.error('Create wish error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Get Wishes
app.get('/api/wishes', authenticateToken, async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const pageSize = 20;
    const offset = (page - 1) * pageSize;

    const countResult = await pool.query(
      'SELECT COUNT(*) as total FROM wishes WHERE user_id = $1',
      [req.userId]
    );

    const result = await pool.query(
      'SELECT * FROM wishes WHERE user_id = $1 ORDER BY created_at DESC LIMIT $2 OFFSET $3',
      [req.userId, pageSize, offset]
    );

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

    sendResponse(res, true, 'Wish completed', result.rows[0]);
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

// ==================== MOODS ====================

// Create Mood
app.post('/api/moods', authenticateToken, async (req, res) => {
  try {
    const { mood_type, date, note } = req.body;

    const result = await pool.query(
      'INSERT INTO mood_entries (user_id, mood_type, date, note) VALUES ($1, $2, $3, $4) RETURNING *',
      [req.userId, mood_type, date, note]
    );

    sendResponse(res, true, 'Mood created', result.rows[0], 201);
  } catch (err) {
    console.error('Create mood error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Get Moods
app.get('/api/moods', authenticateToken, async (req, res) => {
  try {
    const { date } = req.query;
    const page = parseInt(req.query.page) || 1;
    const pageSize = 20;
    const offset = (page - 1) * pageSize;

    let query = 'SELECT COUNT(*) as total FROM mood_entries WHERE user_id = $1';
    let countParams = [req.userId];

    if (date) {
      query += ' AND DATE(date) = $2';
      countParams.push(date);
    }

    const countResult = await pool.query(query, countParams);

    query = 'SELECT * FROM mood_entries WHERE user_id = $1';
    let params = [req.userId];
    let paramCount = 2;

    if (date) {
      query += ` AND DATE(date) = $${paramCount}`;
      params.push(date);
      paramCount++;
    }

    query += ` ORDER BY date DESC LIMIT $${paramCount} OFFSET $${paramCount + 1}`;
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

// Create Activity (в схеме БД колонка называется event_date, не date)
app.post('/api/activities', authenticateToken, async (req, res) => {
  try {
    const { title, description, date, category } = req.body;

    const result = await pool.query(
      'INSERT INTO activity_logs (user_id, title, description, event_date, category) VALUES ($1, $2, $3, $4, $5) RETURNING id, user_id, title, description, event_date AS date, category, created_at, image_urls',
      [req.userId, title, description, date, category]
    );

    sendResponse(res, true, 'Activity created', result.rows[0], 201);
  } catch (err) {
    console.error('Create activity error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Get Activities (колонка в БД: event_date)
app.get('/api/activities', authenticateToken, async (req, res) => {
  try {
    const { date } = req.query;
    const page = parseInt(req.query.page) || 1;
    const pageSize = 20;
    const offset = (page - 1) * pageSize;

    let query = 'SELECT COUNT(*) as total FROM activity_logs WHERE user_id = $1';
    let countParams = [req.userId];

    if (date) {
      query += ' AND DATE(event_date) = $2';
      countParams.push(date);
    }

    const countResult = await pool.query(query, countParams);

    query = 'SELECT * FROM activity_logs WHERE user_id = $1';
    let params = [req.userId];
    let paramCount = 2;

    if (date) {
      query += ` AND DATE(event_date) = $${paramCount}`;
      params.push(date);
      paramCount++;
    }

    query = 'SELECT id, user_id, title, description, event_date AS date, category, created_at, image_urls FROM activity_logs WHERE user_id = $1';
    if (date) query += ` AND DATE(event_date) = $${paramCount}`;
    query += ` ORDER BY event_date DESC LIMIT $${paramCount} OFFSET $${paramCount + 1}`;
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

// Create Cycle
app.post('/api/cycles', authenticateToken, async (req, res) => {
  try {
    const { cycle_start_date, cycle_duration, period_duration, symptoms, mood } = req.body;

    const result = await pool.query(
      'INSERT INTO menstrual_cycles (user_id, cycle_start_date, cycle_duration, period_duration, symptoms, mood) VALUES ($1, $2, $3, $4, $5, $6) RETURNING *',
      [req.userId, cycle_start_date, cycle_duration, period_duration, symptoms, mood]
    );

    sendResponse(res, true, 'Cycle created', result.rows[0], 201);
  } catch (err) {
    console.error('Create cycle error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// Get Cycles
app.get('/api/cycles', authenticateToken, async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const pageSize = 20;
    const offset = (page - 1) * pageSize;

    const countResult = await pool.query(
      'SELECT COUNT(*) as total FROM menstrual_cycles WHERE user_id = $1',
      [req.userId]
    );

    const result = await pool.query(
      'SELECT * FROM menstrual_cycles WHERE user_id = $1 ORDER BY cycle_start_date DESC LIMIT $2 OFFSET $3',
      [req.userId, pageSize, offset]
    );

    sendResponse(res, true, 'Cycles retrieved', {
      items: result.rows,
      total: parseInt(countResult.rows[0].total),
      page,
      page_size: pageSize,
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

// ==================== CALENDARS ====================

// Create Calendar
app.post('/api/calendars', authenticateToken, async (req, res) => {
  try {
    const { name, description, type, color_hex } = req.body;

    const result = await pool.query(
      'INSERT INTO custom_calendars (user_id, name, description, type, color_hex) VALUES ($1, $2, $3, $4, $5) RETURNING *',
      [req.userId, name, description, type, color_hex]
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

// ==================== RELATIONSHIP ====================

// Create or Get Relationship
app.get('/api/relationship', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT * FROM relationship_info WHERE user_id = $1 LIMIT 1',
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
    const { relationship_start_date, first_kiss_date, anniversary_date } = req.body;

    // Try to update existing
    let result = await pool.query(
      'UPDATE relationship_info SET relationship_start_date = $1, first_kiss_date = $2, anniversary_date = $3, updated_at = NOW() WHERE user_id = $4 RETURNING *',
      [relationship_start_date, first_kiss_date, anniversary_date, req.userId]
    );

    // If no rows updated, create new
    if (result.rows.length === 0) {
      result = await pool.query(
        'INSERT INTO relationship_info (user_id, relationship_start_date, first_kiss_date, anniversary_date) VALUES ($1, $2, $3, $4) RETURNING *',
        [req.userId, relationship_start_date, first_kiss_date, anniversary_date]
      );
    }

    sendResponse(res, true, 'Relationship updated', result.rows[0]);
  } catch (err) {
    console.error('Update relationship error:', err);
    sendResponse(res, false, 'Internal server error', null, 500);
  }
});

// ==================== ERROR HANDLING ====================

app.use((req, res) => {
  sendResponse(res, false, 'Endpoint not found', null, 404);
});

// ==================== SERVER START ====================

const PORT = process.env.API_PORT || 3005;
app.listen(PORT, () => {
  console.log(`LoveApp API Server running on port ${PORT}`);
  console.log(`Database: ${process.env.PGHOST}:${process.env.PGPORT}/${process.env.PGDATABASE}`);
});
