import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AchievementDefinition } from './entities/achievement-definition.entity';
import { UserAchievement } from './entities/user-achievement.entity';
import { AchievementsController } from './achievements.controller';
import { AchievementsService } from './achievements.service';

@Module({
  imports: [TypeOrmModule.forFeature([AchievementDefinition, UserAchievement])],
  controllers: [AchievementsController],
  providers: [AchievementsService],
  exports: [AchievementsService],
})
export class AchievementsModule {}
