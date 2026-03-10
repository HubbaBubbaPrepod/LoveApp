import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { DailyQuestion } from './entities/daily-question.entity';
import { DailyAnswer } from './entities/daily-answer.entity';
import { IntimacyScore } from './entities/intimacy-score.entity';
import { IntimacyLog } from './entities/intimacy-log.entity';
import { QuestionsController } from './questions.controller';
import { QuestionsService } from './questions.service';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      DailyQuestion,
      DailyAnswer,
      IntimacyScore,
      IntimacyLog,
    ]),
  ],
  controllers: [QuestionsController],
  providers: [QuestionsService],
  exports: [QuestionsService],
})
export class QuestionsModule {}
