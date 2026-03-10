import {
  Injectable,
  NotFoundException,
  ForbiddenException,
  BadRequestException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { ChatMessage } from './entities/chat-message.entity';
import { StickerPack } from './entities/sticker-pack.entity';
import { Sticker } from './entities/sticker.entity';
import { UserStickerPack } from './entities/user-sticker-pack.entity';
import { MissYouCounter } from './entities/miss-you-counter.entity';
import { SendMessageDto } from './dto/send-message.dto';
import { SendMissYouDto } from './dto/send-miss-you.dto';
import { CoupleService } from '../shared/couple.service';
import { FcmService } from '../shared/fcm.service';

@Injectable()
export class ChatService {
  constructor(
    @InjectRepository(ChatMessage)
    private readonly messageRepo: Repository<ChatMessage>,
    @InjectRepository(StickerPack)
    private readonly stickerPackRepo: Repository<StickerPack>,
    @InjectRepository(Sticker)
    private readonly stickerRepo: Repository<Sticker>,
    @InjectRepository(UserStickerPack)
    private readonly userStickerPackRepo: Repository<UserStickerPack>,
    @InjectRepository(MissYouCounter)
    private readonly missYouRepo: Repository<MissYouCounter>,
    private readonly coupleService: CoupleService,
    private readonly fcmService: FcmService,
  ) {}

  async send(userId: number, dto: SendMessageDto) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const message = this.messageRepo.create({
      ...dto,
      sender_id: userId,
      couple_key: coupleKey,
    });
    const saved = await this.messageRepo.save(message);

    await this.coupleService.broadcastChange(coupleKey, userId, 'chat_message', 'create', saved);

    const pushBody = this.getPushBody(dto);
    await this.fcmService.sendPushToPartner(userId, {
      type: 'chat_message',
      body: pushBody,
      destination: 'chat',
    });

    return saved;
  }

  async findAll(
    userId: number,
    partnerId: number,
    query: { page?: number; limit?: number },
  ) {
    const page = Math.max(query.page || 1, 1);
    const limit = Math.min(Math.max(query.limit || 20, 1), 50);
    const offset = (page - 1) * limit;

    const coupleKey = this.coupleService.buildCoupleKey(userId, partnerId);

    const qb = this.messageRepo.createQueryBuilder('m')
      .where('m.couple_key = :coupleKey', { coupleKey })
      .andWhere('m.deleted_at IS NULL')
      .orderBy('m.created_at', 'DESC')
      .skip(offset)
      .take(limit);

    const [items, total] = await qb.getManyAndCount();
    return { items, total, page, limit };
  }

  async markRead(userId: number, id: number) {
    const msg = await this.messageRepo.findOne({ where: { id } });
    if (!msg) throw new NotFoundException('Message not found');
    if (msg.receiver_id !== userId) throw new ForbiddenException('Not the receiver');

    msg.is_read = true;
    msg.read_at = new Date();
    return this.messageRepo.save(msg);
  }

  async markAllRead(userId: number, partnerId: number) {
    await this.messageRepo
      .createQueryBuilder()
      .update(ChatMessage)
      .set({ is_read: true, read_at: new Date() })
      .where('receiver_id = :userId', { userId })
      .andWhere('sender_id = :partnerId', { partnerId })
      .andWhere('is_read = false')
      .andWhere('deleted_at IS NULL')
      .execute();

    return { success: true };
  }

  async remove(userId: number, id: number) {
    const msg = await this.messageRepo.findOne({ where: { id } });
    if (!msg) throw new NotFoundException('Message not found');
    if (msg.sender_id !== userId) throw new ForbiddenException('Not the sender');

    msg.deleted_at = new Date();
    msg.server_updated_at = new Date();
    await this.messageRepo.save(msg);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'chat_message', 'delete', { id });

    return { id };
  }

  /* ── Stickers ── */

  async getStickerPacks(userId: number) {
    const packs = await this.stickerPackRepo
      .createQueryBuilder('p')
      .leftJoin(
        UserStickerPack,
        'usp',
        'usp.pack_id = p.id AND usp.user_id = :userId',
        { userId },
      )
      .addSelect('CASE WHEN usp.id IS NOT NULL THEN true ELSE false END', 'owned')
      .where('p.is_active = true')
      .orderBy('p.sort_order', 'ASC')
      .getRawAndEntities();

    return packs.entities.map((pack, i) => ({
      ...pack,
      owned: packs.raw[i].owned === true || packs.raw[i].owned === 'true',
    }));
  }

  async getStickerPack(packId: number) {
    const pack = await this.stickerPackRepo.findOne({ where: { id: packId } });
    if (!pack) throw new NotFoundException('Sticker pack not found');

    const stickers = await this.stickerRepo.find({
      where: { pack_id: packId },
      order: { sort_order: 'ASC' },
    });

    return { ...pack, stickers };
  }

  async acquirePack(userId: number, packId: number) {
    const pack = await this.stickerPackRepo.findOne({ where: { id: packId } });
    if (!pack) throw new NotFoundException('Sticker pack not found');

    const existing = await this.userStickerPackRepo.findOne({
      where: { user_id: userId, pack_id: packId },
    });
    if (existing) throw new BadRequestException('Pack already owned');

    if (!pack.is_free) {
      // TODO: deduct coins from user balance
    }

    const usp = this.userStickerPackRepo.create({
      user_id: userId,
      pack_id: packId,
    });
    return this.userStickerPackRepo.save(usp);
  }

  /* ── Miss You ── */

  async sendMissYou(userId: number, dto: SendMissYouDto) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const today = new Date().toISOString().slice(0, 10);

    await this.missYouRepo
      .createQueryBuilder()
      .insert()
      .into(MissYouCounter)
      .values({
        couple_key: coupleKey,
        user_id: userId,
        date: today,
        count: 1,
      })
      .orUpdate(['count', 'updated_at'], ['couple_key', 'user_id', 'date'])
      .setParameter('count', () => 'miss_you_counter.count + 1')
      .execute()
      .catch(async () => {
        // fallback: manual upsert
        const existing = await this.missYouRepo.findOne({
          where: { couple_key: coupleKey, user_id: userId, date: today },
        });
        if (existing) {
          existing.count += 1;
          existing.updated_at = new Date();
          await this.missYouRepo.save(existing);
        } else {
          await this.missYouRepo.save(
            this.missYouRepo.create({
              couple_key: coupleKey,
              user_id: userId,
              date: today,
              count: 1,
            }),
          );
        }
      });

    // Send an emoji message
    const emoji = dto.emoji || '❤️';
    const msg = this.messageRepo.create({
      sender_id: userId,
      receiver_id: 0, // will be resolved
      couple_key: coupleKey,
      message_type: 'emoji',
      emoji,
      content: dto.message || null,
    });
    const partnerId = await this.coupleService.getPartnerId(userId);
    if (partnerId) msg.receiver_id = partnerId;
    const saved = await this.messageRepo.save(msg);

    await this.coupleService.broadcastChange(coupleKey, userId, 'chat_message', 'create', saved);
    await this.fcmService.sendPushToPartner(userId, {
      type: 'miss_you',
      body: `${emoji} Скучаю по тебе!`,
      destination: 'chat',
    });

    return saved;
  }

  async getMissYouToday(userId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const today = new Date().toISOString().slice(0, 10);

    return this.missYouRepo.find({
      where: { couple_key: coupleKey, date: today },
    });
  }

  /* ── Helpers ── */

  private getPushBody(dto: SendMessageDto): string {
    const labels: Record<string, string> = {
      text: dto.content?.slice(0, 100) || 'Сообщение',
      image: '📷 Фото',
      voice: '🎤 Голосовое сообщение',
      video: '🎬 Видео',
      location: `📍 ${dto.location_name || 'Местоположение'}`,
      sticker: '🎨 Стикер',
      drawing: '✏️ Рисунок',
      emoji: dto.emoji || '💕',
      custom: 'Сообщение',
    };
    return labels[dto.message_type] || 'Новое сообщение';
  }
}
