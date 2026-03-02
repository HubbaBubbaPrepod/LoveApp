// src/config/redis.js – Redis client with pub/sub support
const { createClient } = require('redis');
const logger = require('./logger');

let publisher = null;
let subscriber = null;

async function createRedisClient(name) {
  const client = createClient({
    url: process.env.REDIS_URL || 'redis://localhost:6379',
    socket: {
      reconnectStrategy: (retries) => Math.min(retries * 100, 5000),
    },
  });

  client.on('error',        (err)  => logger.error(`Redis [${name}] error: ${err.message}`));
  client.on('connect',      ()     => logger.info(`Redis [${name}] connected`));
  client.on('reconnecting', ()     => logger.warn(`Redis [${name}] reconnecting...`));

  try {
    await client.connect();
  } catch (err) {
    logger.warn(`Redis [${name}] unavailable – running without Redis: ${err.message}`);
    return null;
  }
  return client;
}

async function initRedis() {
  publisher  = await createRedisClient('publisher');
  subscriber = await createRedisClient('subscriber');
}

function getPublisher()  { return publisher; }
function getSubscriber() { return subscriber; }

/**
 * Publish a message to a Redis channel.
 * Used for Socket.IO cross-instance fan-out (horizontal scaling).
 */
async function publish(channel, message) {
  if (!publisher) return;
  try {
    await publisher.publish(channel, JSON.stringify(message));
  } catch (err) {
    logger.error(`Redis publish error: ${err.message}`);
  }
}

/**
 * Subscribe to a Redis channel and call handler for every message.
 */
async function subscribe(channel, handler) {
  if (!subscriber) return;
  try {
    await subscriber.subscribe(channel, (raw) => {
      try { handler(JSON.parse(raw)); } catch {}
    });
  } catch (err) {
    logger.error(`Redis subscribe error: ${err.message}`);
  }
}

/** Simple cache helpers **/
async function cacheGet(key) {
  if (!publisher) return null;
  try {
    const raw = await publisher.get(key);
    return raw ? JSON.parse(raw) : null;
  } catch { return null; }
}

async function cacheSet(key, value, ttlSeconds = 300) {
  if (!publisher) return;
  try {
    await publisher.set(key, JSON.stringify(value), { EX: ttlSeconds });
  } catch {}
}

async function cacheDel(key) {
  if (!publisher) return;
  try { await publisher.del(key); } catch {}
}

module.exports = { initRedis, getPublisher, getSubscriber, publish, subscribe, cacheGet, cacheSet, cacheDel };
