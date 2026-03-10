import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, IsNull } from 'typeorm';
import { BucketListItem } from './entities/bucket-list-item.entity';
import { CreateBucketItemDto } from './dto/create-bucket-item.dto';
import { UpdateBucketItemDto } from './dto/update-bucket-item.dto';
import { CoupleService } from '../shared/couple.service';

@Injectable()
export class BucketlistService {
  constructor(
    @InjectRepository(BucketListItem)
    private readonly itemRepo: Repository<BucketListItem>,
    private readonly coupleService: CoupleService,
  ) {}

  async create(userId: number, dto: CreateBucketItemDto) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const item = this.itemRepo.create({
      ...dto,
      user_id: userId,
      couple_key: coupleKey,
    });
    const saved = await this.itemRepo.save(item);

    await this.coupleService.broadcastChange(
      coupleKey, userId, 'bucketlist', 'create', saved,
    );

    return saved;
  }

  async findAll(
    userId: number,
    query: { category?: string; is_completed?: string },
  ) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const qb = this.itemRepo
      .createQueryBuilder('b')
      .where('b.couple_key = :coupleKey', { coupleKey })
      .andWhere('b.deleted_at IS NULL');

    if (query.category) {
      qb.andWhere('b.category = :category', { category: query.category });
    }

    if (query.is_completed !== undefined) {
      const completed = query.is_completed === 'true';
      qb.andWhere('b.is_completed = :completed', { completed });
    }

    qb.orderBy('b.created_at', 'DESC');

    const items = await qb.getMany();

    // Stats
    const allItems = await this.itemRepo.find({
      where: { couple_key: coupleKey, deleted_at: IsNull() },
    });

    const total = allItems.length;
    const completed = allItems.filter((i) => i.is_completed).length;
    const categories = [...new Set(allItems.map((i) => i.category))];

    return { items, stats: { total, completed, categories } };
  }

  async update(userId: number, id: number, dto: UpdateBucketItemDto) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const item = await this.itemRepo.findOne({
      where: { id, couple_key: coupleKey, deleted_at: IsNull() },
    });
    if (!item) throw new NotFoundException('Bucket list item not found');

    Object.assign(item, dto);
    item.server_updated_at = new Date();
    const saved = await this.itemRepo.save(item);

    await this.coupleService.broadcastChange(
      coupleKey, userId, 'bucketlist', 'update', saved,
    );

    return saved;
  }

  async complete(userId: number, id: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const item = await this.itemRepo.findOne({
      where: { id, couple_key: coupleKey, deleted_at: IsNull() },
    });
    if (!item) throw new NotFoundException('Bucket list item not found');

    item.is_completed = true;
    item.completed_by = userId;
    item.completed_at = new Date();
    item.server_updated_at = new Date();
    const saved = await this.itemRepo.save(item);

    await this.coupleService.broadcastChange(
      coupleKey, userId, 'bucketlist', 'complete', saved,
    );

    return saved;
  }

  async uncomplete(userId: number, id: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const item = await this.itemRepo.findOne({
      where: { id, couple_key: coupleKey, deleted_at: IsNull() },
    });
    if (!item) throw new NotFoundException('Bucket list item not found');

    item.is_completed = false;
    item.completed_by = null;
    item.completed_at = null;
    item.server_updated_at = new Date();
    const saved = await this.itemRepo.save(item);

    await this.coupleService.broadcastChange(
      coupleKey, userId, 'bucketlist', 'uncomplete', saved,
    );

    return saved;
  }

  async remove(userId: number, id: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const item = await this.itemRepo.findOne({
      where: { id, couple_key: coupleKey, deleted_at: IsNull() },
    });
    if (!item) throw new NotFoundException('Bucket list item not found');

    item.deleted_at = new Date();
    item.server_updated_at = new Date();
    await this.itemRepo.save(item);

    await this.coupleService.broadcastChange(
      coupleKey, userId, 'bucketlist', 'delete', { id },
    );

    return { id };
  }
}
