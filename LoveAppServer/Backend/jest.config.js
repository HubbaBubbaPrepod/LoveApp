// jest.config.js
module.exports = {
  testEnvironment: 'node',
  testMatch: ['**/tests/**/*.test.js'],
  collectCoverageFrom: ['src/**/*.js', '!src/db/migrate.js'],
  coverageDirectory: 'coverage',
  coverageThreshold: {
    global: { branches: 60, functions: 70, lines: 70, statements: 70 },
  },
  setupFilesAfterFramework: ['./tests/setup.js'],
  testTimeout: 15000,
};
