import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, IsNull } from 'typeorm';
import { SleepEntry } from './entities/sleep-entry.entity';
import { CreateSleepDto } from './dto/create-sleep.dto';
import { CoupleService } from '../shared/couple.service';

@Injectable()
export class SleepService {
  constructor(
    @InjectRepository(SleepEntry)
    private readonly repo: Repository<SleepEntry>,
    private readonly coupleService: CoupleService,
  ) {}

  async upsert(userId: number, dto: CreateSleepDto) {
    const result = await this.repo
      .createQueryBuilder()
      .insert()
      .into(SleepEntry)
      .values({
        user_id: userId,
        ...dto,
      })
      .orUpdate(
        ['bedtime', 'wake_time', 'duration_minutes', 'quality', 'note', 'server_updated_at'],
        ['user_id', 'date'],
      )
      .setParameter('server_updated_at', new Date())
      .execute();

    const saved = await this.repo.findOne({
      where: { user_id: userId, date: dto.date, deleted_at: IsNull() },
    });

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'sleep_entry', 'create', saved);
    return saved;
  }

  async findAll(userId: number, query: { limit?: number; page?: number }) {
    const limit = Math.min(Math.max(query.limit || 20, 1), 100);
    const page = Math.max(query.page || 1, 1);
    const offset = (page - 1) * limit;

    const [items, total] = await this.repo.findAndCount({
      where: { user_id: userId, deleted_at: IsNull() },
      order: { date: 'DESC' },
      skip: offset,
      take: limit,
    });

    return { items, total, page, limit };
  }

  async findPartner(userId: number, query: { limit?: number }) {
    const partnerId = await this.coupleService.getPartnerId(userId);
    if (!partnerId) return { items: [] };

    const limit = Math.min(Math.max(query.limit || 7, 1), 50);

    const items = await this.repo.find({
      where: { user_id: partnerId, deleted_at: IsNull() },
      order: { date: 'DESC' },
      take: limit,
    });

    return { items };
  }

  async getStats(userId: number, query: { days?: number }) {
    const days = Math.min(Math.max(query.days || 7, 1), 90);

    const stats = await this.repo
      .createQueryBuilder('s')
      .select('AVG(s.duration_minutes)', 'avg_duration')
      .addSelect('MIN(s.duration_minutes)', 'min_duration')
      .addSelect('MAX(s.duration_minutes)', 'max_duration')
      .addSelect('COUNT(*)', 'entries')
      .where('s.user_id = :userId', { userId })
      .andWhere('s.deleted_at IS NULL')
      .andWhere(`s.date >= CURRENT_DATE - INTERVAL '${days} days'`)
      .getRawOne();

    // Mode quality
    const modeQuality = await this.repo
      .createQueryBuilder('s')
      .select('s.quality', 'quality')
      .addSelect('COUNT(*)', 'cnt')
      .where('s.user_id = :userId', { userId })
      .andWhere('s.deleted_at IS NULL')
      .andWhere('s.quality IS NOT NULL')
      .andWhere(`s.date >= CURRENT_DATE - INTERVAL '${days} days'`)
      .groupBy('s.quality')
      .orderBy('cnt', 'DESC')
      .limit(1)
      .getRawOne();

    return {
      avg_duration: stats?.avg_duration ? Math.round(Number(stats.avg_duration)) : null,
      min_duration: stats?.min_duration ? Number(stats.min_duration) : null,
      max_duration: stats?.max_duration ? Number(stats.max_duration) : null,
      entries: Number(stats?.entries || 0),
      mode_quality: modeQuality?.quality || null,
      days,
    };
  }

  async remove(userId: number, id: number) {
    const record = await this.repo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!record) throw new NotFoundException('Sleep entry not found');
    if (Number(record.user_id) !== userId) throw new ForbiddenException();

    record.deleted_at = new Date();
    record.server_updated_at = new Date();
    await this.repo.save(record);
    return { deleted: true };
  }
}
