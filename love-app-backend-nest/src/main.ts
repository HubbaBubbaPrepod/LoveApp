import './instrumentation';
import { NestFactory } from '@nestjs/core';
import {
  FastifyAdapter,
  NestFastifyApplication,
} from '@nestjs/platform-fastify';
import { ValidationPipe } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import { RedisIoAdapter } from './common/adapters/redis-io.adapter';
import { WINSTON_MODULE_NEST_PROVIDER } from 'nest-winston';
import { AppModule } from './app.module';
import multipart from '@fastify/multipart';

async function bootstrap() {
  const app = await NestFactory.create<NestFastifyApplication>(
    AppModule,
    new FastifyAdapter({ trustProxy: true }),
    { bufferLogs: true },
  );

  const config = app.get(ConfigService);
  const logger = app.get(WINSTON_MODULE_NEST_PROVIDER);
  app.useLogger(logger);

  // Register fastify multipart for file uploads
  await app.getHttpAdapter().getInstance().register(multipart, {
    limits: {
      fileSize: config.get<number>('MAX_FILE_SIZE', 10485760),
    },
  });

  app.setGlobalPrefix('api', { exclude: ['health', 'metrics'] });

  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
      transformOptions: { enableImplicitConversion: true },
    }),
  );

  app.enableCors({
    origin: config.get<string>('CORS_ORIGIN', '*'),
    credentials: true,
  });

  const redisUrl = config.get<string>('REDIS_URL');
  if (redisUrl) {
    const ioAdapter = new RedisIoAdapter(app, redisUrl);
    await ioAdapter.connectToRedis();
    app.useWebSocketAdapter(ioAdapter);
  }

  const swaggerConfig = new DocumentBuilder()
    .setTitle('LoveApp API')
    .setDescription('LoveApp Backend API')
    .setVersion('1.0')
    .addBearerAuth()
    .build();
  const document = SwaggerModule.createDocument(app, swaggerConfig);
  SwaggerModule.setup('docs', app, document);

  const port = config.get<number>('PORT', 3005);
  await app.listen(port, '0.0.0.0');
  logger.log(`Application listening on port ${port}`, 'Bootstrap');
}
bootstrap();
