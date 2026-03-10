import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, DataSource } from 'typeorm';
import { DailyQuestion } from './entities/daily-question.entity';
import { DailyAnswer } from './entities/daily-answer.entity';
import { IntimacyScore } from './entities/intimacy-score.entity';
import { IntimacyLog } from './entities/intimacy-log.entity';
import { AnswerQuestionDto } from './dto/answer-question.dto';
import { CoupleService } from '../shared/couple.service';
import { FcmService } from '../shared/fcm.service';

@Injectable()
export class QuestionsService {
  constructor(
    @InjectRepository(DailyQuestion)
    private readonly questionRepo: Repository<DailyQuestion>,
    @InjectRepository(DailyAnswer)
    private readonly answerRepo: Repository<DailyAnswer>,
    @InjectRepository(IntimacyScore)
    private readonly intimacyScoreRepo: Repository<IntimacyScore>,
    @InjectRepository(IntimacyLog)
    private readonly intimacyLogRepo: Repository<IntimacyLog>,
    private readonly dataSource: DataSource,
    private readonly coupleService: CoupleService,
    private readonly fcmService: FcmService,
  ) {}

  async addIntimacyPoints(
    coupleKey: string,
    userId: number,
    actionType: string,
    points: number,
  ): Promise<void> {
    await this.intimacyLogRepo.save({
      couple_key: coupleKey,
      user_id: userId,
      action_type: actionType,
      points,
    });

    await this.dataSource.query(
      `INSERT INTO intimacy_scores (couple_key, score, level)
       VALUES ($1, $2, FLOOR($2 / 100) + 1)
       ON CONFLICT (couple_key)
       DO UPDATE SET
         score = intimacy_scores.score + $2,
         level = FLOOR((intimacy_scores.score + $2) / 100) + 1,
         updated_at = NOW()`,
      [coupleKey, points],
    );
  }

  async getToday(userId: number) {
    const totalQuestions = await this.questionRepo.count();
    if (totalQuestions === 0) return { question: null, answers: [] };

    const now = new Date();
    const start = new Date(now.getFullYear(), 0, 0);
    const diff = now.getTime() - start.getTime();
    const dayOfYear = Math.floor(diff / (1000 * 60 * 60 * 24));
    const questionIndex = dayOfYear % totalQuestions;

    const questions = await this.questionRepo.find({
      order: { id: 'ASC' },
      skip: questionIndex,
      take: 1,
    });
    const question = questions[0];
    if (!question) return { question: null, answers: [] };

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const today = new Date().toISOString().slice(0, 10);

    const answers = await this.answerRepo.find({
      where: { question_id: question.id, couple_key: coupleKey, date: today },
    });

    return { question, answers };
  }

  async answer(userId: number, questionId: number, dto: AnswerQuestionDto) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const today = new Date().toISOString().slice(0, 10);

    await this.dataSource.query(
      `INSERT INTO daily_answers (question_id, user_id, couple_key, answer, date)
       VALUES ($1, $2, $3, $4, $5)
       ON CONFLICT (question_id, user_id, date)
       DO UPDATE SET answer = $4`,
      [questionId, userId, coupleKey, dto.answer, today],
    );

    await this.addIntimacyPoints(coupleKey, userId, 'daily_qa', 5);

    // Check if both partners answered today
    const answersToday = await this.answerRepo.count({
      where: { question_id: questionId, couple_key: coupleKey, date: today },
    });
    if (answersToday >= 2) {
      await this.addIntimacyPoints(coupleKey, userId, 'qa_both_answered', 10);
    }

    await this.fcmService.sendPushToPartner(userId, {
      type: 'daily_qa',
      destination: 'questions',
    });

    await this.coupleService.broadcastChange(
      coupleKey,
      userId,
      'daily_answer',
      'create',
    );

    return { success: true };
  }

  async getHistory(userId: number, query: { page?: number; limit?: number }) {
    const page = Math.max(query.page || 1, 1);
    const limit = Math.min(Math.max(query.limit || 10, 1), 50);
    const offset = (page - 1) * limit;
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const items = await this.dataSource.query(
      `SELECT
         q.id AS question_id,
         q.question_text,
         q.category,
         a1.answer AS my_answer,
         a2.answer AS partner_answer,
         a1.date
       FROM daily_answers a1
       JOIN daily_questions q ON q.id = a1.question_id
       LEFT JOIN daily_answers a2
         ON a2.question_id = a1.question_id
         AND a2.couple_key = a1.couple_key
         AND a2.date = a1.date
         AND a2.user_id != a1.user_id
       WHERE a1.user_id = $1 AND a1.couple_key = $2
       ORDER BY a1.date DESC, q.id DESC
       LIMIT $3 OFFSET $4`,
      [userId, coupleKey, limit, offset],
    );

    const countResult = await this.dataSource.query(
      `SELECT COUNT(*) AS total FROM daily_answers WHERE user_id = $1 AND couple_key = $2`,
      [userId, coupleKey],
    );

    return { items, total: +countResult[0].total, page, limit };
  }
}
