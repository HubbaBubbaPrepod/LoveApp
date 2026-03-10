import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { MoodEntry } from './entities/mood-entry.entity';
import { MoodsController } from './moods.controller';
import { MoodsService } from './moods.service';

@Module({
  imports: [TypeOrmModule.forFeature([MoodEntry])],
  controllers: [MoodsController],
  providers: [MoodsService],
  exports: [MoodsService],
})
export class MoodsModule {}
