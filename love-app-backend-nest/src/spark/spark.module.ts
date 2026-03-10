import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { SparkStreak } from './entities/spark-streak.entity';
import { SparkLog } from './entities/spark-log.entity';
import { SparkController } from './spark.controller';
import { SparkService } from './spark.service';

@Module({
  imports: [TypeOrmModule.forFeature([SparkStreak, SparkLog])],
  controllers: [SparkController],
  providers: [SparkService],
  exports: [SparkService],
})
export class SparkModule {}
