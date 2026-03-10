import { Test, TestingModule } from '@nestjs/testing';
import {
  FastifyAdapter,
  NestFastifyApplication,
} from '@nestjs/platform-fastify';
import { HealthModule } from './../src/health/health.module';

/**
 * Lightweight e2e test that only boots the HealthModule.
 * Full-stack e2e tests (with DB/Redis) should extend this pattern
 * by importing the relevant modules and providing test-container overrides.
 */
describe('Health (e2e)', () => {
  let app: NestFastifyApplication;

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [HealthModule],
    }).compile();

    app = moduleFixture.createNestApplication(new FastifyAdapter());
    await app.init();
    await app.getHttpAdapter().getInstance().ready();
  });

  afterAll(async () => {
    await app.close();
  });

  it('/health (GET) should return 200', () => {
    return app.inject({ method: 'GET', url: '/health' }).then((result) => {
      expect(result.statusCode).toBe(200);
    });
  });
});
