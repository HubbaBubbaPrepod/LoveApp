import {
  Injectable,
  BadRequestException,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, DataSource, IsNull } from 'typeorm';
import { Moment } from './entities/moment.entity';
import { CreateMomentDto } from './dto/create-moment.dto';
import { CoupleService } from '../shared/couple.service';
import { FcmService } from '../shared/fcm.service';

@Injectable()
export class MomentsService {
  constructor(
    @InjectRepository(Moment)
    private readonly momentRepo: Repository<Moment>,
    private readonly dataSource: DataSource,
    private readonly coupleService: CoupleService,
    private readonly fcmService: FcmService,
  ) {}

  async create(userId: number, dto: CreateMomentDto) {
    if (!dto.content && !dto.image_url) {
      throw new BadRequestException('Content or image_url is required');
    }

    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const moment = this.momentRepo.create({
      ...dto,
      user_id: userId,
      couple_key: coupleKey,
    });
    const saved = await this.momentRepo.save(moment);

    // Award 3 intimacy points inline (no questions service import)
    await this.dataSource.query(
      `INSERT INTO intimacy_logs (couple_key, user_id, action_type, points)
       VALUES ($1, $2, 'moment', 3)`,
      [coupleKey, userId],
    );
    await this.dataSource.query(
      `INSERT INTO intimacy_scores (couple_key, score, level)
       VALUES ($1, 3, 1)
       ON CONFLICT (couple_key)
       DO UPDATE SET
         score = intimacy_scores.score + 3,
         level = FLOOR((intimacy_scores.score + 3) / 100) + 1,
         updated_at = NOW()`,
      [coupleKey],
    );

    await this.coupleService.broadcastChange(coupleKey, userId, 'moment', 'create', saved);

    await this.fcmService.sendPushToPartner(userId, {
      type: 'moment',
      destination: 'moments',
    });

    return saved;
  }

  async findAll(userId: number, query: { page?: number; limit?: number }) {
    const page = Math.max(query.page || 1, 1);
    const limit = Math.min(Math.max(query.limit || 20, 1), 50);
    const offset = (page - 1) * limit;
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const items = await this.dataSource.query(
      `SELECT
         m.id,
         m.user_id,
         m.content,
         m.image_url,
         m.mood,
         m.location_name,
         m.latitude,
         m.longitude,
         m.created_at,
         u.display_name
       FROM moments m
       LEFT JOIN users u ON u.id = m.user_id
       WHERE m.couple_key = $1 AND m.deleted_at IS NULL
       ORDER BY m.created_at DESC
       LIMIT $2 OFFSET $3`,
      [coupleKey, limit, offset],
    );

    const countResult = await this.dataSource.query(
      `SELECT COUNT(*) AS total FROM moments WHERE couple_key = $1 AND deleted_at IS NULL`,
      [coupleKey],
    );

    return { items, total: +countResult[0].total, page, limit };
  }

  async remove(userId: number, id: number) {
    const moment = await this.momentRepo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!moment) throw new NotFoundException('Moment not found');
    if (moment.user_id != userId) throw new ForbiddenException('Not the owner');

    moment.deleted_at = new Date();
    await this.momentRepo.save(moment);
    return { deleted: true };
  }
}
