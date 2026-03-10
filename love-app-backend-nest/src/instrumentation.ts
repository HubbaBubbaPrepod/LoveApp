import * as Sentry from '@sentry/node';
import { nodeProfilingIntegration } from '@sentry/profiling-node';

if (process.env.SENTRY_DSN) {
  Sentry.init({
    dsn: process.env.SENTRY_DSN,
    integrations: [nodeProfilingIntegration()],
    tracesSampleRate: process.env.NODE_ENV === 'production' ? 0.2 : 1.0,
    profilesSampleRate: 0.1,
    environment: process.env.NODE_ENV || 'development',
    beforeSend(event) {
      if (event.request?.data) {
        const data = event.request.data as Record<string, unknown>;
        for (const key of [
          'password',
          'token',
          'refreshToken',
          'secret',
          'pin',
        ]) {
          if (key in data) data[key] = '[FILTERED]';
        }
      }
      return event;
    },
  });
}
