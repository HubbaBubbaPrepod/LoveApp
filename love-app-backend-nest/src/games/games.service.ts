import {
  Injectable,
  NotFoundException,
  BadRequestException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, DataSource } from 'typeorm';
import { GameQuestion } from './entities/game-question.entity';
import { GameSession } from './entities/game-session.entity';
import { GameRound } from './entities/game-round.entity';
import { StartGameDto } from './dto/start-game.dto';
import { AnswerDto } from './dto/answer.dto';
import { CoupleService } from '../shared/couple.service';

@Injectable()
export class GamesService {
  constructor(
    @InjectRepository(GameQuestion)
    private readonly questionRepo: Repository<GameQuestion>,
    @InjectRepository(GameSession)
    private readonly sessionRepo: Repository<GameSession>,
    @InjectRepository(GameRound)
    private readonly roundRepo: Repository<GameRound>,
    private readonly coupleService: CoupleService,
    private readonly dataSource: DataSource,
  ) {}

  async start(userId: number, dto: StartGameDto) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    // Cancel any active sessions for this couple
    await this.sessionRepo
      .createQueryBuilder()
      .update()
      .set({ status: 'cancelled' })
      .where('couple_key = :coupleKey AND status = :status', {
        coupleKey,
        status: 'active',
      })
      .execute();

    // Pick 5 random questions for this game type
    const questions = await this.questionRepo
      .createQueryBuilder('q')
      .where('q.game_type = :type', { type: dto.game_type })
      .orderBy('RANDOM()')
      .limit(5)
      .getMany();

    if (questions.length === 0) {
      throw new BadRequestException(
        `No questions available for type: ${dto.game_type}`,
      );
    }

    const totalRounds = Math.min(questions.length, 5);

    // Create session + rounds atomically
    const { savedSession, rounds } = await this.dataSource.transaction(async (manager) => {
      const session = manager.getRepository(GameSession).create({
        couple_key: coupleKey,
        game_type: dto.game_type,
        total_rounds: totalRounds,
        current_round: 1,
      });
      const savedSession = await manager.getRepository(GameSession).save(session);

      const roundRepo = manager.getRepository(GameRound);
      const rounds = await roundRepo.save(
        Array.from({ length: totalRounds }, (_, i) =>
          roundRepo.create({
            session_id: savedSession.id,
            round_number: i + 1,
            question_id: questions[i].id,
          }),
        ),
      );
      return { savedSession, rounds };
    });

    await this.coupleService.broadcastChange(
      coupleKey, userId, 'game', 'start', savedSession,
    );

    return {
      session: savedSession,
      current_round: rounds[0],
      question: questions[0],
    };
  }

  async getSession(userId: number, sessionId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const session = await this.sessionRepo.findOne({
      where: { id: sessionId, couple_key: coupleKey },
    });
    if (!session) throw new NotFoundException('Session not found');

    const rounds = await this.roundRepo.find({
      where: { session_id: sessionId },
      order: { round_number: 'ASC' },
    });

    const questionIds = rounds.map((r) => r.question_id);
    const questions =
      questionIds.length > 0
        ? await this.questionRepo
            .createQueryBuilder('q')
            .where('q.id IN (:...ids)', { ids: questionIds })
            .getMany()
        : [];

    const questionMap = new Map(questions.map((q) => [q.id, q]));

    return {
      session,
      rounds: rounds.map((r) => ({
        ...r,
        question: questionMap.get(r.question_id) || null,
      })),
    };
  }

  async answer(userId: number, dto: AnswerDto) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const partnerId = await this.coupleService.getPartnerId(userId);

    return this.dataSource.transaction(async (manager) => {
      const session = await manager.findOne(GameSession, {
        where: { id: dto.session_id, couple_key: coupleKey, status: 'active' },
      });
      if (!session) throw new NotFoundException('Active session not found');

      const round = await manager.findOne(GameRound, {
        where: { session_id: session.id, round_number: dto.round_number },
      });
      if (!round) throw new NotFoundException('Round not found');

      // Determine if user1 or user2 (user1 = smaller id)
      const isUser1 =
        partnerId == null || userId < partnerId;

      if (isUser1) {
        if (round.user1_answer)
          throw new BadRequestException('Already answered this round');
        round.user1_answer = dto.answer;
      } else {
        if (round.user2_answer)
          throw new BadRequestException('Already answered this round');
        round.user2_answer = dto.answer;
      }

      // Check if both answered
      if (round.user1_answer && round.user2_answer) {
        round.is_match = round.user1_answer === round.user2_answer;
      }

      await manager.save(GameRound, round);

      // Check if all rounds done
      const allRounds = await manager.find(GameRound, {
        where: { session_id: session.id },
        order: { round_number: 'ASC' },
      });

      const allDone = allRounds.every(
        (r) => r.user1_answer && r.user2_answer,
      );

      if (allDone) {
        const matches = allRounds.filter((r) => r.is_match).length;
        session.compatibility_score =
          Math.round((matches / allRounds.length) * 100 * 100) / 100;
        session.status = 'finished';
        session.current_round = session.total_rounds;
        await manager.save(GameSession, session);

        await this.coupleService.broadcastChange(
          coupleKey, userId, 'game', 'finished', {
            session_id: session.id,
            compatibility_score: session.compatibility_score,
          },
        );
      } else {
        // Advance current_round
        const nextIncomplete = allRounds.find(
          (r) => !r.user1_answer || !r.user2_answer,
        );
        if (nextIncomplete) {
          session.current_round = nextIncomplete.round_number;
          await manager.save(GameSession, session);
        }

        await this.coupleService.broadcastChange(
          coupleKey, userId, 'game', 'answer', {
            session_id: session.id,
            round_number: dto.round_number,
          },
        );
      }

      return { round, session };
    });
  }

  async getRecent(userId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    return this.sessionRepo.find({
      where: { couple_key: coupleKey },
      order: { created_at: 'DESC' },
      take: 10,
    });
  }

  async getQuestionsByType(type: string) {
    const questions = await this.questionRepo.find({
      where: { game_type: type },
    });

    const categories: Record<string, number> = {};
    for (const q of questions) {
      categories[q.category] = (categories[q.category] || 0) + 1;
    }

    return { total: questions.length, categories };
  }

  async getCompatibility(userId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const sessions = await this.sessionRepo.find({
      where: { couple_key: coupleKey, status: 'finished' },
      order: { created_at: 'DESC' },
    });

    if (sessions.length === 0) {
      return { average: 0, total_games: 0, by_type: {}, trend: [] };
    }

    const total = sessions.reduce(
      (sum, s) => sum + Number(s.compatibility_score || 0),
      0,
    );
    const average = Math.round((total / sessions.length) * 100) / 100;

    const byType: Record<string, { count: number; average: number }> = {};
    for (const s of sessions) {
      if (!byType[s.game_type]) {
        byType[s.game_type] = { count: 0, average: 0 };
      }
      byType[s.game_type].count++;
      byType[s.game_type].average += Number(s.compatibility_score || 0);
    }
    for (const key of Object.keys(byType)) {
      byType[key].average =
        Math.round((byType[key].average / byType[key].count) * 100) / 100;
    }

    const trend = sessions.slice(0, 10).map((s) => ({
      date: s.created_at,
      score: Number(s.compatibility_score),
      type: s.game_type,
    }));

    return { average, total_games: sessions.length, by_type: byType, trend };
  }
}
