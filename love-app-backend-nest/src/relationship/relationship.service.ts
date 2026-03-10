import {
  Injectable,
  BadRequestException,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, DataSource } from 'typeorm';
import * as crypto from 'crypto';
import { RelationshipInfo } from './entities/relationship-info.entity';
import { User } from '../users/entities/user.entity';
import { UpdateRelationshipDto } from './dto/relationship.dto';

@Injectable()
export class RelationshipService {
  constructor(
    @InjectRepository(RelationshipInfo)
    private readonly relRepo: Repository<RelationshipInfo>,
    @InjectRepository(User)
    private readonly userRepo: Repository<User>,
    private readonly dataSource: DataSource,
  ) {}

  async get(userId: number) {
    const rel = await this.relRepo.findOne({ where: { user_id: userId } });
    if (!rel) return { relationship: null };

    let partner: User | null = null;
    if (rel.partner_id) {
      partner = await this.userRepo.findOne({
        where: { id: rel.partner_id },
        select: ['id', 'display_name', 'profile_image', 'avatar_url', 'gender'],
      });
    }

    return { relationship: rel, partner };
  }

  async update(userId: number, dto: UpdateRelationshipDto) {
    let rel = await this.relRepo.findOne({ where: { user_id: userId } });
    if (!rel) {
      rel = this.relRepo.create({ user_id: userId });
    }

    if (dto.start_date !== undefined) rel.start_date = dto.start_date;
    if (dto.anniversary_date !== undefined)
      rel.anniversary_date = dto.anniversary_date;

    return this.relRepo.save(rel);
  }

  async generateCode(userId: number) {
    const code = crypto.randomBytes(3).toString('hex').toUpperCase();
    const expires = new Date(Date.now() + 30 * 60 * 1000);

    let rel = await this.relRepo.findOne({ where: { user_id: userId } });
    if (!rel) {
      rel = this.relRepo.create({ user_id: userId });
    }

    rel.pairing_code = code;
    rel.pairing_expires = expires;
    await this.relRepo.save(rel);

    return { code, expires_at: expires };
  }

  async link(userId: number, code: string) {
    const partner = await this.relRepo.findOne({
      where: { pairing_code: code },
    });

    if (!partner || !partner.pairing_expires) {
      throw new NotFoundException('Invalid pairing code');
    }

    if (new Date(partner.pairing_expires) < new Date()) {
      throw new BadRequestException('Pairing code expired');
    }

    if (partner.user_id === userId) {
      throw new BadRequestException('Cannot pair with yourself');
    }

    const coupleKey = `${Math.min(userId, partner.user_id)}_${Math.max(userId, partner.user_id)}`;

    const qr = this.dataSource.createQueryRunner();
    await qr.connect();
    await qr.startTransaction();

    try {
      // Update partner's record
      await qr.manager.update(RelationshipInfo, partner.id, {
        partner_id: userId,
        couple_key: coupleKey,
        pairing_code: undefined,
        pairing_expires: undefined,
      });

      // Upsert user's record
      let myRel = await qr.manager.findOne(RelationshipInfo, {
        where: { user_id: userId },
      });
      if (!myRel) {
        myRel = qr.manager.create(RelationshipInfo, { user_id: userId });
      }
      myRel.partner_id = partner.user_id;
      myRel.couple_key = coupleKey;
      await qr.manager.save(myRel);

      await qr.commitTransaction();

      return { couple_key: coupleKey, partner_id: partner.user_id };
    } catch (err) {
      await qr.rollbackTransaction();
      throw err;
    } finally {
      await qr.release();
    }
  }
}
