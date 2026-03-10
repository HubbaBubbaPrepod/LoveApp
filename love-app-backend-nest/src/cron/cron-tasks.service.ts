import { Injectable, Logger } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, LessThan } from 'typeorm';
import { Pet } from '../pet/entities/pet.entity.js';

@Injectable()
export class CronTasksService {
  private readonly logger = new Logger(CronTasksService.name);

  constructor(
    @InjectRepository(Pet)
    private readonly petRepo: Repository<Pet>,
  ) {}

  /** Apply hunger/happiness/cleanliness decay to all pets every hour */
  @Cron(CronExpression.EVERY_HOUR)
  async decayPetStats() {
    const DECAY_AMOUNT = 2;
    const INTERVAL_MS = 3_600_000; // 1 hour

    const pets = await this.petRepo.find();
    const modified: Pet[] = [];

    for (const pet of pets) {
      const elapsed = Date.now() - new Date(pet.last_decay).getTime();
      const intervals = Math.floor(elapsed / INTERVAL_MS);
      if (intervals <= 0) continue;

      pet.hunger = Math.max(0, pet.hunger - DECAY_AMOUNT * intervals);
      pet.happiness = Math.max(0, pet.happiness - DECAY_AMOUNT * intervals);
      pet.cleanliness = Math.max(0, pet.cleanliness - DECAY_AMOUNT * intervals);
      pet.energy = Math.min(100, pet.energy + intervals);
      pet.last_decay = new Date();
      modified.push(pet);
    }

    if (modified.length > 0) {
      await this.petRepo.save(modified);
      this.logger.log(`Pet decay applied to ${modified.length} pets`);
    }
  }

  /** Clean up expired premium subscriptions daily at 3 AM */
  @Cron('0 3 * * *')
  async cleanupExpiredSubscriptions() {
    const result = await this.petRepo.manager
      .createQueryBuilder()
      .update('premium_subscriptions')
      .set({ is_active: false })
      .where('is_active = true AND expires_at < NOW()')
      .execute();

    if (result.affected && result.affected > 0) {
      this.logger.log(`Deactivated ${result.affected} expired subscriptions`);
    }
  }

  /** Prune old phone-status history entries older than 30 days (weekly, Sunday 4 AM) */
  @Cron('0 4 * * 0')
  async pruneOldStatusHistory() {
    const cutoff = new Date();
    cutoff.setDate(cutoff.getDate() - 30);

    const result = await this.petRepo.manager
      .createQueryBuilder()
      .delete()
      .from('phone_status_history')
      .where('created_at < :cutoff', { cutoff })
      .execute();

    if (result.affected && result.affected > 0) {
      this.logger.log(`Pruned ${result.affected} old status history entries`);
    }
  }
}
