// src/queues/notificationQueue.js
// Bull queue for async push-notification delivery via Firebase Cloud Messaging.
//
const Bull    = require('bull');
const admin   = require('firebase-admin');
const logger  = require('../config/logger');

const REDIS_URL = process.env.REDIS_URL || 'redis://localhost:6379';

const notificationQueue = new Bull('notifications', REDIS_URL, {
  defaultJobOptions: {
    attempts: 3,
    backoff: { type: 'exponential', delay: 3000 },
    removeOnComplete: 50,
    removeOnFail:    100,
  },
});

// ─── Job types ─────────────────────────────────────────────────────────────
// { type: 'push', fcmToken, title, body, data? }
// { type: 'multicast', tokens: string[], title, body, data? }

notificationQueue.process(async (job) => {
  const { type, fcmToken, tokens, title, body, data = {} } = job.data;

  const messaging = admin.messaging();

  if (type === 'push' && fcmToken) {
    await messaging.send({
      token: fcmToken,
      notification: { title, body },
      data: Object.fromEntries(Object.entries(data).map(([k, v]) => [k, String(v)])),
      android: { priority: 'high' },
      apns: { payload: { aps: { sound: 'default' } } },
    });
    logger.info(`[notificationQueue] Sent push to token …${fcmToken.slice(-8)}`);
  } else if (type === 'multicast' && Array.isArray(tokens) && tokens.length) {
    const res = await messaging.sendEachForMulticast({
      tokens,
      notification: { title, body },
      data: Object.fromEntries(Object.entries(data).map(([k, v]) => [k, String(v)])),
    });
    logger.info(`[notificationQueue] Multicast: ${res.successCount}/${tokens.length} delivered`);
  } else {
    throw new Error(`Unknown notification job type: ${type}`);
  }
});

notificationQueue.on('failed', (job, err) => {
  logger.error(`[notificationQueue] job ${job.id} failed:`, err.message);
});

module.exports = { notificationQueue };
