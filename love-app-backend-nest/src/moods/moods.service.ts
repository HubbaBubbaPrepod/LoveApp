import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, IsNull } from 'typeorm';
import { MoodEntry } from './entities/mood-entry.entity';
import { CreateMoodDto } from './dto/create-mood.dto';
import { CoupleService } from '../shared/couple.service';
import { FcmService } from '../shared/fcm.service';

@Injectable()
export class MoodsService {
  constructor(
    @InjectRepository(MoodEntry)
    private readonly moodRepo: Repository<MoodEntry>,
    private readonly coupleService: CoupleService,
    private readonly fcmService: FcmService,
  ) {}

  async create(userId: number, dto: CreateMoodDto) {
    const mood = this.moodRepo.create({ ...dto, user_id: userId });
    const saved = await this.moodRepo.save(mood);

    await this.fcmService.sendPushToPartner(userId, {
      type: 'partner_mood',
      destination: 'mood',
    });

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'mood', 'create', saved);

    return saved;
  }

  async update(userId: number, id: number, dto: CreateMoodDto) {
    const mood = await this.moodRepo.findOne({
      where: { id, user_id: userId, deleted_at: IsNull() },
    });
    if (!mood) throw new NotFoundException('Mood entry not found');

    if (dto.mood_type !== undefined) mood.mood_type = dto.mood_type;
    if (dto.note !== undefined) mood.note = dto.note;
    mood.server_updated_at = new Date();
    const saved = await this.moodRepo.save(mood);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'mood', 'update', saved);

    return saved;
  }

  async findAll(userId: number, query: { page?: number; limit?: number }) {
    const page = Math.max(query.page || 1, 1);
    const limit = Math.min(Math.max(query.limit || 20, 1), 50);
    const offset = (page - 1) * limit;

    const [items, total] = await this.moodRepo.findAndCount({
      where: { user_id: userId, deleted_at: IsNull() },
      order: { created_at: 'DESC' },
      skip: offset,
      take: limit,
    });

    return { items, total, page, limit };
  }

  async findPartner(userId: number) {
    const partnerId = await this.coupleService.getPartnerId(userId);
    if (!partnerId) return { items: [] };

    const items = await this.moodRepo.find({
      where: { user_id: partnerId, deleted_at: IsNull() },
      order: { created_at: 'DESC' },
      take: 50,
    });

    return { items };
  }

  async analytics(userId: number) {
    const partnerId = await this.coupleService.getPartnerId(userId);

    // Distribution: count by mood_type
    const distribution = await this.moodRepo
      .createQueryBuilder('m')
      .select('m.mood_type', 'mood_type')
      .addSelect('COUNT(*)', 'count')
      .where('m.user_id = :userId', { userId })
      .andWhere('m.deleted_at IS NULL')
      .groupBy('m.mood_type')
      .getRawMany();

    // Daily trends: last 30 days
    const dailyTrends = await this.moodRepo
      .createQueryBuilder('m')
      .select("DATE(m.created_at)", 'date')
      .addSelect('m.mood_type', 'mood_type')
      .addSelect('COUNT(*)', 'count')
      .where('m.user_id = :userId', { userId })
      .andWhere('m.deleted_at IS NULL')
      .andWhere("m.created_at >= NOW() - INTERVAL '30 days'")
      .groupBy("DATE(m.created_at)")
      .addGroupBy('m.mood_type')
      .orderBy("DATE(m.created_at)", 'ASC')
      .getRawMany();

    // Day-of-week averages
    const dayOfWeek = await this.moodRepo
      .createQueryBuilder('m')
      .select("EXTRACT(DOW FROM m.created_at)", 'dow')
      .addSelect('m.mood_type', 'mood_type')
      .addSelect('COUNT(*)', 'count')
      .where('m.user_id = :userId', { userId })
      .andWhere('m.deleted_at IS NULL')
      .groupBy("EXTRACT(DOW FROM m.created_at)")
      .addGroupBy('m.mood_type')
      .orderBy('dow', 'ASC')
      .getRawMany();

    // Streak: consecutive days with a mood entry
    const streakResult = await this.moodRepo.query(
      `WITH dates AS (
        SELECT DISTINCT DATE(created_at) AS d
        FROM mood_entries
        WHERE user_id = $1 AND deleted_at IS NULL
      ),
      numbered AS (
        SELECT d, d - (ROW_NUMBER() OVER (ORDER BY d))::int * INTERVAL '1 day' AS grp
        FROM dates
      )
      SELECT MAX(cnt) AS streak FROM (
        SELECT COUNT(*) AS cnt FROM numbered GROUP BY grp
      ) sub`,
      [userId],
    );
    const streak = streakResult[0]?.streak ? Number(streakResult[0].streak) : 0;

    // Partner distribution
    let partnerDistribution: any[] = [];
    if (partnerId) {
      partnerDistribution = await this.moodRepo
        .createQueryBuilder('m')
        .select('m.mood_type', 'mood_type')
        .addSelect('COUNT(*)', 'count')
        .where('m.user_id = :partnerId', { partnerId })
        .andWhere('m.deleted_at IS NULL')
        .groupBy('m.mood_type')
        .getRawMany();
    }

    return { distribution, dailyTrends, dayOfWeek, streak, partnerDistribution };
  }

  async remove(userId: number, id: number) {
    const mood = await this.moodRepo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!mood) throw new NotFoundException('Mood entry not found');
    if (mood.user_id !== userId) throw new ForbiddenException('Not the owner');

    mood.deleted_at = new Date();
    mood.server_updated_at = new Date();
    await this.moodRepo.save(mood);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'mood', 'delete', { id });

    return { id };
  }
}
