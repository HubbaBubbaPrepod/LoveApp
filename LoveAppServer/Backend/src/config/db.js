// src/config/db.js – PostgreSQL connection pool
const pg = require('pg');

// Return PostgreSQL `date` columns as plain 'YYYY-MM-DD' strings
pg.types.setTypeParser(1082, (val) => val);

const pool = new pg.Pool({
  host:     process.env.PGHOST     || 'localhost',
  user:     process.env.PGUSER     || 'postgres',
  password: process.env.PGPASSWORD || '',
  database: process.env.PGDATABASE || 'loveapp_db',
  port:     parseInt(process.env.PGPORT || '5432', 10),
  max:      20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
});

pool.on('error', (err) => {
  console.error('Unexpected error on idle PostgreSQL client', err);
});

module.exports = pool;
