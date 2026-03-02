// src/queues/imageQueue.js
// Bull queue for async image compression using Sharp.
//
// Producer (e.g. upload route):
//   const { imageQueue } = require('../queues/imageQueue');
//   await imageQueue.add({ inputPath, outputPath, width, quality });
//
const Bull  = require('bull');
const sharp = require('sharp');
const fs    = require('fs');
const path  = require('path');
const logger = require('../config/logger');

const REDIS_URL = process.env.REDIS_URL || 'redis://localhost:6379';

const imageQueue = new Bull('imageProcessing', REDIS_URL, {
  defaultJobOptions: {
    attempts: 3,
    backoff: { type: 'exponential', delay: 2000 },
    removeOnComplete: 100,
    removeOnFail:    200,
  },
});

// ─── Processor ────────────────────────────────────────────────────────────
imageQueue.process(async (job) => {
  const { inputPath, outputPath, width = 1920, quality = 80, format = 'webp' } = job.data;

  if (!fs.existsSync(inputPath)) {
    throw new Error(`Input file not found: ${inputPath}`);
  }

  await fs.promises.mkdir(path.dirname(outputPath), { recursive: true });

  const meta = await sharp(inputPath).metadata();
  const resizeWidth = meta.width && meta.width > width ? width : undefined;

  await sharp(inputPath)
    .resize(resizeWidth)
    [format === 'webp' ? 'webp' : 'jpeg']({ quality })
    .toFile(outputPath);

  logger.info(`[imageQueue] Processed ${inputPath} → ${outputPath}`);
  return { outputPath };
});

// ─── Event logging ─────────────────────────────────────────────────────────
imageQueue.on('failed', (job, err) => {
  logger.error(`[imageQueue] job ${job.id} failed:`, err.message);
});

imageQueue.on('completed', (job) => {
  logger.debug(`[imageQueue] job ${job.id} completed`);
});

module.exports = { imageQueue };
