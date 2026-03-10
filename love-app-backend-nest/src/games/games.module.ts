import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { GameQuestion } from './entities/game-question.entity';
import { GameSession } from './entities/game-session.entity';
import { GameRound } from './entities/game-round.entity';
import { GamesController } from './games.controller';
import { GamesService } from './games.service';

@Module({
  imports: [TypeOrmModule.forFeature([GameQuestion, GameSession, GameRound])],
  controllers: [GamesController],
  providers: [GamesService],
  exports: [GamesService],
})
export class GamesModule {}
