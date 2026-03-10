import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, IsNull } from 'typeorm';
import { LoveLetter } from './entities/love-letter.entity';
import { CreateLetterDto } from './dto/create-letter.dto';
import { CoupleService } from '../shared/couple.service';

@Injectable()
export class LettersService {
  constructor(
    @InjectRepository(LoveLetter)
    private readonly letterRepo: Repository<LoveLetter>,
    private readonly coupleService: CoupleService,
  ) {}

  async create(userId: number, dto: CreateLetterDto) {
    const partnerId = await this.coupleService.getPartnerId(userId);
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const letter = this.letterRepo.create({
      ...dto,
      sender_id: userId,
      receiver_id: partnerId,
      couple_key: coupleKey,
    });
    const saved = await this.letterRepo.save(letter);

    await this.coupleService.broadcastChange(
      coupleKey, userId, 'letter', 'create', saved,
    );

    return saved;
  }

  async findAll(
    userId: number,
    filter: 'all' | 'available' | 'sealed' = 'all',
  ) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const today = new Date().toISOString().split('T')[0];

    const qb = this.letterRepo
      .createQueryBuilder('l')
      .where('l.couple_key = :coupleKey', { coupleKey })
      .andWhere('l.deleted_at IS NULL');

    if (filter === 'available') {
      // Letters where open_date is null or <= today, or sender is the user
      qb.andWhere(
        '(l.open_date IS NULL OR l.open_date <= :today OR l.sender_id = :userId)',
        { today, userId },
      );
    } else if (filter === 'sealed') {
      // Future open_date letters where user is receiver
      qb.andWhere('l.open_date > :today', { today });
      qb.andWhere('l.receiver_id = :userId', { userId });
    }

    qb.orderBy('l.created_at', 'DESC');

    const letters = await qb.getMany();

    // Hide content of sealed letters for receiver
    return letters.map((l) => {
      if (
        l.receiver_id === userId &&
        l.open_date &&
        l.open_date > today
      ) {
        return { ...l, content: null as any, title: l.title };
      }
      return l;
    });
  }

  async findOne(userId: number, id: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    const letter = await this.letterRepo.findOne({
      where: { id, couple_key: coupleKey, deleted_at: IsNull() },
    });
    if (!letter) throw new NotFoundException('Letter not found');

    const today = new Date().toISOString().split('T')[0];

    // If receiver and open_date has passed, auto-mark as opened
    if (
      letter.receiver_id === userId &&
      (!letter.open_date || letter.open_date <= today)
    ) {
      if (!letter.is_opened) {
        letter.is_opened = true;
        letter.opened_at = new Date();
        await this.letterRepo.save(letter);
      }
    }

    // If receiver and letter is still sealed, deny content
    if (
      letter.receiver_id === userId &&
      letter.open_date &&
      letter.open_date > today
    ) {
      throw new ForbiddenException('This letter cannot be opened yet');
    }

    return letter;
  }

  async getStats(userId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const today = new Date().toISOString().split('T')[0];

    const total = await this.letterRepo.count({
      where: { couple_key: coupleKey, deleted_at: IsNull() },
    });

    const opened = await this.letterRepo.count({
      where: { couple_key: coupleKey, is_opened: true, deleted_at: IsNull() },
    });

    const sealed = await this.letterRepo
      .createQueryBuilder('l')
      .where('l.couple_key = :coupleKey', { coupleKey })
      .andWhere('l.deleted_at IS NULL')
      .andWhere('l.open_date > :today', { today })
      .getCount();

    return { total, opened, sealed };
  }

  async remove(userId: number, id: number) {
    const letter = await this.letterRepo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!letter) throw new NotFoundException('Letter not found');
    if (letter.sender_id !== userId)
      throw new ForbiddenException('Only the sender can delete this letter');

    letter.deleted_at = new Date();
    await this.letterRepo.save(letter);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(
      coupleKey, userId, 'letter', 'delete', { id },
    );

    return { id };
  }
}
