import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, DataSource } from 'typeorm';
import { IntimacyScore } from '../questions/entities/intimacy-score.entity';
import { IntimacyLog } from '../questions/entities/intimacy-log.entity';
import { INTIMACY_LEVELS } from './constants/intimacy-levels';
import { CoupleService } from '../shared/couple.service';

@Injectable()
export class IntimacyService {
  constructor(
    @InjectRepository(IntimacyScore)
    private readonly scoreRepo: Repository<IntimacyScore>,
    @InjectRepository(IntimacyLog)
    private readonly logRepo: Repository<IntimacyLog>,
    private readonly dataSource: DataSource,
    private readonly coupleService: CoupleService,
  ) {}

  async get(userId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    let scoreEntry = await this.scoreRepo.findOne({ where: { couple_key: coupleKey } });
    if (!scoreEntry) {
      scoreEntry = { id: 0, couple_key: coupleKey, score: 0, level: 1, updated_at: new Date() };
    }

    const currentLevel = INTIMACY_LEVELS.find((l) => l.level === scoreEntry.level)
      || INTIMACY_LEVELS[0];
    const nextLevel = INTIMACY_LEVELS.find((l) => l.level === scoreEntry.level + 1);

    return {
      score: scoreEntry.score,
      level: scoreEntry.level,
      levelName: currentLevel.name,
      nextLevel: nextLevel
        ? { level: nextLevel.level, name: nextLevel.name, minScore: nextLevel.minScore }
        : null,
      levels: INTIMACY_LEVELS,
    };
  }

  async getHistory(userId: number, query: { page?: number; limit?: number }) {
    const page = Math.max(query.page || 1, 1);
    const limit = Math.min(Math.max(query.limit || 20, 1), 50);
    const offset = (page - 1) * limit;
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const items = await this.dataSource.query(
      `SELECT
         il.id,
         il.user_id,
         il.action_type,
         il.points,
         il.created_at,
         u.display_name,
         u.profile_image
       FROM intimacy_logs il
       LEFT JOIN users u ON u.id = il.user_id
       WHERE il.couple_key = $1
       ORDER BY il.created_at DESC
       LIMIT $2 OFFSET $3`,
      [coupleKey, limit, offset],
    );

    const countResult = await this.dataSource.query(
      `SELECT COUNT(*) AS total FROM intimacy_logs WHERE couple_key = $1`,
      [coupleKey],
    );

    return { items, total: +countResult[0].total, page, limit };
  }

  getLevels() {
    return INTIMACY_LEVELS;
  }
}
