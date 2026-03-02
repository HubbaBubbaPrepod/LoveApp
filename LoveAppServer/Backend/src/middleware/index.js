// src/middleware/index.js – All custom Express middleware
const rateLimit = require('express-rate-limit');
const helmet    = require('helmet');
const morgan    = require('morgan');
const { httpRequestDuration } = require('../config/metrics');
const logger = require('../config/logger');

/** Security headers */
const helmetMiddleware = helmet({
  crossOriginResourcePolicy: { policy: 'cross-origin' }, // allow /uploads to be served cross-origin
});

/** Rate limiting for all API routes */
const apiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 300,
  standardHeaders: true,
  legacyHeaders: false,
  message: { success: false, message: 'Too many requests, please try again later.' },
});

/** Tighter limiter for auth routes */
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 20,
  standardHeaders: true,
  legacyHeaders: false,
  message: { success: false, message: 'Too many auth requests, please try again later.' },
});

/** HTTP access logging via Morgan → Winston */
const morganMiddleware = morgan(
  ':method :url :status :response-time ms - :res[content-length]',
  {
    stream: { write: (msg) => logger.http(msg.trim()) },
    skip: (req) => req.url === '/api/health' || req.url === '/metrics',
  }
);

/** Prometheus request duration tracking */
function promMiddleware(req, res, next) {
  const start = Date.now();
  res.on('finish', () => {
    const route = req.route?.path || req.path || 'unknown';
    httpRequestDuration
      .labels(req.method, route, String(res.statusCode))
      .observe((Date.now() - start) / 1000);
  });
  next();
}

/** Global error handler */
// eslint-disable-next-line no-unused-vars
function errorHandler(err, req, res, next) {
  logger.error(`Unhandled error: ${err.message}`, { stack: err.stack, url: req.url, method: req.method });
  res.status(500).json({ success: false, message: 'Internal server error' });
}

module.exports = {
  helmetMiddleware,
  apiLimiter,
  authLimiter,
  morganMiddleware,
  promMiddleware,
  errorHandler,
};
