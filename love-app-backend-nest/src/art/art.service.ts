import {
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, DataSource } from 'typeorm';
import { ArtCanvas } from './entities/art-canvas.entity';
import { CanvasStroke } from './entities/canvas-stroke.entity';
import { CreateCanvasDto } from './dto/create-canvas.dto';
import { UpdateStrokesDto } from './dto/update-strokes.dto';
import { CoupleService } from '../shared/couple.service';

@Injectable()
export class ArtService {
  constructor(
    @InjectRepository(ArtCanvas)
    private readonly canvasRepo: Repository<ArtCanvas>,
    @InjectRepository(CanvasStroke)
    private readonly strokeRepo: Repository<CanvasStroke>,
    private readonly coupleService: CoupleService,
    private readonly dataSource: DataSource,
  ) {}

  async create(userId: number, dto: CreateCanvasDto) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const canvas = this.canvasRepo.create({
      ...dto,
      couple_key: coupleKey,
    });
    return this.canvasRepo.save(canvas);
  }

  async findAll(userId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    return this.canvasRepo.find({
      where: { couple_key: coupleKey },
      order: { created_at: 'DESC' },
    });
  }

  async getStrokes(canvasId: number) {
    const stroke = await this.strokeRepo.findOne({
      where: { canvas_id: canvasId },
    });
    return stroke || { canvas_id: canvasId, strokes_data: null };
  }

  async updateStrokes(canvasId: number, dto: UpdateStrokesDto) {
    const canvas = await this.canvasRepo.findOne({ where: { id: canvasId } });
    if (!canvas) throw new NotFoundException('Canvas not found');

    let stroke = await this.strokeRepo.findOne({
      where: { canvas_id: canvasId },
    });

    if (stroke) {
      stroke.strokes_data = dto.strokes_data;
      stroke.updated_at = new Date();
    } else {
      stroke = this.strokeRepo.create({
        canvas_id: canvasId,
        strokes_data: dto.strokes_data,
      });
    }

    return this.strokeRepo.save(stroke);
  }

  async uploadThumbnail(canvasId: number, thumbnailUrl: string) {
    const canvas = await this.canvasRepo.findOne({ where: { id: canvasId } });
    if (!canvas) throw new NotFoundException('Canvas not found');

    canvas.thumbnail_url = thumbnailUrl;
    canvas.updated_at = new Date();
    return this.canvasRepo.save(canvas);
  }

  async remove(userId: number, id: number) {
    const canvas = await this.canvasRepo.findOne({ where: { id } });
    if (!canvas) throw new NotFoundException('Canvas not found');

    await this.dataSource.transaction(async (manager) => {
      await manager.getRepository(CanvasStroke).delete({ canvas_id: id });
      await manager.getRepository(ArtCanvas).remove(canvas);
    });

    return { id };
  }
}
