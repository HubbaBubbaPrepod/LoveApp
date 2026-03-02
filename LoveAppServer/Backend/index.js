// index.js – LoveApp API Server (modular, real-time, Redis-backed)
// This replaces the monolithic server.js with a clean, scalable architecture.

require('dotenv').config();
const express = require('express');
const http    = require('http');
const cors    = require('cors');
const path    = require('path');
const fs      = require('fs');
const multer  = require('multer');
const { Server } = require('socket.io');

// ── Internal modules ──────────────────────────────────────────────────────────
const logger = require('./src/config/logger');
const { initRedis } = require('./src/config/redis');
const { initFirebase } = require('./src/config/firebase');
const { runMigrations } = require('./src/db/migrate');
const { verifyToken } = require('./src/utils/auth');
const pool = require('./src/config/db');

const {
  helmetMiddleware,
  apiLimiter,
  authLimiter,
  morganMiddleware,
  promMiddleware,
  errorHandler,
} = require('./src/middleware/index');

const { initSyncHandler, getCoupleKey } = require('./src/socket/syncHandler');

// ── Routes ───────────────────────────────────────────────────────────────────
const authRoutes         = require('./src/routes/auth');
const notesRoutes        = require('./src/routes/notes');
const wishesRoutes       = require('./src/routes/wishes');
const moodsRoutes        = require('./src/routes/moods');
const activitiesRoutes   = require('./src/routes/activities');
const cyclesRoutes       = require('./src/routes/cycles');
const calendarsRoutes    = require('./src/routes/calendars');
const relationshipRoutes = require('./src/routes/relationship');
const artRoutes          = require('./src/routes/art');
const uploadRoutes       = require('./src/routes/upload');
const adminRoutes        = require('./src/routes/admin');
const metricsRoute       = require('./src/routes/metrics');

// ─────────────────────────────────────────────────────────────────────────────
async function bootstrap() {
  // 1. External services
  await initRedis();
  initFirebase();

  // 2. DB migrations (non-blocking — warn on failure but don't crash)
  try { await runMigrations(); } catch (e) { logger.error(`Migrations error: ${e.message}`); }

  // 3. Express app
  const app    = express();
  const server = http.createServer(app);

  // ── File storage ────────────────────────────────────────────────────────────
  const storagePath = process.env.STORAGE_PATH || path.join(__dirname, 'uploads');
  fs.mkdirSync(storagePath, { recursive: true });

  const uploadMiddleware = multer({
    storage: multer.diskStorage({
      destination: (req, file, cb) => cb(null, storagePath),
      filename:    (req, file, cb) => {
        const ext  = path.extname(file.originalname);
        const name = path.basename(file.originalname, ext).replace(/[^a-zA-Z0-9-_]/g, '_');
        cb(null, `${Date.now()}_${name}${ext}`);
      },
    }),
    limits: { fileSize: parseInt(process.env.MAX_FILE_SIZE || String(20 * 1024 * 1024)) },
  });

  // Make upload helper available to route handlers
  app.set('uploadSingle', uploadMiddleware.single('file'));

  // ── Socket.IO ────────────────────────────────────────────────────────────────
  const io = new Server(server, { cors: { origin: '*', methods: ['GET', 'POST'] } });

  // JWT auth middleware for Socket.IO
  io.use(async (socket, next) => {
    const token = socket.handshake.auth?.token || socket.handshake.query?.token;
    if (!token) return next(new Error('Unauthorized'));
    const decoded = verifyToken(token);
    if (!decoded) return next(new Error('Unauthorized'));
    socket.userId = decoded.userId;
    try {
      socket.coupleKey = await getCoupleKey(decoded.userId);
    } catch {
      socket.coupleKey = `solo_${decoded.userId}`;
    }
    next();
  });

  initSyncHandler(io);

  // Share io instance with Express routes (for REST→Socket fanout)
  app.set('io', io);

  // ── Global middleware ────────────────────────────────────────────────────────
  app.use(helmetMiddleware);
  app.use(cors({ origin: process.env.CORS_ORIGIN || '*' }));
  app.use(express.json({ limit: '5mb' }));
  app.use(express.urlencoded({ extended: true, limit: '5mb' }));
  app.use(morganMiddleware);
  app.use(promMiddleware);
  app.use('/uploads', express.static(storagePath));

  // ── Metrics scrape endpoint (before rate-limiting) ───────────────────────────
  app.use('/metrics', metricsRoute);

  // ── Health check ─────────────────────────────────────────────────────────────
  app.get('/api/health', (req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
  });

  // ── API routes ────────────────────────────────────────────────────────────────
  app.use('/api', apiLimiter);
  app.use('/api/auth',         authLimiter, authRoutes);
  app.use('/api/notes',        notesRoutes);
  app.use('/api/wishes',       wishesRoutes);
  app.use('/api/moods',        moodsRoutes);
  app.use('/api/activities',   activitiesRoutes);
  app.use('/api/cycles',       cyclesRoutes);
  app.use('/api/calendars',    calendarsRoutes);
  app.use('/api/relationship', relationshipRoutes);
  app.use('/api/partner',      relationshipRoutes); // alias for partner pairing routes
  app.use('/api/art',          artRoutes);
  app.use('/api/upload',       uploadRoutes);
  app.use('/api/admin',        adminRoutes);
  // Legacy alias so existing clients keep working
  app.post('/api/fcm-token', require('./src/utils/auth').authenticateToken, async (req, res) => {
    try {
      const { fcm_token } = req.body;
      if (!fcm_token) return res.status(400).json({ success: false, message: 'fcm_token required' });
      await pool.query(
        `INSERT INTO fcm_tokens (user_id, fcm_token, updated_at) VALUES ($1,$2,NOW())
         ON CONFLICT (user_id) DO UPDATE SET fcm_token=$2, updated_at=NOW()`,
        [req.userId, fcm_token]
      );
      res.json({ success: true, message: 'FCM token registered' });
    } catch { res.status(500).json({ success: false, message: 'Internal server error' }); }
  });

  // ── Admin panel SPA ───────────────────────────────────────────────────────────
  const distPath = path.join(__dirname, 'dist');
  app.use(express.static(distPath));
  app.get(/^(?!\/api|\/metrics).*/, (req, res) => {
    const indexFile = path.join(distPath, 'index.html');
    res.sendFile(indexFile, (err) => {
      if (err) res.status(404).json({ success: false, message: 'Not found' });
    });
  });

  // ── Error handler (must be last) ─────────────────────────────────────────────
  app.use(errorHandler);

  // ── Start server ──────────────────────────────────────────────────────────────
  const PORT = process.env.API_PORT || 3005;
  server.listen(PORT, () => {
    logger.info(`LoveApp API running on port ${PORT}`);
    logger.info(`Database: ${process.env.PGHOST}:${process.env.PGPORT}/${process.env.PGDATABASE}`);
    logger.info(`Environment: ${process.env.NODE_ENV || 'development'}`);
  });

  // Graceful shutdown
  process.on('SIGTERM', () => {
    logger.info('SIGTERM received – shutting down gracefully');
    server.close(() => {
      pool.end();
      process.exit(0);
    });
  });
}

bootstrap().catch((err) => {
  console.error('Failed to start server:', err);
  process.exit(1);
});
