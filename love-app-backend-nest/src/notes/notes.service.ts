import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, IsNull } from 'typeorm';
import { Note } from './entities/note.entity';
import { CreateNoteDto } from './dto/create-note.dto';
import { UpdateNoteDto } from './dto/update-note.dto';
import { CoupleService } from '../shared/couple.service';
import { CoupleAwareService } from '../shared/couple-aware.service';

@Injectable()
export class NotesService extends CoupleAwareService {
  constructor(
    @InjectRepository(Note)
    private readonly noteRepo: Repository<Note>,
    coupleService: CoupleService,
  ) {
    super(coupleService);
  }

  async create(userId: number, dto: CreateNoteDto) {
    const note = this.noteRepo.create({ ...dto, user_id: userId });
    const saved = await this.noteRepo.save(note);
    await this.broadcast(userId, 'note', 'create', saved);
    return saved;
  }

  async findAll(userId: number, query: { search?: string; page?: number; limit?: number }) {
    const page = Math.max(query.page || 1, 1);
    const limit = Math.min(Math.max(query.limit || 20, 1), 50);
    const offset = (page - 1) * limit;

    const partnerId = await this.partnerId(userId);

    const qb = this.noteRepo.createQueryBuilder('n')
      .where('n.deleted_at IS NULL')
      .andWhere(
        partnerId
          ? '(n.user_id = :userId OR (n.user_id = :partnerId AND n.is_private = false))'
          : 'n.user_id = :userId',
        { userId, partnerId },
      );

    if (query.search) {
      qb.andWhere('(n.title ILIKE :search OR n.content ILIKE :search)', {
        search: `%${query.search}%`,
      });
    }

    qb.orderBy('n.created_at', 'DESC')
      .skip(offset)
      .take(limit);

    const [items, total] = await qb.getManyAndCount();
    return { items, total, page, limit };
  }

  async findOne(userId: number, id: number) {
    const note = await this.noteRepo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!note) throw new NotFoundException('Note not found');

    const pid = await this.partnerId(userId);
    if (note.user_id !== userId) {
      if (note.user_id !== pid || note.is_private) {
        throw new ForbiddenException('Access denied');
      }
    }

    return note;
  }

  async update(userId: number, id: number, dto: UpdateNoteDto) {
    const note = await this.noteRepo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!note) throw new NotFoundException('Note not found');
    if (note.user_id !== userId) throw new ForbiddenException('Not the owner');

    Object.assign(note, dto);
    note.server_updated_at = new Date();
    const saved = await this.noteRepo.save(note);
    await this.broadcast(userId, 'note', 'update', saved);
    return saved;
  }

  async remove(userId: number, id: number) {
    const note = await this.noteRepo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!note) throw new NotFoundException('Note not found');
    if (note.user_id !== userId) throw new ForbiddenException('Not the owner');

    note.deleted_at = new Date();
    note.server_updated_at = new Date();
    await this.noteRepo.save(note);
    await this.broadcast(userId, 'note', 'delete', { id });
    return { id };
  }
}
