import * as winston from 'winston';
import 'winston-daily-rotate-file';
import { WinstonModule, utilities } from 'nest-winston';

export function createWinstonConfig(logsDir = './logs', logLevel = 'info') {
  const transports: winston.transport[] = [
    new winston.transports.Console({
      format: winston.format.combine(
        winston.format.timestamp(),
        utilities.format.nestLike('LoveApp', {
          prettyPrint: true,
          colors: true,
        }),
      ),
    }),
    new winston.transports.DailyRotateFile({
      dirname: logsDir,
      filename: 'error-%DATE%.log',
      datePattern: 'YYYY-MM-DD',
      level: 'error',
      maxFiles: '14d',
      format: winston.format.combine(
        winston.format.timestamp(),
        winston.format.json(),
      ),
    }),
    new winston.transports.DailyRotateFile({
      dirname: logsDir,
      filename: 'combined-%DATE%.log',
      datePattern: 'YYYY-MM-DD',
      maxFiles: '14d',
      format: winston.format.combine(
        winston.format.timestamp(),
        winston.format.json(),
      ),
    }),
  ];

  return {
    level: logLevel,
    transports,
  };
}

export const winstonModuleFactory = {
  useFactory: () => {
    const logsDir = process.env.LOGS_DIR || './logs';
    const logLevel = process.env.LOG_LEVEL || 'info';
    return WinstonModule.createLogger(createWinstonConfig(logsDir, logLevel));
  },
};
