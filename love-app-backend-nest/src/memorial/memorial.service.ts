import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, IsNull } from 'typeorm';
import { MemorialDay } from './entities/memorial-day.entity';
import { CreateMemorialDto } from './dto/create-memorial.dto';
import { UpdateMemorialDto } from './dto/update-memorial.dto';
import { CoupleService } from '../shared/couple.service';

@Injectable()
export class MemorialService {
  constructor(
    @InjectRepository(MemorialDay)
    private readonly repo: Repository<MemorialDay>,
    private readonly coupleService: CoupleService,
  ) {}

  async create(userId: number, dto: CreateMemorialDto) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const entity = this.repo.create({ ...dto, user_id: userId, couple_key: coupleKey });
    const saved = await this.repo.save(entity);
    await this.coupleService.broadcastChange(coupleKey, userId, 'memorial', 'create', saved);
    return saved;
  }

  async findAll(userId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    return this.repo.find({
      where: { couple_key: coupleKey, deleted_at: IsNull() },
      order: { date: 'ASC' },
    });
  }

  async update(userId: number, id: number, dto: UpdateMemorialDto) {
    const record = await this.repo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!record) throw new NotFoundException('Memorial day not found');
    if (Number(record.user_id) !== userId) throw new ForbiddenException();

    Object.assign(record, dto, { server_updated_at: new Date() });
    const saved = await this.repo.save(record);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'memorial', 'update', saved);
    return saved;
  }

  async remove(userId: number, id: number) {
    const record = await this.repo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!record) throw new NotFoundException('Memorial day not found');
    if (Number(record.user_id) !== userId) throw new ForbiddenException();

    record.deleted_at = new Date();
    record.server_updated_at = new Date();
    await this.repo.save(record);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'memorial', 'delete', { id });
    return { deleted: true };
  }
}
