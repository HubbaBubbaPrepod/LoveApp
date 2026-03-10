import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { MissYouEvent } from './entities/miss-you-event.entity';
import { SendMissYouDto } from './dto/send-miss-you.dto';
import { SparkLog } from '../spark/entities/spark-log.entity';
import { CoupleService } from '../shared/couple.service';
import { FcmService } from '../shared/fcm.service';

@Injectable()
export class MissYouService {
  constructor(
    @InjectRepository(MissYouEvent)
    private readonly repo: Repository<MissYouEvent>,
    @InjectRepository(SparkLog)
    private readonly sparkLogRepo: Repository<SparkLog>,
    private readonly coupleService: CoupleService,
    private readonly fcmService: FcmService,
  ) {}

  async send(userId: number, dto: SendMissYouDto) {
    const partnerId = await this.coupleService.getPartnerId(userId);
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const event = this.repo.create({
      sender_id: userId,
      receiver_id: partnerId,
      couple_key: coupleKey,
      emoji: dto.emoji ?? '❤️',
      message: dto.message,
    });
    const saved = await this.repo.save(event);

    // spark log — ON CONFLICT DO NOTHING
    const today = new Date().toISOString().slice(0, 10);
    await this.sparkLogRepo
      .createQueryBuilder()
      .insert()
      .into(SparkLog)
      .values({
        couple_key: coupleKey,
        user_id: userId,
        spark_type: 'miss_you',
        date: today,
      })
      .orIgnore()
      .execute();

    await this.coupleService.broadcastChange(
      coupleKey,
      userId,
      'miss_you',
      'create',
      saved,
    );

    await this.fcmService.sendPushToPartner(userId, {
      type: 'miss_you',
      destination: 'miss_you',
    });

    return saved;
  }

  async findAll(userId: number, query: { page?: number; limit?: number }) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const page = Math.max(query.page || 1, 1);
    const limit = Math.min(Math.max(query.limit || 30, 1), 100);
    const offset = (page - 1) * limit;

    const [items, total] = await this.repo
      .createQueryBuilder('e')
      .leftJoin('users', 'u', 'u.id = e.sender_id')
      .addSelect('u.display_name', 'sender_display_name')
      .addSelect('u.profile_image', 'sender_profile_image')
      .where('e.couple_key = :coupleKey', { coupleKey })
      .orderBy('e.created_at', 'DESC')
      .skip(offset)
      .take(limit)
      .getManyAndCount();

    return { items, total, page, limit };
  }

  async getToday(userId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const today = new Date().toISOString().slice(0, 10);

    const result = await this.repo
      .createQueryBuilder('e')
      .select('COUNT(*)', 'total')
      .addSelect(
        `COUNT(*) FILTER (WHERE e.sender_id = :userId)`,
        'sent_by_me',
      )
      .addSelect(
        `COUNT(*) FILTER (WHERE e.sender_id != :userId)`,
        'sent_by_partner',
      )
      .where('e.couple_key = :coupleKey', { coupleKey })
      .andWhere('e.created_at::date = :today', { today })
      .setParameter('userId', userId)
      .getRawOne();

    return {
      total: Number(result.total),
      sent_by_me: Number(result.sent_by_me),
      sent_by_partner: Number(result.sent_by_partner),
    };
  }
}
