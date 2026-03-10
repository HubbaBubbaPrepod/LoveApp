import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, IsNull } from 'typeorm';
import { StoryEntry } from './entities/story-entry.entity';
import { CreateStoryDto } from './dto/create-story.dto';
import { UpdateStoryDto } from './dto/update-story.dto';
import { CoupleService } from '../shared/couple.service';

@Injectable()
export class StoryService {
  constructor(
    @InjectRepository(StoryEntry)
    private readonly storyRepo: Repository<StoryEntry>,
    private readonly coupleService: CoupleService,
  ) {}

  async create(userId: number, dto: CreateStoryDto) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const entry = this.storyRepo.create({
      ...dto,
      couple_key: coupleKey,
      author_id: userId,
    });
    const saved = await this.storyRepo.save(entry);
    await this.coupleService.broadcastChange(coupleKey, userId, 'story', 'create', saved);
    return saved;
  }

  async findAll(userId: number, query: { page?: number; pageSize?: number }) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const page = Math.max(query.page || 1, 1);
    const pageSize = Math.min(Math.max(query.pageSize || 20, 1), 50);
    const offset = (page - 1) * pageSize;

    const qb = this.storyRepo
      .createQueryBuilder('s')
      .leftJoin('users', 'u', 'u.id = s.author_id')
      .addSelect(['u.nickname', 'u.avatar_url'])
      .where('s.couple_key = :coupleKey', { coupleKey })
      .andWhere('s.deleted_at IS NULL')
      .orderBy('s.entry_date', 'DESC')
      .addOrderBy('s.created_at', 'DESC')
      .offset(offset)
      .limit(pageSize);

    const [items, total] = await Promise.all([
      qb.getRawAndEntities().then((r) => r.raw),
      qb.getCount(),
    ]);

    return { items, total, page, pageSize };
  }

  async findOne(userId: number, id: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const entry = await this.storyRepo
      .createQueryBuilder('s')
      .leftJoin('users', 'u', 'u.id = s.author_id')
      .addSelect(['u.nickname', 'u.avatar_url'])
      .where('s.id = :id', { id })
      .andWhere('s.couple_key = :coupleKey', { coupleKey })
      .andWhere('s.deleted_at IS NULL')
      .getRawOne();

    if (!entry) throw new NotFoundException('Story entry not found');
    return entry;
  }

  async getStats(userId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const stats = await this.storyRepo
      .createQueryBuilder('s')
      .select('s.entry_type', 'entry_type')
      .addSelect('COUNT(*)', 'count')
      .where('s.couple_key = :coupleKey', { coupleKey })
      .andWhere('s.deleted_at IS NULL')
      .groupBy('s.entry_type')
      .getRawMany();

    const dates = await this.storyRepo
      .createQueryBuilder('s')
      .select('MIN(s.created_at)', 'first_entry')
      .addSelect('MAX(s.created_at)', 'last_entry')
      .where('s.couple_key = :coupleKey', { coupleKey })
      .andWhere('s.deleted_at IS NULL')
      .getRawOne();

    return { by_type: stats, ...dates };
  }

  async update(userId: number, id: number, dto: UpdateStoryDto) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const entry = await this.storyRepo.findOne({
      where: { id, couple_key: coupleKey, deleted_at: IsNull() },
    });
    if (!entry) throw new NotFoundException('Story entry not found');
    if (entry.author_id !== userId) throw new ForbiddenException('Only author can edit');

    Object.assign(entry, dto);
    const saved = await this.storyRepo.save(entry);
    await this.coupleService.broadcastChange(coupleKey, userId, 'story', 'update', saved);
    return saved;
  }

  async remove(userId: number, id: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const entry = await this.storyRepo.findOne({
      where: { id, couple_key: coupleKey, deleted_at: IsNull() },
    });
    if (!entry) throw new NotFoundException('Story entry not found');
    if (entry.author_id !== userId) throw new ForbiddenException('Only author can delete');

    entry.deleted_at = new Date();
    await this.storyRepo.save(entry);
    await this.coupleService.broadcastChange(coupleKey, userId, 'story', 'delete', { id });
    return { id };
  }
}
