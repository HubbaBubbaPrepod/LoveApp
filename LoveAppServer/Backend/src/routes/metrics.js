// src/routes/metrics.js – Prometheus metrics scrape endpoint
const express = require('express');
const { register } = require('../config/metrics');

const router = express.Router();

// GET /metrics
router.get('/', async (req, res) => {
  try {
    const metrics = await register.metrics();
    res.set('Content-Type', register.contentType);
    res.end(metrics);
  } catch (err) {
    res.status(500).end(err.message);
  }
});

module.exports = router;
