// src/workers/imageWorker.js
// Standalone process that drains the Bull imageProcessing queue.
// PM2 runs this as a separate fork-mode process.

require('../instrumentation'); // Sentry must be first

const logger = require('../utils/logger');

// Importing the queue file registers the processor immediately
require('../queues/imageQueue');

logger.info('[imageWorker] Image processing worker started');

process.on('uncaughtException',  err => logger.error('[imageWorker] Uncaught exception', { error: err.message }));
process.on('unhandledRejection', err => logger.error('[imageWorker] Unhandled rejection',  { error: String(err) }));
