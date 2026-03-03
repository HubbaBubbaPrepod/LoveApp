/**
 * PM2 ecosystem configuration — LoveApp API
 *
 * Usage:
 *   pm2 start ecosystem.config.js --env production
 *   pm2 reload ecosystem.config.js --env production   # zero-downtime reload
 *   pm2 save && pm2 startup                           # survive reboots
 */

module.exports = {
  apps: [
    {
      name:             'loveapp-api',
      script:           'index.js',

      // ── Clustering ──────────────────────────────────────────────────
      instances:        'max',       // one process per CPU core
      exec_mode:        'cluster',   // enables built-in load balancing

      // ── Restart policy ──────────────────────────────────────────────
      autorestart:      true,
      watch:            false,        // don't watch files in production
      max_memory_restart: '500M',

      // ── Graceful shutdown ───────────────────────────────────────────
      kill_timeout:     5000,         // ms to wait before SIGKILL
      listen_timeout:   10000,        // ms PM2 waits for app to be ready
      shutdown_with_message: true,

      // ── Logging ─────────────────────────────────────────────────────
      log_date_format:  'YYYY-MM-DD HH:mm:ss Z',
      merge_logs:       true,
      out_file:         '/var/log/loveapp/out.log',
      error_file:       '/var/log/loveapp/error.log',
      log_file:         '/var/log/loveapp/combined.log',

      // ── Environment: development ────────────────────────────────────
      env: {
        NODE_ENV: 'development',
        PORT:     3005,
      },

      // ── Environment: production ─────────────────────────────────────
      env_production: {
        NODE_ENV:                    'production',
        PORT:                        3005,
        // All secrets injected via OS environment or .env — do NOT put
        // real credentials here; this file is committed to version control.
        // PGHOST, PGPORT, PGDATABASE, PGUSER, PGPASSWORD
        // JWT_SECRET, REFRESH_TOKEN_SECRET
        // REDIS_URL
        // FIREBASE_SERVICE_ACCOUNT_PATH
        // SENTRY_DSN
      },
    },

    // ── Bull Worker — image processing ──────────────────────────────────
    {
      name:         'loveapp-image-worker',
      script:       'src/workers/imageWorker.js',
      instances:    1,
      exec_mode:    'fork',
      autorestart:  true,
      watch:        false,
      max_memory_restart: '300M',
      log_date_format:    'YYYY-MM-DD HH:mm:ss Z',
      merge_logs:         true,
      out_file:     '/var/log/loveapp/imageWorker.out.log',
      error_file:   '/var/log/loveapp/imageWorker.error.log',
      env_production: {
        NODE_ENV: 'production',
      },
    },

    // ── Bull Worker — push notifications ────────────────────────────────
    {
      name:         'loveapp-notif-worker',
      script:       'src/workers/notifWorker.js',
      instances:    1,
      exec_mode:    'fork',
      autorestart:  true,
      watch:        false,
      max_memory_restart: '200M',
      log_date_format:    'YYYY-MM-DD HH:mm:ss Z',
      merge_logs:         true,
      out_file:     '/var/log/loveapp/notifWorker.out.log',
      error_file:   '/var/log/loveapp/notifWorker.error.log',
      env_production: {
        NODE_ENV: 'production',
      },
    },
  ],
};
