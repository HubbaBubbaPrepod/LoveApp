import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { StoryEntry } from './entities/story-entry.entity';
import { StoryController } from './story.controller';
import { StoryService } from './story.service';

@Module({
  imports: [TypeOrmModule.forFeature([StoryEntry])],
  controllers: [StoryController],
  providers: [StoryService],
  exports: [StoryService],
})
export class StoryModule {}
