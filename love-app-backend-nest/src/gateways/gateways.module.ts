import { Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { SyncGateway } from './sync.gateway';
import { MetricsModule } from '../metrics/metrics.module';

@Module({
  imports: [
    JwtModule.registerAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        secret: config.get<string>('JWT_SECRET'),
      }),
    }),
    MetricsModule,
  ],
  providers: [SyncGateway],
  exports: [SyncGateway],
})
export class GatewaysModule {}
