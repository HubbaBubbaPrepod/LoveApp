import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, IsNull } from 'typeorm';
import { ActivityLog } from './entities/activity-log.entity';
import { CustomActivityType } from './entities/custom-activity-type.entity';
import { CreateActivityDto } from './dto/create-activity.dto';
import { CreateCustomTypeDto } from './dto/create-custom-type.dto';
import { CoupleService } from '../shared/couple.service';
import { FcmService } from '../shared/fcm.service';

@Injectable()
export class ActivitiesService {
  constructor(
    @InjectRepository(ActivityLog)
    private readonly activityRepo: Repository<ActivityLog>,
    @InjectRepository(CustomActivityType)
    private readonly customTypeRepo: Repository<CustomActivityType>,
    private readonly coupleService: CoupleService,
    private readonly fcmService: FcmService,
  ) {}

  // ─── Activity Logs ───────────────────────────────────────

  async create(userId: number, dto: CreateActivityDto) {
    const activity = this.activityRepo.create({ ...dto, user_id: userId });
    const saved = await this.activityRepo.save(activity);

    await this.fcmService.sendPushToPartner(userId, {
      type: 'partner_activity',
      destination: 'activity',
    });

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'activity', 'create', saved);

    return saved;
  }

  async findAll(
    userId: number,
    query: { page?: number; limit?: number; category?: string; start_date?: string; end_date?: string },
  ) {
    const page = Math.max(query.page || 1, 1);
    const limit = Math.min(Math.max(query.limit || 20, 1), 50);
    const offset = (page - 1) * limit;

    const qb = this.activityRepo
      .createQueryBuilder('a')
      .where('a.user_id = :userId', { userId })
      .andWhere('a.deleted_at IS NULL');

    if (query.category) {
      qb.andWhere('a.category = :category', { category: query.category });
    }
    if (query.start_date) {
      qb.andWhere('a.event_date >= :startDate', { startDate: query.start_date });
    }
    if (query.end_date) {
      qb.andWhere('a.event_date <= :endDate', { endDate: query.end_date });
    }

    qb.orderBy('a.event_date', 'DESC').skip(offset).take(limit);

    const [items, total] = await qb.getManyAndCount();
    return { items, total, page, limit };
  }

  async findPartner(userId: number) {
    const partnerId = await this.coupleService.getPartnerId(userId);
    if (!partnerId) return { items: [] };

    const items = await this.activityRepo.find({
      where: { user_id: partnerId, deleted_at: IsNull() },
      order: { event_date: 'DESC' },
      take: 50,
    });

    return { items };
  }

  async update(userId: number, id: number, dto: Partial<CreateActivityDto>) {
    const activity = await this.activityRepo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!activity) throw new NotFoundException('Activity not found');
    if (Number(activity.user_id) !== Number(userId))
      throw new ForbiddenException('Not the owner');

    Object.assign(activity, dto, { server_updated_at: new Date() });
    const saved = await this.activityRepo.save(activity);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'activity', 'update', saved);

    return saved;
  }

  async remove(userId: number, id: number) {
    const activity = await this.activityRepo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!activity) throw new NotFoundException('Activity not found');
    if (Number(activity.user_id) !== Number(userId))
      throw new ForbiddenException('Not the owner');

    activity.deleted_at = new Date();
    activity.server_updated_at = new Date();
    await this.activityRepo.save(activity);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'activity', 'delete', { id });

    return { success: true };
  }

  // ─── Custom Activity Types ──────────────────────────────

  async createType(userId: number, dto: CreateCustomTypeDto) {
    const type = this.customTypeRepo.create({ ...dto, user_id: userId });
    return this.customTypeRepo.save(type);
  }

  async getTypes(userId: number) {
    return this.customTypeRepo.find({
      where: { user_id: userId },
      order: { created_at: 'ASC' },
    });
  }

  async deleteType(userId: number, id: number) {
    const type = await this.customTypeRepo.findOne({ where: { id } });
    if (!type) throw new NotFoundException('Custom type not found');
    if (Number(type.user_id) !== Number(userId))
      throw new ForbiddenException('Not the owner');

    await this.customTypeRepo.remove(type);
    return { success: true };
  }
}
