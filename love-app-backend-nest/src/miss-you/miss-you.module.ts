import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { MissYouEvent } from './entities/miss-you-event.entity';
import { SparkLog } from '../spark/entities/spark-log.entity';
import { MissYouController } from './miss-you.controller';
import { MissYouService } from './miss-you.service';

@Module({
  imports: [TypeOrmModule.forFeature([MissYouEvent, SparkLog])],
  controllers: [MissYouController],
  providers: [MissYouService],
  exports: [MissYouService],
})
export class MissYouModule {}
