import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, IsNull } from 'typeorm';
import { Wish } from './entities/wish.entity';
import { CreateWishDto } from './dto/create-wish.dto';
import { UpdateWishDto } from './dto/update-wish.dto';
import { CoupleService } from '../shared/couple.service';

@Injectable()
export class WishesService {
  constructor(
    @InjectRepository(Wish)
    private readonly wishRepo: Repository<Wish>,
    private readonly coupleService: CoupleService,
  ) {}

  async create(userId: number, dto: CreateWishDto) {
    const wish = this.wishRepo.create({ ...dto, user_id: userId });
    const saved = await this.wishRepo.save(wish);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'wish', 'create', saved);

    return saved;
  }

  async findAll(
    userId: number,
    query: { category?: string; is_completed?: string; page?: number; limit?: number },
  ) {
    const page = Math.max(query.page || 1, 1);
    const limit = Math.min(Math.max(query.limit || 20, 1), 50);
    const offset = (page - 1) * limit;

    const partnerId = await this.coupleService.getPartnerId(userId);

    const qb = this.wishRepo.createQueryBuilder('w')
      .where('w.deleted_at IS NULL')
      .andWhere(
        partnerId
          ? '(w.user_id = :userId OR (w.user_id = :partnerId AND w.is_private = false))'
          : 'w.user_id = :userId',
        { userId, partnerId },
      );

    if (query.category) {
      qb.andWhere('w.category = :category', { category: query.category });
    }

    if (query.is_completed !== undefined) {
      qb.andWhere('w.is_completed = :isCompleted', {
        isCompleted: query.is_completed === 'true',
      });
    }

    qb.orderBy('w.created_at', 'DESC')
      .skip(offset)
      .take(limit);

    const [items, total] = await qb.getManyAndCount();
    return { items, total, page, limit };
  }

  async findOne(userId: number, id: number) {
    const wish = await this.wishRepo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!wish) throw new NotFoundException('Wish not found');

    const partnerId = await this.coupleService.getPartnerId(userId);
    if (wish.user_id !== userId) {
      if (wish.user_id !== partnerId || wish.is_private) {
        throw new ForbiddenException('Access denied');
      }
    }

    return wish;
  }

  async update(userId: number, id: number, dto: UpdateWishDto) {
    const wish = await this.wishRepo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!wish) throw new NotFoundException('Wish not found');
    if (wish.user_id !== userId) throw new ForbiddenException('Not the owner');

    Object.assign(wish, dto);
    wish.server_updated_at = new Date();
    const saved = await this.wishRepo.save(wish);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'wish', 'update', saved);

    return saved;
  }

  async complete(userId: number, id: number) {
    const wish = await this.wishRepo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!wish) throw new NotFoundException('Wish not found');
    if (wish.user_id !== userId) throw new ForbiddenException('Not the owner');

    wish.is_completed = true;
    wish.completed_at = new Date();
    wish.server_updated_at = new Date();
    const saved = await this.wishRepo.save(wish);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'wish', 'complete', saved);

    return saved;
  }

  async remove(userId: number, id: number) {
    const wish = await this.wishRepo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!wish) throw new NotFoundException('Wish not found');
    if (wish.user_id !== userId) throw new ForbiddenException('Not the owner');

    wish.deleted_at = new Date();
    wish.server_updated_at = new Date();
    await this.wishRepo.save(wish);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'wish', 'delete', { id });

    return { id };
  }
}
