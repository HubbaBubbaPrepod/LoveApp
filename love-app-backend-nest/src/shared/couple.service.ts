import { Injectable, Inject } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Server } from 'socket.io';
import { RelationshipInfo } from '../relationship/entities/relationship-info.entity';
import { RedisService } from '../redis/redis.service';

@Injectable()
export class CoupleService {
  private io: Server | null = null;

  constructor(
    @InjectRepository(RelationshipInfo)
    private readonly relationshipRepo: Repository<RelationshipInfo>,
    private readonly redis: RedisService,
  ) {}

  setServer(io: Server) {
    this.io = io;
  }

  async getPartnerId(userId: number): Promise<number | null> {
    const rel = await this.relationshipRepo.findOne({
      where: [
        { user_id: userId },
      ],
    });
    return rel?.partner_id ?? null;
  }

  buildCoupleKey(userId: number, partnerId: number): string {
    const min = Math.min(userId, partnerId);
    const max = Math.max(userId, partnerId);
    return `${min}_${max}`;
  }

  async getCoupleKey(userId: number): Promise<string> {
    const partnerId = await this.getPartnerId(userId);
    if (!partnerId) return `solo_${userId}`;
    return this.buildCoupleKey(userId, partnerId);
  }

  async broadcastChange(
    coupleKey: string,
    senderId: number,
    entityType: string,
    action: string,
    data?: any,
  ) {
    const payload = {
      entityType,
      action,
      data,
      senderId,
      serverTimestamp: new Date().toISOString(),
    };

    if (this.io) {
      this.io
        .to(`couple:${coupleKey}`)
        .except(String(senderId))
        .emit('data-change', payload);
    }

    await this.redis.publish('loveapp:data-changes', {
      coupleKey,
      ...payload,
    });
  }
}
