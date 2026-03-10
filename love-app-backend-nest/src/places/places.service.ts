import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, IsNull } from 'typeorm';
import { CommonPlace } from './entities/common-place.entity';
import { CreatePlaceDto } from './dto/create-place.dto';
import { UpdatePlaceDto } from './dto/update-place.dto';
import { CoupleService } from '../shared/couple.service';

@Injectable()
export class PlacesService {
  constructor(
    @InjectRepository(CommonPlace)
    private readonly placeRepo: Repository<CommonPlace>,
    private readonly coupleService: CoupleService,
  ) {}

  async create(userId: number, dto: CreatePlaceDto) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const place = this.placeRepo.create({
      ...dto,
      category: dto.category || 'other',
      user_id: userId,
      couple_key: coupleKey,
    });
    const saved = await this.placeRepo.save(place);

    await this.coupleService.broadcastChange(coupleKey, userId, 'common_place', 'create', saved);

    return saved;
  }

  async findAll(
    userId: number,
    query: { page?: number; limit?: number; category?: string },
  ) {
    const page = Math.max(query.page || 1, 1);
    const limit = Math.min(Math.max(query.limit || 20, 1), 50);
    const offset = (page - 1) * limit;
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const qb = this.placeRepo
      .createQueryBuilder('p')
      .where('p.couple_key = :coupleKey', { coupleKey })
      .andWhere('p.deleted_at IS NULL');

    if (query.category) {
      qb.andWhere('p.category = :category', { category: query.category });
    }

    const [items, total] = await qb
      .orderBy('p.created_at', 'DESC')
      .skip(offset)
      .take(limit)
      .getManyAndCount();

    return { items, total, page, limit };
  }

  async update(userId: number, id: number, dto: UpdatePlaceDto) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const place = await this.placeRepo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!place) throw new NotFoundException('Place not found');
    if (place.couple_key !== coupleKey) throw new ForbiddenException('Not your couple\'s place');

    Object.assign(place, dto);
    if (dto.category) place.category = dto.category;

    const saved = await this.placeRepo.save(place);

    await this.coupleService.broadcastChange(coupleKey, userId, 'common_place', 'update', saved);

    return saved;
  }

  async remove(userId: number, id: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const place = await this.placeRepo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!place) throw new NotFoundException('Place not found');
    if (place.couple_key !== coupleKey) throw new ForbiddenException('Not your couple\'s place');

    place.deleted_at = new Date();
    await this.placeRepo.save(place);
    return { deleted: true };
  }
}
