// tests/notes.test.js
const request = require('supertest');
const jwt     = require('jsonwebtoken');

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
jest.mock('../src/instrumentation', () => ({ init: jest.fn(), captureException: jest.fn() }), { virtual: true });

const pool = require('../src/config/db');

// ─── Helpers ──────────────────────────────────────────────────────────────
const JWT_SECRET = process.env.JWT_SECRET || 'test_secret_key_change_me';

function makeToken(userId = 1, partnerId = 2) {
  return jwt.sign({ userId, partnerId }, JWT_SECRET, { expiresIn: '1h' });
}

// ─── App setup ────────────────────────────────────────────────────────────
let app;
beforeAll(() => {
  const express = require('express');
  app = express();
  app.use(express.json());
  app.use('/api/notes', require('../src/routes/notes'));
});

beforeEach(() => {
  pool.query.mockReset();
});

// ─── GET /api/notes ───────────────────────────────────────────────────────
describe('GET /api/notes', () => {
  it('returns 401 without Authorization header', async () => {
    const res = await request(app).get('/api/notes');
    expect(res.status).toBe(401);
  });

  it('returns paginated notes for authenticated user', async () => {
    pool.query
      .mockResolvedValueOnce({ rows: [{ total: '2' }] })          // count
      .mockResolvedValueOnce({                                      // data
        rows: [
          { id: 1, title: 'Note A', content: 'Hello', user_id: 1, created_at: new Date(), updated_at: new Date() },
          { id: 2, title: 'Note B', content: 'World', user_id: 1, created_at: new Date(), updated_at: new Date() },
        ],
      });
    const res = await request(app)
      .get('/api/notes')
      .set('Authorization', `Bearer ${makeToken()}`);
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('data');
    expect(Array.isArray(res.body.data)).toBe(true);
  });
});

// ─── POST /api/notes ──────────────────────────────────────────────────────
describe('POST /api/notes', () => {
  it('returns 400 if title is missing', async () => {
    const res = await request(app)
      .post('/api/notes')
      .set('Authorization', `Bearer ${makeToken()}`)
      .send({ content: 'no title' });
    expect(res.status).toBe(400);
  });

  it('creates and returns a new note', async () => {
    const created = { id: 5, title: 'My Note', content: 'Some content', user_id: 1, created_at: new Date(), updated_at: new Date() };
    pool.query.mockResolvedValueOnce({ rows: [created] });
    const res = await request(app)
      .post('/api/notes')
      .set('Authorization', `Bearer ${makeToken()}`)
      .send({ title: 'My Note', content: 'Some content' });
    expect(res.status).toBe(201);
    expect(res.body.data).toMatchObject({ title: 'My Note' });
  });
});

// ─── GET /api/notes/:id ───────────────────────────────────────────────────
describe('GET /api/notes/:id', () => {
  it('returns 404 for unknown note', async () => {
    pool.query.mockResolvedValueOnce({ rows: [] });
    const res = await request(app)
      .get('/api/notes/9999')
      .set('Authorization', `Bearer ${makeToken()}`);
    expect(res.status).toBe(404);
  });

  it('returns the note when found', async () => {
    const note = { id: 1, title: 'Test Note', content: 'body', user_id: 1 };
    pool.query.mockResolvedValueOnce({ rows: [note] });
    const res = await request(app)
      .get('/api/notes/1')
      .set('Authorization', `Bearer ${makeToken()}`);
    expect(res.status).toBe(200);
    expect(res.body.data).toMatchObject({ id: 1 });
  });
});

// ─── PATCH /api/notes/:id ─────────────────────────────────────────────────
describe('PATCH /api/notes/:id', () => {
  it('updates and returns the note', async () => {
    const updated = { id: 1, title: 'Updated', content: 'body', user_id: 1 };
    pool.query.mockResolvedValueOnce({ rows: [updated] });
    const res = await request(app)
      .patch('/api/notes/1')
      .set('Authorization', `Bearer ${makeToken()}`)
      .send({ title: 'Updated' });
    expect(res.status).toBe(200);
    expect(res.body.data.title).toBe('Updated');
  });
});

// ─── DELETE /api/notes/:id ────────────────────────────────────────────────
describe('DELETE /api/notes/:id', () => {
  it('returns 204 on successful delete', async () => {
    pool.query.mockResolvedValueOnce({ rows: [{ id: 1 }] });
    const res = await request(app)
      .delete('/api/notes/1')
      .set('Authorization', `Bearer ${makeToken()}`);
    expect(res.status).toBe(204);
  });

  it('returns 404 when note does not exist', async () => {
    pool.query.mockResolvedValueOnce({ rows: [] });
    const res = await request(app)
      .delete('/api/notes/9999')
      .set('Authorization', `Bearer ${makeToken()}`);
    expect(res.status).toBe(404);
  });
});
