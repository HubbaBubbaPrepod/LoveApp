module.exports = {
  apps: [
    {
      name:                'loveapp-api',
      script:              'dist/main.js',
      instances:           1,
      exec_mode:           'fork',
      autorestart:         true,
      watch:               false,
      max_memory_restart:  '500M',
      kill_timeout:        5000,
      listen_timeout:      10000,
      shutdown_with_message: true,
      log_date_format:     'YYYY-MM-DD HH:mm:ss Z',
      merge_logs:          true,
      out_file:            'logs/out.log',
      error_file:          'logs/error.log',
      log_file:            'logs/combined.log',
      env:                 { NODE_ENV: 'development', PORT: 3005 },
      env_production:      { NODE_ENV: 'production',  PORT: 3005 },
    },
  ],
};
