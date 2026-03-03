// tests/auth.test.js
const request = require('supertest');

// ─── Mocks ────────────────────────────────────────────────────────────────────
jest.mock('../src/config/db', () => ({ query: jest.fn() }));
jest.mock('../src/config/redis', () => ({
  initRedis:  jest.fn().mockResolvedValue(undefined),
  cacheGet:   jest.fn().mockResolvedValue(null),
  cacheSet:   jest.fn().mockResolvedValue(undefined),
  cacheDel:   jest.fn().mockResolvedValue(undefined),
  publish:    jest.fn().mockResolvedValue(undefined),
  subscribe:  jest.fn().mockResolvedValue(undefined),
}));
jest.mock('../src/config/firebase', () => ({ initFirebase: jest.fn(), admin: { apps: [] } }));
jest.mock('../src/instrumentation', () => ({}), { virtual: true });

const pool = require('../src/config/db');

// ─── App setup ───────────────────────────────────────────────────────────────
let app;
beforeAll(() => {
  const express = require('express');
  app = express();
  app.use(express.json());
  app.use('/api/auth', require('../src/routes/auth'));
});

beforeEach(() => {
  pool.query.mockReset();
});

// ─── POST /api/auth/signup ────────────────────────────────────────────────────
describe('POST /api/auth/signup', () => {
  it('returns 400 if required fields are missing', async () => {
    // missing username and display_name
    const res = await request(app)
      .post('/api/auth/signup')
      .send({ email: 'test@test.com', password: 'password123' });
    expect(res.status).toBe(400);
    expect(res.body.success).toBe(false);
  });

  it('returns 400 if email or username already exists', async () => {
    pool.query.mockResolvedValueOnce({ rows: [{ id: 1 }] }); // user exists
    const res = await request(app)
      .post('/api/auth/signup')
      .send({ username: 'test', email: 'existing@test.com', password: 'password123', display_name: 'Test' });
    expect(res.status).toBe(400);
    expect(res.body.success).toBe(false);
  });

  it('creates a user and returns access + refresh tokens', async () => {
    pool.query
      .mockResolvedValueOnce({ rows: [] })  // user does not exist
      .mockResolvedValueOnce({ rows: [{ id: 1, username: 'test', email: 'test@test.com', display_name: 'Test', gender: null, created_at: new Date() }] }) // INSERT user
      .mockResolvedValueOnce({ rows: [] }); // INSERT refresh_token

    const res = await request(app)
      .post('/api/auth/signup')
      .send({ username: 'test', email: 'test@test.com', password: 'password123', display_name: 'Test' });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data).toHaveProperty('token');
    expect(res.body.data).toHaveProperty('refresh_token');
  });
});

// ─── POST /api/auth/login ─────────────────────────────────────────────────────
describe('POST /api/auth/login', () => {
  it('returns 400 if body is empty', async () => {
    const res = await request(app).post('/api/auth/login').send({});
    expect(res.status).toBe(400);
  });

  it('returns 401 for wrong password', async () => {
    const bcrypt = require('bcryptjs');
    const hashed = await bcrypt.hash('correctPassword', 10);
    pool.query.mockResolvedValueOnce({
      rows: [{ id: 1, username: 'test', email: 'test@test.com', display_name: 'Test', gender: null, password_hash: hashed, created_at: new Date() }],
    });
    const res = await request(app)
      .post('/api/auth/login')
      .send({ email: 'test@test.com', password: 'wrongPassword' });
    expect(res.status).toBe(401);
  });

  it('returns tokens on successful login', async () => {
    const bcrypt = require('bcryptjs');
    const hashed = await bcrypt.hash('correctPassword', 10);
    pool.query
      .mockResolvedValueOnce({ rows: [{ id: 1, username: 'test', email: 'test@test.com', display_name: 'Test', gender: null, password_hash: hashed, created_at: new Date() }] })
      .mockResolvedValueOnce({ rows: [] }); // INSERT refresh_token

    const res = await request(app)
      .post('/api/auth/login')
      .send({ email: 'test@test.com', password: 'correctPassword' });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data).toHaveProperty('token');
    expect(res.body.data).toHaveProperty('refresh_token');
  });

  it('returns 401 for unknown email', async () => {
    pool.query.mockResolvedValueOnce({ rows: [] });
    const res = await request(app)
      .post('/api/auth/login')
      .send({ email: 'nobody@test.com', password: 'password123' });
    expect(res.status).toBe(401);
  });
});

// ─── POST /api/auth/refresh ───────────────────────────────────────────────────
describe('POST /api/auth/refresh', () => {
  it('returns 400 if refresh_token is missing', async () => {
    const res = await request(app).post('/api/auth/refresh').send({});
    expect(res.status).toBe(400);
  });

  it('returns 401 for invalid or expired refresh token', async () => {
    pool.query.mockResolvedValueOnce({ rows: [] });
    const res = await request(app)
      .post('/api/auth/refresh')
      .send({ refresh_token: 'invalid_token_value' });
    expect(res.status).toBe(401);
  });

  it('returns new tokens for a valid refresh token', async () => {
    pool.query
      .mockResolvedValueOnce({ rows: [{ user_id: 1, username: 'test', email: 'test@test.com', display_name: 'Test', gender: null }] }) // token lookup
      .mockResolvedValueOnce({ rows: [] }) // INSERT new refresh_token
      .mockResolvedValueOnce({ rows: [] }); // DELETE old refresh_token

    const res = await request(app)
      .post('/api/auth/refresh')
      .send({ refresh_token: 'valid_token' });

    expect(res.status).toBe(200);
    expect(res.body.data).toHaveProperty('token');
    expect(res.body.data).toHaveProperty('refresh_token');
  });
});
