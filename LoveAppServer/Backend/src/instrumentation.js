// src/instrumentation.js
// Must be required as the VERY FIRST import in index.js:
//   require('./src/instrumentation');
//
const Sentry = require('@sentry/node');
const { nodeProfilingIntegration } = require('@sentry/profiling-node');

Sentry.init({
  dsn: process.env.SENTRY_DSN, // set in .env
  integrations: [
    nodeProfilingIntegration(),
    Sentry.httpIntegration(),
    Sentry.expressIntegration(),
    Sentry.postgresIntegration(),
    Sentry.redisIntegration(),
  ],
  tracesSampleRate: process.env.NODE_ENV === 'production' ? 0.2 : 1.0,
  profilesSampleRate: 0.1,
  environment: process.env.NODE_ENV || 'development',
  release: `loveapp-api@${process.env.npm_package_version}`,
  beforeSend(event) {
    // Strip PII from request bodies
    if (event.request && event.request.data) {
      const sensitive = ['password', 'token', 'refreshToken', 'secret'];
      sensitive.forEach(k => {
        if (event.request.data[k]) event.request.data[k] = '[FILTERED]';
      });
    }
    return event;
  },
});

module.exports = Sentry;
