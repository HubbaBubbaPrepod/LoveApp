import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, IsNull } from 'typeorm';
import { CustomCalendar } from './entities/custom-calendar.entity';
import { CustomCalendarEvent } from './entities/custom-calendar-event.entity';
import { CreateCalendarDto } from './dto/create-calendar.dto';
import { CreateEventDto } from './dto/create-event.dto';
import { CoupleService } from '../shared/couple.service';
import { FcmService } from '../shared/fcm.service';

@Injectable()
export class CalendarsService {
  constructor(
    @InjectRepository(CustomCalendar)
    private readonly calendarRepo: Repository<CustomCalendar>,
    @InjectRepository(CustomCalendarEvent)
    private readonly eventRepo: Repository<CustomCalendarEvent>,
    private readonly coupleService: CoupleService,
    private readonly fcmService: FcmService,
  ) {}

  // ─── Calendars ──────────────────────────────────────────

  async create(userId: number, dto: CreateCalendarDto) {
    const calendar = this.calendarRepo.create({ ...dto, user_id: userId });
    const saved = await this.calendarRepo.save(calendar);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'calendar', 'create', saved);

    return saved;
  }

  async findAll(userId: number) {
    return this.calendarRepo.find({
      where: { user_id: userId, deleted_at: IsNull() },
      order: { created_at: 'ASC' },
    });
  }

  async findPartner(userId: number) {
    const partnerId = await this.coupleService.getPartnerId(userId);
    if (!partnerId) return { items: [] };

    const items = await this.calendarRepo.find({
      where: { user_id: partnerId, deleted_at: IsNull() },
      order: { created_at: 'ASC' },
    });

    return { items };
  }

  async update(userId: number, id: number, dto: Partial<CreateCalendarDto>) {
    const calendar = await this.calendarRepo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!calendar) throw new NotFoundException('Calendar not found');
    if (Number(calendar.user_id) !== Number(userId))
      throw new ForbiddenException('Not the owner');

    Object.assign(calendar, dto, { server_updated_at: new Date() });
    const saved = await this.calendarRepo.save(calendar);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'calendar', 'update', saved);

    return saved;
  }

  async remove(userId: number, id: number) {
    const calendar = await this.calendarRepo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!calendar) throw new NotFoundException('Calendar not found');
    if (Number(calendar.user_id) !== Number(userId))
      throw new ForbiddenException('Not the owner');

    calendar.deleted_at = new Date();
    calendar.server_updated_at = new Date();
    await this.calendarRepo.save(calendar);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'calendar', 'delete', { id });

    return { success: true };
  }

  // ─── Events ─────────────────────────────────────────────

  async checkCalendarAccess(userId: number, calendarId: number) {
    const calendar = await this.calendarRepo.findOne({
      where: { id: calendarId, deleted_at: IsNull() },
    });
    if (!calendar) throw new NotFoundException('Calendar not found');

    if (Number(calendar.user_id) === Number(userId)) return calendar;

    const partnerId = await this.coupleService.getPartnerId(userId);
    if (partnerId && Number(calendar.user_id) === Number(partnerId)) return calendar;

    throw new ForbiddenException('No access to this calendar');
  }

  async createEvent(userId: number, dto: CreateEventDto) {
    await this.checkCalendarAccess(userId, dto.calendar_id);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const event = this.eventRepo.create({
      ...dto,
      user_id: userId,
      couple_key: coupleKey,
    });
    const saved = await this.eventRepo.save(event);

    await this.fcmService.sendPushToPartner(userId, {
      type: 'partner_calendar_event',
      destination: 'calendar',
    });

    await this.coupleService.broadcastChange(coupleKey, userId, 'calendar_event', 'create', saved);

    return saved;
  }

  async findEvents(
    userId: number,
    query: { calendar_id?: number; start_date?: string; end_date?: string },
  ) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const qb = this.eventRepo
      .createQueryBuilder('e')
      .where('e.deleted_at IS NULL')
      .andWhere('(e.user_id = :userId OR e.couple_key = :coupleKey)', { userId, coupleKey });

    if (query.calendar_id) {
      qb.andWhere('e.calendar_id = :calendarId', { calendarId: query.calendar_id });
    }
    if (query.start_date) {
      qb.andWhere('e.event_date >= :startDate', { startDate: query.start_date });
    }
    if (query.end_date) {
      qb.andWhere('e.event_date <= :endDate', { endDate: query.end_date });
    }

    qb.orderBy('e.event_date', 'ASC').addOrderBy('e.start_time', 'ASC');

    return qb.getMany();
  }

  async updateEvent(userId: number, id: number, dto: Partial<CreateEventDto>) {
    const event = await this.eventRepo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!event) throw new NotFoundException('Event not found');
    if (Number(event.user_id) !== Number(userId))
      throw new ForbiddenException('Not the owner');

    Object.assign(event, dto, { server_updated_at: new Date() });
    const saved = await this.eventRepo.save(event);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'calendar_event', 'update', saved);

    return saved;
  }

  async removeEvent(userId: number, id: number) {
    const event = await this.eventRepo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!event) throw new NotFoundException('Event not found');
    if (Number(event.user_id) !== Number(userId))
      throw new ForbiddenException('Not the owner');

    event.deleted_at = new Date();
    event.server_updated_at = new Date();
    await this.eventRepo.save(event);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'calendar_event', 'delete', { id });

    return { success: true };
  }
}
