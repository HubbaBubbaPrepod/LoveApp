import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { PhoneStatus } from './entities/phone-status.entity';
import { PhoneStatusHistory } from './entities/phone-status-history.entity';
import { UpdatePhoneStatusDto } from './dto/update-phone-status.dto';
import { CoupleService } from '../shared/couple.service';

@Injectable()
export class PhoneStatusService {
  constructor(
    @InjectRepository(PhoneStatus)
    private readonly statusRepo: Repository<PhoneStatus>,
    @InjectRepository(PhoneStatusHistory)
    private readonly historyRepo: Repository<PhoneStatusHistory>,
    private readonly coupleService: CoupleService,
  ) {}

  async update(userId: number, dto: UpdatePhoneStatusDto) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    let status = await this.statusRepo.findOne({
      where: { user_id: userId },
    });

    if (status) {
      Object.assign(status, dto, { couple_key: coupleKey });
      if (dto.is_active) status.last_active_at = new Date();
    } else {
      status = this.statusRepo.create({
        ...dto,
        user_id: userId,
        couple_key: coupleKey,
        last_active_at: dto.is_active ? new Date() : null,
      });
    }

    const saved = await this.statusRepo.save(status);

    // Sample to history if no record in last 5 minutes
    const fiveMinAgo = new Date(Date.now() - 5 * 60_000);
    const recentHistory = await this.historyRepo
      .createQueryBuilder('h')
      .where('h.user_id = :userId', { userId })
      .andWhere('h.recorded_at >= :fiveMinAgo', { fiveMinAgo })
      .getCount();

    if (recentHistory === 0) {
      const historyRecord = this.historyRepo.create({
        user_id: userId,
        couple_key: coupleKey,
        battery_level: saved.battery_level,
        is_charging: saved.is_charging,
        screen_status: saved.screen_status,
        wifi_name: saved.wifi_name,
        is_active: saved.is_active,
        app_in_foreground: saved.app_in_foreground,
        network_type: saved.network_type,
      });
      await this.historyRepo.save(historyRecord);
    }

    await this.coupleService.broadcastChange(
      coupleKey,
      userId,
      'phone-status',
      'update',
      {
        battery_level: saved.battery_level,
        is_charging: saved.is_charging,
        screen_status: saved.screen_status,
        is_active: saved.is_active,
        app_in_foreground: saved.app_in_foreground,
        network_type: saved.network_type,
      },
    );

    return saved;
  }

  async getPartner(userId: number) {
    const partnerId = await this.coupleService.getPartnerId(userId);
    if (!partnerId) return null;

    return this.statusRepo
      .createQueryBuilder('ps')
      .leftJoin('users', 'u', 'u.id = ps.user_id')
      .addSelect('u.display_name', 'display_name')
      .addSelect('u.avatar', 'avatar')
      .where('ps.user_id = :partnerId', { partnerId })
      .getRawOne();
  }

  async getMe(userId: number) {
    return this.statusRepo.findOne({ where: { user_id: userId } });
  }

  async getHistory(userId: number, query: { hours?: number }) {
    const partnerId = await this.coupleService.getPartnerId(userId);
    if (!partnerId) return [];

    const hours = Math.min(Math.max(query.hours || 24, 1), 72);
    const since = new Date(Date.now() - hours * 3600_000);

    return this.historyRepo
      .createQueryBuilder('h')
      .where('h.user_id = :partnerId', { partnerId })
      .andWhere('h.recorded_at >= :since', { since })
      .orderBy('h.recorded_at', 'DESC')
      .limit(300)
      .getMany();
  }

  async getBoth(userId: number) {
    const own = await this.getMe(userId);
    const partner = await this.getPartner(userId);
    return { own, partner };
  }
}
