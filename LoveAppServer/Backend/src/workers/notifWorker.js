// src/workers/notifWorker.js
// Standalone process that drains the Bull notifications queue.
// PM2 runs this as a separate fork-mode process.

require('../instrumentation'); // Sentry must be first

const logger = require('../config/logger');

// Importing the queue file registers the processor immediately
require('../queues/notificationQueue');

logger.info('[notifWorker] Notification worker started');

process.on('uncaughtException',  err => logger.error('[notifWorker] Uncaught exception', { error: err.message }));
process.on('unhandledRejection', err => logger.error('[notifWorker] Unhandled rejection',  { error: String(err) }));
