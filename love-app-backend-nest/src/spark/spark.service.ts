import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { SparkStreak } from './entities/spark-streak.entity';
import { SparkLog } from './entities/spark-log.entity';
import { LogSparkDto } from './dto/log-spark.dto';
import { CoupleService } from '../shared/couple.service';

@Injectable()
export class SparkService {
  constructor(
    @InjectRepository(SparkStreak)
    private readonly streakRepo: Repository<SparkStreak>,
    @InjectRepository(SparkLog)
    private readonly logRepo: Repository<SparkLog>,
    private readonly coupleService: CoupleService,
  ) {}

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private yesterday(): string {
    const d = new Date();
    d.setDate(d.getDate() - 1);
    return d.toISOString().slice(0, 10);
  }

  async get(userId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    let streak = await this.streakRepo.findOne({ where: { couple_key: coupleKey } });

    if (!streak) {
      streak = this.streakRepo.create({ couple_key: coupleKey });
      streak = await this.streakRepo.save(streak);
    }

    // Auto-reset if stale
    if (streak.last_spark_date && streak.last_spark_date < this.yesterday()) {
      streak.current_streak = 0;
      streak = await this.streakRepo.save(streak);
    }

    return streak;
  }

  async log(userId: number, dto: LogSparkDto) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const today = this.today();

    // Insert log (ON CONFLICT DO NOTHING)
    await this.logRepo
      .createQueryBuilder()
      .insert()
      .into(SparkLog)
      .values({
        couple_key: coupleKey,
        user_id: userId,
        spark_type: dto.spark_type,
        date: today,
      })
      .orIgnore()
      .execute();

    // Count distinct users who logged today for this couple
    const todayLogs = await this.logRepo
      .createQueryBuilder('l')
      .select('COUNT(DISTINCT l.user_id)', 'cnt')
      .where('l.couple_key = :coupleKey', { coupleKey })
      .andWhere('l.date = :today', { today })
      .getRawOne();

    const bothLogged = Number(todayLogs?.cnt) >= 2;

    // Get or create streak
    let streak = await this.streakRepo.findOne({ where: { couple_key: coupleKey } });
    if (!streak) {
      streak = this.streakRepo.create({ couple_key: coupleKey });
    }

    if (bothLogged && streak.last_spark_date !== today) {
      const yesterday = this.yesterday();
      streak.current_streak =
        streak.last_spark_date === yesterday
          ? streak.current_streak + 1
          : 1;
      streak.longest_streak = Math.max(streak.longest_streak, streak.current_streak);
      streak.total_sparks += 1;
      streak.last_spark_date = today;
      streak = await this.streakRepo.save(streak);
    }

    await this.coupleService.broadcastChange(coupleKey, userId, 'spark', 'log');
    return streak;
  }

  async getHistory(userId: number, query: { limit?: number }) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const limit = Math.min(Math.max(query.limit || 14, 1), 30);

    const rows = await this.logRepo
      .createQueryBuilder('l')
      .select('l.date', 'date')
      .addSelect('COUNT(DISTINCT l.user_id)', 'participants')
      .addSelect('ARRAY_AGG(DISTINCT l.spark_type)', 'types')
      .where('l.couple_key = :coupleKey', { coupleKey })
      .groupBy('l.date')
      .orderBy('l.date', 'DESC')
      .limit(limit)
      .getRawMany();

    return rows;
  }

  async getBreakdown(userId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    // By type (last 30 days)
    const byType = await this.logRepo
      .createQueryBuilder('l')
      .select('l.spark_type', 'spark_type')
      .addSelect('COUNT(*)', 'count')
      .where('l.couple_key = :coupleKey', { coupleKey })
      .andWhere("l.date >= CURRENT_DATE - INTERVAL '30 days'")
      .groupBy('l.spark_type')
      .orderBy('count', 'DESC')
      .getRawMany();

    // Weekly (last 4 weeks)
    const weekly = await this.logRepo
      .createQueryBuilder('l')
      .select("DATE_TRUNC('week', l.date::timestamp)", 'week')
      .addSelect('COUNT(*)', 'count')
      .where('l.couple_key = :coupleKey', { coupleKey })
      .andWhere("l.date >= CURRENT_DATE - INTERVAL '28 days'")
      .groupBy('week')
      .orderBy('week', 'ASC')
      .getRawMany();

    return { byType, weekly };
  }
}
