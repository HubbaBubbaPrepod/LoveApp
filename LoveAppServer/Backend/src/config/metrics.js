// src/config/metrics.js – Prometheus metrics setup
let client;
let httpRequestDuration;
let activeSocketConnections;
let dbQueryDuration;

try {
  client = require('prom-client');

  const register = new client.Registry();
  client.collectDefaultMetrics({ register, prefix: 'loveapp_' });

  httpRequestDuration = new client.Histogram({
    name: 'loveapp_http_request_duration_seconds',
    help: 'Duration of HTTP requests in seconds',
    labelNames: ['method', 'route', 'status_code'],
    buckets: [0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5],
    registers: [register],
  });

  activeSocketConnections = new client.Gauge({
    name: 'loveapp_active_socket_connections',
    help: 'Number of active Socket.IO connections',
    registers: [register],
  });

  dbQueryDuration = new client.Histogram({
    name: 'loveapp_db_query_duration_seconds',
    help: 'Duration of PostgreSQL queries in seconds',
    labelNames: ['operation'],
    buckets: [0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1],
    registers: [register],
  });

  module.exports = { register, httpRequestDuration, activeSocketConnections, dbQueryDuration };
} catch {
  // prom-client not installed – metrics are no-ops
  const noop = { labels: () => ({ observe: () => {}, inc: () => {}, dec: () => {} }), observe: () => {}, inc: () => {}, dec: () => {} };
  module.exports = {
    register: { metrics: async () => '', contentType: '' },
    httpRequestDuration: noop,
    activeSocketConnections: noop,
    dbQueryDuration: noop,
  };
}
