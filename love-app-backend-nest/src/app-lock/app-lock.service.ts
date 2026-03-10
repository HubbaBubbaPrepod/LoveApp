import {
  Injectable,
  NotFoundException,
  UnauthorizedException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as bcrypt from 'bcryptjs';
import { AppLockSetting } from './entities/app-lock-setting.entity';
import { SetPinDto } from './dto/set-pin.dto';
import { VerifyPinDto } from './dto/verify-pin.dto';
import { UpdateLockDto } from './dto/update-lock.dto';
import { RemoveLockDto } from './dto/remove-lock.dto';

@Injectable()
export class AppLockService {
  constructor(
    @InjectRepository(AppLockSetting)
    private readonly repo: Repository<AppLockSetting>,
  ) {}

  async getStatus(userId: number) {
    const record = await this.repo.findOne({ where: { user_id: userId } });
    return {
      is_set: !!record,
      is_biometric: record?.is_biometric ?? false,
    };
  }

  async setPin(userId: number, dto: SetPinDto) {
    const pin_hash = await bcrypt.hash(dto.pin, 10);

    await this.repo
      .createQueryBuilder()
      .insert()
      .into(AppLockSetting)
      .values({ user_id: userId, pin_hash })
      .orUpdate(['pin_hash', 'updated_at'], ['user_id'])
      .execute();

    return { success: true };
  }

  async verify(userId: number, dto: VerifyPinDto) {
    const record = await this.repo.findOne({ where: { user_id: userId } });
    if (!record) return { valid: false };

    const valid = await bcrypt.compare(dto.pin, record.pin_hash);
    return { valid };
  }

  async updateLock(userId: number, dto: UpdateLockDto) {
    const record = await this.repo.findOne({ where: { user_id: userId } });
    if (!record) throw new NotFoundException('Lock not set');

    const valid = await bcrypt.compare(dto.current_pin, record.pin_hash);
    if (!valid) throw new UnauthorizedException('Invalid current PIN');

    if (dto.new_pin) {
      record.pin_hash = await bcrypt.hash(dto.new_pin, 10);
    }
    if (dto.is_biometric !== undefined) {
      record.is_biometric = dto.is_biometric;
    }

    await this.repo.save(record);
    return { success: true };
  }

  async removeLock(userId: number, dto: RemoveLockDto) {
    const record = await this.repo.findOne({ where: { user_id: userId } });
    if (!record) throw new NotFoundException('Lock not set');

    const valid = await bcrypt.compare(dto.pin, record.pin_hash);
    if (!valid) throw new UnauthorizedException('Invalid PIN');

    await this.repo.remove(record);
    return { success: true };
  }
}
