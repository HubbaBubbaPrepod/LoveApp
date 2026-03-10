import { registerAs } from '@nestjs/config';

export const databaseConfig = registerAs('database', () => ({
  type: 'postgres' as const,
  host: process.env.PGHOST,
  port: parseInt(process.env.PGPORT || '5432', 10),
  username: process.env.PGUSER,
  password: process.env.PGPASSWORD,
  database: process.env.PGDATABASE,
  synchronize: process.env.TYPEORM_SYNC === 'true',
  logging: process.env.NODE_ENV !== 'production',
  entities: [__dirname + '/../**/*.entity{.ts,.js}'],
  extra: {
    max: 20,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 2000,
  },
}));
