import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { LocationUpdate } from './entities/location-update.entity';
import { LocationHistory } from './entities/location-history.entity';
import { LocationSettings } from './entities/location-settings.entity';
import { UpdateLocationDto } from './dto/update-location.dto';
import { BatchLocationDto } from './dto/batch-location.dto';
import { UpdateSettingsDto } from './dto/update-settings.dto';
import { CoupleService } from '../shared/couple.service';
import { haversineKm } from './helpers/haversine.helper';

@Injectable()
export class LocationService {
  constructor(
    @InjectRepository(LocationUpdate)
    private readonly updateRepo: Repository<LocationUpdate>,
    @InjectRepository(LocationHistory)
    private readonly historyRepo: Repository<LocationHistory>,
    @InjectRepository(LocationSettings)
    private readonly settingsRepo: Repository<LocationSettings>,
    private readonly coupleService: CoupleService,
  ) {}

  async update(userId: number, dto: UpdateLocationDto) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const record = this.updateRepo.create({
      ...dto,
      user_id: userId,
      couple_key: coupleKey,
    });
    const saved = await this.updateRepo.save(record);

    await this.coupleService.broadcastChange(
      coupleKey,
      userId,
      'location',
      'update',
      {
        latitude: dto.latitude,
        longitude: dto.longitude,
        accuracy: dto.accuracy,
        speed: dto.speed,
        address: dto.address,
        is_moving: dto.is_moving,
        battery_level: dto.battery_level,
      },
    );

    return saved;
  }

  async batch(userId: number, dto: BatchLocationDto) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const records = dto.points.map((p) =>
      this.historyRepo.create({
        user_id: userId,
        couple_key: coupleKey,
        latitude: p.latitude,
        longitude: p.longitude,
        accuracy: p.accuracy,
        recorded_at: p.recorded_at ? new Date(p.recorded_at) : new Date(),
      }),
    );
    await this.historyRepo.save(records);
    return { inserted: records.length };
  }

  async getLatest(userId: number) {
    const partnerId = await this.coupleService.getPartnerId(userId);

    const own = await this.updateRepo.findOne({
      where: { user_id: userId },
      order: { created_at: 'DESC' },
    });

    let partner: LocationUpdate | null = null;
    if (partnerId) {
      partner = await this.updateRepo.findOne({
        where: { user_id: partnerId },
        order: { created_at: 'DESC' },
      });
    }

    return { own, partner };
  }

  async getHistory(userId: number, query: { hours?: number }) {
    const hours = Math.min(Math.max(query.hours || 24, 1), 168);
    const since = new Date(Date.now() - hours * 3600_000);
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const updates = await this.updateRepo
      .createQueryBuilder('u')
      .where('u.couple_key = :coupleKey', { coupleKey })
      .andWhere('u.created_at >= :since', { since })
      .orderBy('u.created_at', 'DESC')
      .limit(500)
      .getMany();

    const history = await this.historyRepo
      .createQueryBuilder('h')
      .where('h.couple_key = :coupleKey', { coupleKey })
      .andWhere('h.recorded_at >= :since', { since })
      .orderBy('h.recorded_at', 'DESC')
      .limit(500)
      .getMany();

    return { updates, history };
  }

  async getStats(
    userId: number,
    query: { start_date?: string; end_date?: string },
  ) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const endDate = query.end_date ? new Date(query.end_date) : new Date();
    const startDate = query.start_date
      ? new Date(query.start_date)
      : new Date(endDate.getTime() - 24 * 3600_000);

    const points = await this.updateRepo
      .createQueryBuilder('u')
      .where('u.user_id = :userId', { userId })
      .andWhere('u.couple_key = :coupleKey', { coupleKey })
      .andWhere('u.created_at BETWEEN :startDate AND :endDate', {
        startDate,
        endDate,
      })
      .orderBy('u.created_at', 'ASC')
      .getMany();

    let totalDistanceKm = 0;
    let maxSpeed = 0;
    const speeds: number[] = [];

    for (let i = 1; i < points.length; i++) {
      totalDistanceKm += haversineKm(
        +points[i - 1].latitude,
        +points[i - 1].longitude,
        +points[i].latitude,
        +points[i].longitude,
      );
      if (points[i].speed != null) {
        const s = +points[i].speed;
        speeds.push(s);
        if (s > maxSpeed) maxSpeed = s;
      }
    }

    const avgSpeed =
      speeds.length > 0
        ? speeds.reduce((a, b) => a + b, 0) / speeds.length
        : 0;

    return {
      total_distance_km: Math.round(totalDistanceKm * 100) / 100,
      max_speed: Math.round(maxSpeed * 100) / 100,
      avg_speed: Math.round(avgSpeed * 100) / 100,
      points_count: points.length,
    };
  }

  async getSettings(userId: number) {
    const settings = await this.settingsRepo.findOne({
      where: { user_id: userId },
    });
    return (
      settings ?? {
        sharing_enabled: true,
        update_interval: 300,
        show_address: true,
        show_speed: true,
        show_battery: true,
      }
    );
  }

  async updateSettings(userId: number, dto: UpdateSettingsDto) {
    let settings = await this.settingsRepo.findOne({
      where: { user_id: userId },
    });
    if (settings) {
      Object.assign(settings, dto);
    } else {
      settings = this.settingsRepo.create({ ...dto, user_id: userId });
    }
    return this.settingsRepo.save(settings);
  }

  async clearHistory(userId: number, days: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const since = new Date(Date.now() - days * 86_400_000);

    const [upd, hist] = await Promise.all([
      this.updateRepo
        .createQueryBuilder()
        .delete()
        .where('couple_key = :coupleKey', { coupleKey })
        .andWhere('created_at < :since', { since })
        .execute(),
      this.historyRepo
        .createQueryBuilder()
        .delete()
        .where('couple_key = :coupleKey', { coupleKey })
        .andWhere('recorded_at < :since', { since })
        .execute(),
    ]);

    return {
      deleted_updates: upd.affected ?? 0,
      deleted_history: hist.affected ?? 0,
    };
  }
}
