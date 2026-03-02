// tests/setup.js
// Global Jest setup — runs once before all test suites.
process.env.NODE_ENV = 'test';
process.env.JWT_SECRET = 'test-secret-key';
process.env.JWT_REFRESH_SECRET = 'test-refresh-secret-key';
process.env.DATABASE_URL = process.env.TEST_DATABASE_URL || 'postgresql://postgres:postgres@localhost:5432/loveapp_test';
process.env.REDIS_URL = process.env.TEST_REDIS_URL || 'redis://localhost:6379';
process.env.SENTRY_DSN = ''; // disable Sentry in tests

// Silence logger during tests
jest.mock('../src/config/logger', () => ({
  info:  jest.fn(),
  warn:  jest.fn(),
  error: jest.fn(),
  debug: jest.fn(),
}));

afterAll(async () => {
  // Ensure all async handles are closed
  await new Promise(resolve => setTimeout(resolve, 200));
});
