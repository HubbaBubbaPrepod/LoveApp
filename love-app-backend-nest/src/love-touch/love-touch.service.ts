import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { LoveTouchSession } from './entities/love-touch-session.entity';
import { EndSessionDto } from './dto/end-session.dto';
import { CoupleService } from '../shared/couple.service';
import { FcmService } from '../shared/fcm.service';

@Injectable()
export class LoveTouchService {
  constructor(
    @InjectRepository(LoveTouchSession)
    private readonly repo: Repository<LoveTouchSession>,
    private readonly coupleService: CoupleService,
    private readonly fcmService: FcmService,
  ) {}

  async start(userId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const session = this.repo.create({
      couple_key: coupleKey,
      started_by: userId,
    });
    const saved = await this.repo.save(session);

    await this.coupleService.broadcastChange(
      coupleKey,
      userId,
      'love-touch-invite',
      'create',
      saved,
    );

    await this.fcmService.sendPushToPartner(userId, {
      type: 'love_touch',
      destination: 'love_touch',
    });

    return saved;
  }

  async join(userId: number, sessionId: number) {
    const session = await this.repo.findOne({ where: { id: sessionId } });
    if (!session) throw new NotFoundException('Session not found');

    session.partner_joined = true;
    const saved = await this.repo.save(session);

    await this.coupleService.broadcastChange(
      session.couple_key,
      userId,
      'love-touch-joined',
      'update',
      saved,
    );

    return saved;
  }

  async end(userId: number, sessionId: number, dto: EndSessionDto) {
    const session = await this.repo.findOne({ where: { id: sessionId } });
    if (!session) throw new NotFoundException('Session not found');

    session.ended_at = new Date();
    session.hearts_count = dto.hearts_count ?? 0;
    const saved = await this.repo.save(session);

    await this.coupleService.broadcastChange(
      session.couple_key,
      userId,
      'love-touch-ended',
      'update',
      saved,
    );

    return saved;
  }

  async getHistory(userId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    return this.repo.find({
      where: { couple_key: coupleKey },
      order: { created_at: 'DESC' },
      take: 50,
    });
  }
}
