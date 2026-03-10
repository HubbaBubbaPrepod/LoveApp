import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Geofence } from './entities/geofence.entity';
import { GeofenceEvent } from './entities/geofence-event.entity';
import { CreateGeofenceDto } from './dto/create-geofence.dto';
import { CreateGeofenceEventDto } from './dto/create-geofence-event.dto';
import { CoupleService } from '../shared/couple.service';
import { FcmService } from '../shared/fcm.service';

@Injectable()
export class GeofencesService {
  constructor(
    @InjectRepository(Geofence)
    private readonly geofenceRepo: Repository<Geofence>,
    @InjectRepository(GeofenceEvent)
    private readonly eventRepo: Repository<GeofenceEvent>,
    private readonly coupleService: CoupleService,
    private readonly fcmService: FcmService,
  ) {}

  async create(userId: number, dto: CreateGeofenceDto) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const geofence = this.geofenceRepo.create({
      ...dto,
      user_id: userId,
      couple_key: coupleKey,
    });
    const saved = await this.geofenceRepo.save(geofence);

    await this.coupleService.broadcastChange(
      coupleKey,
      userId,
      'geofence',
      'create',
      saved,
    );

    return saved;
  }

  async findAll(userId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    return this.geofenceRepo.find({
      where: { couple_key: coupleKey },
      order: { created_at: 'DESC' },
    });
  }

  async findOne(userId: number, id: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const geofence = await this.geofenceRepo.findOne({ where: { id } });
    if (!geofence) throw new NotFoundException('Geofence not found');
    if (geofence.couple_key !== coupleKey)
      throw new ForbiddenException('Not your geofence');
    return geofence;
  }

  async update(userId: number, id: number, dto: Partial<CreateGeofenceDto>) {
    const geofence = await this.findOne(userId, id);
    Object.assign(geofence, dto);
    const saved = await this.geofenceRepo.save(geofence);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(
      coupleKey,
      userId,
      'geofence',
      'update',
      saved,
    );

    return saved;
  }

  async remove(userId: number, id: number) {
    const geofence = await this.findOne(userId, id);
    await this.geofenceRepo.remove(geofence);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(
      coupleKey,
      userId,
      'geofence',
      'delete',
      { id },
    );

    return { deleted: true };
  }

  async recordEvent(userId: number, dto: CreateGeofenceEventDto) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const geofence = await this.geofenceRepo.findOne({
      where: { id: dto.geofence_id },
    });
    if (!geofence) throw new NotFoundException('Geofence not found');

    const event = this.eventRepo.create({
      ...dto,
      user_id: userId,
      couple_key: coupleKey,
    });
    const saved = await this.eventRepo.save(event);

    await this.coupleService.broadcastChange(
      coupleKey,
      userId,
      'geofence-event',
      dto.event_type,
      { ...saved, geofence_name: geofence.name },
    );

    if (
      (dto.event_type === 'enter' && geofence.notify_on_enter) ||
      (dto.event_type === 'exit' && geofence.notify_on_exit)
    ) {
      await this.fcmService.sendPushToPartner(userId, {
        type: 'geofence_event',
        title: `Geofence ${dto.event_type}`,
        body: `Partner ${dto.event_type === 'enter' ? 'arrived at' : 'left'} ${geofence.name}`,
        destination: 'geofences',
      });
    }

    return saved;
  }

  async getEvents(
    geofenceId: number,
    query: { hours?: number },
  ) {
    const hours = Math.min(Math.max(query.hours || 168, 1), 720);
    const since = new Date(Date.now() - hours * 3600_000);

    return this.eventRepo
      .createQueryBuilder('e')
      .where('e.geofence_id = :geofenceId', { geofenceId })
      .andWhere('e.triggered_at >= :since', { since })
      .orderBy('e.triggered_at', 'DESC')
      .getMany();
  }

  async getRecentEvents(
    userId: number,
    query: { limit?: number },
  ) {
    const limit = Math.min(Math.max(query.limit || 50, 1), 200);
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    return this.eventRepo
      .createQueryBuilder('e')
      .where('e.couple_key = :coupleKey', { coupleKey })
      .orderBy('e.triggered_at', 'DESC')
      .limit(limit)
      .getMany();
  }
}
