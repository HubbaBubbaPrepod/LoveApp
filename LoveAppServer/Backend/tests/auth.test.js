// tests/auth.test.js
const request = require('supertest');

// ─── Mocks ────────────────────────────────────────────────────────────────
jest.mock('../src/config/db', () => ({
  query: jest.fn(),
}));
jest.mock('../src/config/redis', () => ({
  initRedis: jest.fn(),
  getRedis:  jest.fn().mockReturnValue({
    get: jest.fn().mockResolvedValue(null),
    set: jest.fn().mockResolvedValue('OK'),
    del: jest.fn().mockResolvedValue(1),
  }),
}));
jest.mock('../src/config/firebase', () => ({
  initFirebase: jest.fn(),
}));
// Silence Sentry
jest.mock('../src/instrumentation', () => ({ init: jest.fn(), captureException: jest.fn() }), { virtual: true });

const pool = require('../src/config/db');

// We require index.js AFTER mocks are set up to avoid side-effects
let app;
beforeAll(() => {
  // Create minimal Express app with only auth routes
  const express = require('express');
  app = express();
  app.use(express.json());
  app.use('/api/auth', require('../src/routes/auth'));
});

describe('POST /api/auth/register', () => {
  it('returns 400 if email is missing', async () => {
    const res = await request(app)
      .post('/api/auth/register')
      .send({ name: 'Test', password: 'password123' });
    expect(res.status).toBe(400);
  });

  it('returns 400 if password is too short', async () => {
    const res = await request(app)
      .post('/api/auth/register')
      .send({ name: 'Test', email: 'test@test.com', password: 'abc' });
    expect(res.status).toBe(400);
  });

  it('creates a user and returns tokens', async () => {
    pool.query
      .mockResolvedValueOnce({ rows: [] })       // check email doesn't exist
      .mockResolvedValueOnce({ rows: [{ id: 1, name: 'Test', email: 'test@test.com', partner_id: null, partner_code: 'ABC12345' }] }); // insert
    const res = await request(app)
      .post('/api/auth/register')
      .send({ name: 'Test', email: 'test@test.com', password: 'password123' });
    expect(res.status).toBe(201);
    expect(res.body).toHaveProperty('data.accessToken');
  });

  it('returns 409 if email already exists', async () => {
    pool.query.mockResolvedValueOnce({ rows: [{ id: 1 }] }); // email exists
    const res = await request(app)
      .post('/api/auth/register')
      .send({ name: 'Test', email: 'existing@test.com', password: 'password123' });
    expect(res.status).toBe(409);
  });
});

describe('POST /api/auth/login', () => {
  it('returns 400 if body is empty', async () => {
    const res = await request(app).post('/api/auth/login').send({});
    expect(res.status).toBe(400);
  });

  it('returns 401 for wrong password', async () => {
    const bcrypt = require('bcryptjs');
    const hashed = await bcrypt.hash('correctPassword', 10);
    pool.query.mockResolvedValueOnce({
      rows: [{ id: 1, name: 'Test', email: 'test@test.com', password_hash: hashed, partner_id: null }],
    });
    const res = await request(app)
      .post('/api/auth/login')
      .send({ email: 'test@test.com', password: 'wrongPassword' });
    expect(res.status).toBe(401);
  });

  it('returns tokens on successful login', async () => {
    const bcrypt = require('bcryptjs');
    const hashed = await bcrypt.hash('correctPassword', 10);
    pool.query.mockResolvedValueOnce({
      rows: [{ id: 1, name: 'Test', email: 'test@test.com', password_hash: hashed, partner_id: 2 }],
    });
    const res = await request(app)
      .post('/api/auth/login')
      .send({ email: 'test@test.com', password: 'correctPassword' });
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('data.accessToken');
    expect(res.body).toHaveProperty('data.refreshToken');
  });
});
