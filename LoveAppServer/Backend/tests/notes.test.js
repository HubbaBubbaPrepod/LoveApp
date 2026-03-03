// tests/notes.test.js
const request = require('supertest');
const jwt     = require('jsonwebtoken');

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

// ─── Helpers ──────────────────────────────────────────────────────────────────
const JWT_SECRET = process.env.JWT_SECRET || 'test-secret-key';

function makeToken(userId = 1) {
  return jwt.sign({ userId }, JWT_SECRET, { expiresIn: '1h' });
}

// Mock getPartnerId to return null (no partner) for all tests unless overridden
function mockNoPartner() {
  pool.query.mockResolvedValueOnce({ rows: [] }); // relationship_info lookup → no partner
}

// ─── App setup ────────────────────────────────────────────────────────────────
let app;
beforeAll(() => {
  const express = require('express');
  app = express();
  app.use(express.json());
  // Attach a dummy io so broadcastChange doesn't throw
  app.set('io', null);
  app.use('/api/notes', require('../src/routes/notes'));
});

beforeEach(() => {
  pool.query.mockReset();
});

// ─── GET /api/notes ───────────────────────────────────────────────────────────
describe('GET /api/notes', () => {
  it('returns 401 without Authorization header', async () => {
    const res = await request(app).get('/api/notes');
    expect(res.status).toBe(401);
  });

  it('returns paginated notes for authenticated user', async () => {
    mockNoPartner(); // getPartnerId
    pool.query
      .mockResolvedValueOnce({ rows: [{ total: '2' }] })  // count
      .mockResolvedValueOnce({
        rows: [
          { id: 1, title: 'Note A', content: 'Hello', user_id: 1, tags: [], created_at: new Date(), updated_at: new Date() },
          { id: 2, title: 'Note B', content: 'World', user_id: 1, tags: [], created_at: new Date(), updated_at: new Date() },
        ],
      });
    const res = await request(app)
      .get('/api/notes')
      .set('Authorization', `Bearer ${makeToken()}`);
    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data).toHaveProperty('items');
    expect(Array.isArray(res.body.data.items)).toBe(true);
    expect(res.body.data.total).toBe(2);
  });
});

// ─── POST /api/notes ──────────────────────────────────────────────────────────
describe('POST /api/notes', () => {
  it('creates and returns a new note', async () => {
    const created = { id: 5, title: 'My Note', content: 'Some content', user_id: 1, tags: [], created_at: new Date(), updated_at: new Date() };
    pool.query
      .mockResolvedValueOnce({ rows: [created] })  // INSERT note
      .mockResolvedValueOnce({ rows: [] });          // getPartnerId
    const res = await request(app)
      .post('/api/notes')
      .set('Authorization', `Bearer ${makeToken()}`)
      .send({ title: 'My Note', content: 'Some content' });
    expect(res.status).toBe(201);
    expect(res.body.success).toBe(true);
    expect(res.body.data).toMatchObject({ title: 'My Note' });
  });
});

// ─── GET /api/notes/:id ───────────────────────────────────────────────────────
describe('GET /api/notes/:id', () => {
  it('returns 404 for unknown note', async () => {
    mockNoPartner(); // getPartnerId
    pool.query.mockResolvedValueOnce({ rows: [] }); // note lookup
    const res = await request(app)
      .get('/api/notes/9999')
      .set('Authorization', `Bearer ${makeToken()}`);
    expect(res.status).toBe(404);
  });

  it('returns the note when found', async () => {
    const note = { id: 1, title: 'Test Note', content: 'body', user_id: 1, tags: [] };
    mockNoPartner(); // getPartnerId
    pool.query.mockResolvedValueOnce({ rows: [note] });
    const res = await request(app)
      .get('/api/notes/1')
      .set('Authorization', `Bearer ${makeToken()}`);
    expect(res.status).toBe(200);
    expect(res.body.data).toMatchObject({ id: 1, title: 'Test Note' });
  });
});

// ─── PUT /api/notes/:id ───────────────────────────────────────────────────────
describe('PUT /api/notes/:id', () => {
  it('returns 404 when note does not belong to user', async () => {
    pool.query.mockResolvedValueOnce({ rows: [] }); // UPDATE returns empty
    const res = await request(app)
      .put('/api/notes/9999')
      .set('Authorization', `Bearer ${makeToken()}`)
      .send({ title: 'Updated', content: 'body', is_private: false });
    expect(res.status).toBe(404);
  });

  it('updates and returns the note', async () => {
    const updated = { id: 1, title: 'Updated', content: 'body', user_id: 1, tags: [] };
    pool.query
      .mockResolvedValueOnce({ rows: [updated] })  // UPDATE
      .mockResolvedValueOnce({ rows: [] });          // getPartnerId
    const res = await request(app)
      .put('/api/notes/1')
      .set('Authorization', `Bearer ${makeToken()}`)
      .send({ title: 'Updated', content: 'body', is_private: false });
    expect(res.status).toBe(200);
    expect(res.body.data.title).toBe('Updated');
  });
});

// ─── DELETE /api/notes/:id ────────────────────────────────────────────────────
describe('DELETE /api/notes/:id', () => {
  it('returns 200 on successful soft-delete', async () => {
    pool.query
      .mockResolvedValueOnce({ rows: [{ id: 1, server_updated_at: new Date() }] }) // UPDATE deleted_at
      .mockResolvedValueOnce({ rows: [] }); // getPartnerId
    const res = await request(app)
      .delete('/api/notes/1')
      .set('Authorization', `Bearer ${makeToken()}`);
    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
  });

  it('returns 404 when note does not exist', async () => {
    pool.query.mockResolvedValueOnce({ rows: [] });
    const res = await request(app)
      .delete('/api/notes/9999')
      .set('Authorization', `Bearer ${makeToken()}`);
    expect(res.status).toBe(404);
  });
});
