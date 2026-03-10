import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, IsNull } from 'typeorm';
import { MenstrualCycle } from './entities/menstrual-cycle.entity';
import { CreateCycleDto } from './dto/create-cycle.dto';
import { UpdateDayDto } from './dto/update-day.dto';
import { CoupleService } from '../shared/couple.service';
import { FcmService } from '../shared/fcm.service';

@Injectable()
export class CyclesService {
  constructor(
    @InjectRepository(MenstrualCycle)
    private readonly cycleRepo: Repository<MenstrualCycle>,
    private readonly coupleService: CoupleService,
    private readonly fcmService: FcmService,
  ) {}

  async create(userId: number, dto: CreateCycleDto) {
    const cycle = this.cycleRepo.create({ ...dto, user_id: userId });
    const saved = await this.cycleRepo.save(cycle);

    await this.fcmService.sendPushToPartner(userId, {
      type: 'partner_cycle',
      destination: 'cycle',
    });

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'cycle', 'create', saved);

    return saved;
  }

  async findAll(userId: number) {
    return this.cycleRepo.find({
      where: { user_id: userId, deleted_at: IsNull() },
      order: { start_date: 'DESC' },
    });
  }

  async findLatest(userId: number) {
    return this.cycleRepo.findOne({
      where: { user_id: userId, deleted_at: IsNull() },
      order: { start_date: 'DESC' },
    });
  }

  async findPartner(userId: number) {
    const partnerId = await this.coupleService.getPartnerId(userId);
    if (!partnerId) return { items: [] };

    const items = await this.cycleRepo.find({
      where: { user_id: partnerId, deleted_at: IsNull() },
      order: { start_date: 'DESC' },
      take: 12,
    });

    return { items };
  }

  async update(userId: number, id: number, dto: Partial<CreateCycleDto>) {
    const cycle = await this.cycleRepo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!cycle) throw new NotFoundException('Cycle not found');
    if (Number(cycle.user_id) !== Number(userId))
      throw new ForbiddenException('Not the owner');

    Object.assign(cycle, dto, { server_updated_at: new Date() });
    const saved = await this.cycleRepo.save(cycle);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'cycle', 'update', saved);

    return saved;
  }

  async updateDay(userId: number, id: number, dto: UpdateDayDto) {
    const cycle = await this.cycleRepo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!cycle) throw new NotFoundException('Cycle not found');
    if (Number(cycle.user_id) !== Number(userId))
      throw new ForbiddenException('Not the owner');

    const symptomsValue = dto.symptoms ? JSON.stringify(dto.symptoms) : null;
    const moodValue = dto.mood ? JSON.stringify(dto.mood) : null;

    const setClauses: string[] = [];
    const params: any[] = [id];
    let paramIndex = 2;

    if (symptomsValue !== null) {
      setClauses.push(
        `symptoms = jsonb_set(COALESCE(symptoms, '{}'), $${paramIndex}::text[], $${paramIndex + 1}::jsonb)`,
      );
      params.push(`{${dto.date}}`, symptomsValue);
      paramIndex += 2;
    }
    if (moodValue !== null) {
      setClauses.push(
        `mood = jsonb_set(COALESCE(mood, '{}'), $${paramIndex}::text[], $${paramIndex + 1}::jsonb)`,
      );
      params.push(`{${dto.date}}`, moodValue);
      paramIndex += 2;
    }

    if (setClauses.length === 0) {
      return cycle;
    }

    setClauses.push('server_updated_at = NOW()');

    await this.cycleRepo.query(
      `UPDATE menstrual_cycles SET ${setClauses.join(', ')} WHERE id = $1 AND deleted_at IS NULL`,
      params,
    );

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'cycle', 'update', { id, date: dto.date });

    return this.cycleRepo.findOne({ where: { id } });
  }

  async remove(userId: number, id: number) {
    const cycle = await this.cycleRepo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!cycle) throw new NotFoundException('Cycle not found');
    if (Number(cycle.user_id) !== Number(userId))
      throw new ForbiddenException('Not the owner');

    cycle.deleted_at = new Date();
    cycle.server_updated_at = new Date();
    await this.cycleRepo.save(cycle);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'cycle', 'delete', { id });

    return { success: true };
  }
}
