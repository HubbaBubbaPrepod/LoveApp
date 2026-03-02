// src/config/logger.js – Winston logger with daily rotation
const winston = require('winston');
const DailyRotateFile = require('winston-daily-rotate-file');
const path = require('path');

const logsDir = process.env.LOGS_DIR || path.join(__dirname, '../../logs');

const formats = [
  winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
  winston.format.errors({ stack: true }),
  winston.format.splat(),
];

const consoleFormat = winston.format.combine(
  ...formats,
  winston.format.colorize(),
  winston.format.printf(({ timestamp, level, message, stack }) =>
    stack ? `${timestamp} [${level}]: ${message}\n${stack}` : `${timestamp} [${level}]: ${message}`
  )
);

const fileFormat = winston.format.combine(...formats, winston.format.json());

const transports = [
  new winston.transports.Console({ format: consoleFormat }),
  new DailyRotateFile({
    filename: path.join(logsDir, 'error-%DATE%.log'),
    datePattern: 'YYYY-MM-DD',
    level: 'error',
    maxFiles: '14d',
    format: fileFormat,
  }),
  new DailyRotateFile({
    filename: path.join(logsDir, 'combined-%DATE%.log'),
    datePattern: 'YYYY-MM-DD',
    maxFiles: '14d',
    format: fileFormat,
  }),
];

const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  transports,
  exitOnError: false,
});

module.exports = logger;
