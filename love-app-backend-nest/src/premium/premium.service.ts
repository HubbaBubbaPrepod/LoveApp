import {
  Injectable,
  BadRequestException,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, DataSource } from 'typeorm';
import { SubscriptionPlan } from './entities/subscription-plan.entity';
import { Subscription } from './entities/subscription.entity';
import { VerifyPurchaseDto } from './dto/verify-purchase.dto';
import { RestoreDto } from './dto/restore.dto';

@Injectable()
export class PremiumService {
  constructor(
    @InjectRepository(SubscriptionPlan)
    private readonly planRepo: Repository<SubscriptionPlan>,
    @InjectRepository(Subscription)
    private readonly subRepo: Repository<Subscription>,
    private readonly dataSource: DataSource,
  ) {}

  async getPlans() {
    return this.planRepo.find({
      where: { active: true },
      order: { sort_order: 'ASC' },
    });
  }

  async getStatus(userId: number) {
    const sub = await this.subRepo
      .createQueryBuilder('s')
      .leftJoinAndSelect(SubscriptionPlan, 'p', 'p.id = s.plan_id')
      .where('s.user_id = :userId', { userId })
      .andWhere('s.status = :status', { status: 'active' })
      .andWhere('s.expires_at > NOW()')
      .orderBy('s.expires_at', 'DESC')
      .getRawOne();

    return sub ?? null;
  }

  async verify(userId: number, dto: VerifyPurchaseDto) {
    // Check duplicate purchase token
    const existing = await this.subRepo.findOne({
      where: { purchase_token: dto.purchase_token },
    });
    if (existing) throw new BadRequestException('Purchase token already used');

    const plan = await this.planRepo.findOneBy({ id: dto.plan_id });
    if (!plan) throw new NotFoundException('Plan not found');

    // Calculate expiry
    const expiresAt = new Date();
    expiresAt.setMonth(expiresAt.getMonth() + plan.duration_months);

    return this.dataSource.transaction(async (manager) => {
      // Supersede old active subscriptions
      await manager.update(
        Subscription,
        { user_id: userId, status: 'active' },
        { status: 'superseded' },
      );

      const sub = manager.create(Subscription, {
        user_id: userId,
        plan_id: plan.id,
        purchase_token: dto.purchase_token,
        order_id: dto.order_id,
        status: 'active',
        expires_at: expiresAt,
        auto_renew: plan.plan_type !== 'one_time',
      });
      const saved = await manager.save(sub);

      // Update user premium status
      await manager.query(
        `UPDATE users SET is_premium = true WHERE id = $1`,
        [userId],
      );

      return saved;
    });
  }

  async restore(userId: number, dto: RestoreDto) {
    const sub = await this.subRepo.findOne({
      where: { purchase_token: dto.purchase_token, user_id: userId },
    });
    if (!sub) throw new NotFoundException('Subscription not found');
    return sub;
  }

  async cancel(userId: number) {
    const sub = await this.subRepo.findOne({
      where: { user_id: userId, status: 'active' },
      order: { expires_at: 'DESC' },
    });
    if (!sub) throw new NotFoundException('No active subscription');

    sub.auto_renew = false;
    return this.subRepo.save(sub);
  }
}
