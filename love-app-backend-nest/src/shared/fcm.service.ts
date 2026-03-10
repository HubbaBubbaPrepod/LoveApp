import { Injectable, Inject } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { WINSTON_MODULE_PROVIDER } from 'nest-winston';
import { Logger } from 'winston';
import { FcmToken } from '../auth/entities/fcm-token.entity';
import { User } from '../users/entities/user.entity';
import { FirebaseService } from '../firebase/firebase.service';
import { CoupleService } from './couple.service';

@Injectable()
export class FcmService {
  constructor(
    @InjectRepository(FcmToken)
    private readonly fcmTokenRepo: Repository<FcmToken>,
    @InjectRepository(User)
    private readonly userRepo: Repository<User>,
    private readonly firebase: FirebaseService,
    private readonly coupleService: CoupleService,
    @Inject(WINSTON_MODULE_PROVIDER) private readonly logger: Logger,
  ) {}

  async sendPushToPartner(
    userId: number,
    data: {
      type: string;
      title?: string;
      body?: string;
      destination?: string;
      [key: string]: any;
    },
  ): Promise<void> {
    try {
      const partnerId = await this.coupleService.getPartnerId(userId);
      if (!partnerId) return;

      const tokenRow = await this.fcmTokenRepo.findOne({
        where: { user_id: partnerId },
      });
      if (!tokenRow?.fcm_token) return;

      const sender = await this.userRepo.findOne({
        where: { id: userId },
        select: ['display_name'],
      });

      const title =
        data.title || this.getDefaultTitle(data.type, sender?.display_name);
      const body =
        data.body || this.getDefaultBody(data.type, sender?.display_name);

      const stringData: Record<string, string> = {};
      for (const [k, v] of Object.entries(data)) {
        stringData[k] = String(v);
      }

      await this.firebase.messaging.send({
        token: tokenRow.fcm_token,
        data: stringData,
        notification: { title, body },
        android: { priority: 'high' },
      });
    } catch (err: any) {
      this.logger.warn(`FCM push failed: ${err.message}`);
    }
  }

  private getDefaultTitle(type: string, name?: string): string {
    const titles: Record<string, string> = {
      partner_mood: '😊 Настроение',
      partner_cycle: '🌸 Цикл',
      data_change: '💕 Обновление',
      partner_activity: '🎯 Активность',
      miss_you: '💕 Скучаю…',
      love_touch: '💕 Love Touch',
      daily_qa: '❓ Вопрос дня',
      moment: '📸 Новый момент',
      task_completed: '✅ Задание выполнено!',
      geofence_event: '📍 Геозона',
    };
    return titles[type] || `💕 ${name || 'Партнёр'}`;
  }

  private getDefaultBody(type: string, name?: string): string {
    const bodies: Record<string, string> = {
      partner_mood: `${name || 'Партнёр'} обновил(а) настроение`,
      data_change: 'Есть обновления',
      miss_you: 'Я скучаю по тебе!',
      love_touch: 'Приглашает тебя коснуться экрана вместе!',
    };
    return bodies[type] || '';
  }
}
