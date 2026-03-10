import * as Joi from 'joi';

export const envValidationSchema = Joi.object({
  NODE_ENV: Joi.string()
    .valid('development', 'production', 'test')
    .default('development'),
  PORT: Joi.number().default(3005),

  // PostgreSQL
  PGHOST: Joi.string().required(),
  PGPORT: Joi.number().default(5432),
  PGUSER: Joi.string().required(),
  PGPASSWORD: Joi.string().required(),
  PGDATABASE: Joi.string().required(),

  // Redis
  REDIS_URL: Joi.string().required(),

  // JWT
  JWT_SECRET: Joi.string().required(),
  JWT_ACCESS_EXPIRES: Joi.string().default('15m'),
  JWT_REFRESH_EXPIRES: Joi.string().default('30d'),

  // CORS
  CORS_ORIGIN: Joi.string().default('*'),

  // Storage
  STORAGE_PATH: Joi.string().default('./uploads'),
  MAX_FILE_SIZE: Joi.number().default(10485760),
  SERVER_URL: Joi.string().default(''),

  // Firebase
  FIREBASE_SERVICE_ACCOUNT: Joi.string().optional(),

  // Google OAuth
  GOOGLE_WEB_CLIENT_ID: Joi.string().optional(),
  GOOGLE_ANDROID_CLIENT_ID: Joi.string().optional(),

  // Tencent IM
  TIM_SDK_APP_ID: Joi.number().optional(),
  TIM_SECRET_KEY: Joi.string().optional(),

  // Tenor GIF
  TENOR_API_KEY: Joi.string().optional(),

  // Admin
  ADMIN_JWT_SECRET: Joi.string().optional(),

  // Sentry
  SENTRY_DSN: Joi.string().optional(),

  // Logging
  LOG_LEVEL: Joi.string().default('info'),
  LOGS_DIR: Joi.string().default('./logs'),
});
