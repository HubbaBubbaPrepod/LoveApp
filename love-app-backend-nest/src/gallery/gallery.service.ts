import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { GalleryPhoto } from './entities/gallery-photo.entity';
import { CreatePhotoDto } from './dto/create-photo.dto';
import { CoupleService } from '../shared/couple.service';

@Injectable()
export class GalleryService {
  constructor(
    @InjectRepository(GalleryPhoto)
    private readonly photoRepo: Repository<GalleryPhoto>,
    private readonly coupleService: CoupleService,
  ) {}

  async create(userId: number, dto: CreatePhotoDto) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const photo = this.photoRepo.create({
      ...dto,
      user_id: userId,
      couple_key: coupleKey,
    });
    const saved = await this.photoRepo.save(photo);

    await this.coupleService.broadcastChange(coupleKey, userId, 'gallery_photo', 'create', saved);

    return saved;
  }

  async findAll(userId: number, query: { page?: number; limit?: number }) {
    const page = Math.max(query.page || 1, 1);
    const limit = Math.min(Math.max(query.limit || 20, 1), 50);
    const offset = (page - 1) * limit;

    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const qb = this.photoRepo.createQueryBuilder('p')
      .where('p.couple_key = :coupleKey', { coupleKey })
      .andWhere('p.deleted_at IS NULL')
      .orderBy('p.created_at', 'DESC')
      .skip(offset)
      .take(limit);

    const [items, total] = await qb.getManyAndCount();
    return { items, total, page, limit };
  }

  async updateCaption(userId: number, id: number, caption: string) {
    const photo = await this.photoRepo.findOne({ where: { id } });
    if (!photo) throw new NotFoundException('Photo not found');
    if (photo.user_id !== userId) throw new ForbiddenException('Not the owner');

    photo.caption = caption;
    photo.server_updated_at = new Date();
    const saved = await this.photoRepo.save(photo);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'gallery_photo', 'update', saved);

    return saved;
  }

  async remove(userId: number, id: number) {
    const photo = await this.photoRepo.findOne({ where: { id } });
    if (!photo) throw new NotFoundException('Photo not found');
    if (photo.user_id !== userId) throw new ForbiddenException('Not the owner');

    photo.deleted_at = new Date();
    photo.server_updated_at = new Date();
    await this.photoRepo.save(photo);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'gallery_photo', 'delete', { id });

    return { id };
  }
}
