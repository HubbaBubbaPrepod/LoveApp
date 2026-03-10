import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { LoveTouchSession } from './entities/love-touch-session.entity';
import { LoveTouchController } from './love-touch.controller';
import { LoveTouchService } from './love-touch.service';

@Module({
  imports: [TypeOrmModule.forFeature([LoveTouchSession])],
  controllers: [LoveTouchController],
  providers: [LoveTouchService],
  exports: [LoveTouchService],
})
export class LoveTouchModule {}
