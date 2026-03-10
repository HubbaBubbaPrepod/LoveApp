import {
  Injectable,
  BadRequestException,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, DataSource } from 'typeorm';
import { ShopItem } from './entities/shop-item.entity';
import { CoinTransaction } from './entities/coin-transaction.entity';
import { DailyDeal } from './entities/daily-deal.entity';
import { DailyDealPurchase } from './entities/daily-deal-purchase.entity';
import { DailyMission } from './entities/daily-mission.entity';
import { UserMissionProgress } from './entities/user-mission-progress.entity';
import { CoupleService } from '../shared/couple.service';
import { addCoins } from './helpers/add-coins.helper';

@Injectable()
export class ShopService {
  constructor(
    @InjectRepository(ShopItem)
    private readonly itemRepo: Repository<ShopItem>,
    @InjectRepository(CoinTransaction)
    private readonly txRepo: Repository<CoinTransaction>,
    @InjectRepository(DailyDeal)
    private readonly dealRepo: Repository<DailyDeal>,
    @InjectRepository(DailyDealPurchase)
    private readonly dealPurchaseRepo: Repository<DailyDealPurchase>,
    @InjectRepository(DailyMission)
    private readonly missionRepo: Repository<DailyMission>,
    @InjectRepository(UserMissionProgress)
    private readonly progressRepo: Repository<UserMissionProgress>,
    private readonly coupleService: CoupleService,
    private readonly dataSource: DataSource,
  ) {}

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  async getBalance(userId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const pet = await this.dataSource.query(
      `SELECT coins FROM pets WHERE couple_key = $1`,
      [coupleKey],
    );
    const user = await this.dataSource.query(
      `SELECT premium_coins_multiplier FROM users WHERE id = $1`,
      [userId],
    );
    return {
      coins: pet?.[0]?.coins ?? 0,
      premium_multiplier: user?.[0]?.premium_coins_multiplier ?? 1,
    };
  }

  async getItems(userId: number, category?: string) {
    const where: any = { is_available: true };
    if (category) where.category = category;
    return this.itemRepo.find({ where, order: { sort_order: 'ASC' } });
  }

  async buy(userId: number, itemId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const item = await this.itemRepo.findOne({ where: { id: itemId, is_available: true } });
    if (!item) throw new NotFoundException('Item not found');

    // Check balance
    const pet = await this.dataSource.query(
      `SELECT coins FROM pets WHERE couple_key = $1`,
      [coupleKey],
    );
    const balance = pet?.[0]?.coins ?? 0;
    if (balance < item.price_coins) {
      throw new BadRequestException('Not enough coins');
    }

    // Deduct coins
    await addCoins(
      this.dataSource, coupleKey, userId,
      -item.price_coins, 'purchase', `Bought ${item.name}`, 'shop_item', item.id,
    );

    // Apply effects
    if (item.effect_type && item.effect_amount) {
      const field = item.effect_type; // xp, energy, mood
      await this.dataSource.query(
        `UPDATE pets SET ${field} = COALESCE(${field}, 0) + $1 WHERE couple_key = $2`,
        [item.effect_amount, coupleKey],
      );
    }

    return { item, message: 'Purchase successful' };
  }

  async getDailyDeals(userId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const deals = await this.dealRepo
      .createQueryBuilder('d')
      .where('d.is_active = true')
      .andWhere('d.valid_until > NOW()')
      .orderBy('d.created_at', 'DESC')
      .getMany();

    const dealIds = deals.map((d) => d.id);
    let purchased: number[] = [];
    if (dealIds.length > 0) {
      const rows = await this.dealPurchaseRepo
        .createQueryBuilder('dp')
        .select('dp.deal_id')
        .where('dp.user_id = :userId', { userId })
        .andWhere('dp.deal_id IN (:...dealIds)', { dealIds })
        .getMany();
      purchased = rows.map((r) => r.deal_id);
    }

    return deals.map((d) => ({
      ...d,
      already_purchased: purchased.includes(d.id),
      sold_out: d.redeemed_count >= d.total_available,
    }));
  }

  async buyDeal(userId: number, dealId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);

    return this.dataSource.transaction(async (manager) => {
      const deal = await manager.findOne(DailyDeal, {
        where: { id: dealId, is_active: true },
        lock: { mode: 'pessimistic_write' },
      });
      if (!deal || deal.valid_until < new Date()) {
        throw new NotFoundException('Deal not found or expired');
      }
      if (deal.redeemed_count >= deal.total_available) {
        throw new BadRequestException('Deal sold out');
      }

      // Check already purchased
      const existing = await manager.findOne(DailyDealPurchase, {
        where: { deal_id: dealId, user_id: userId },
      });
      if (existing) throw new BadRequestException('Already purchased this deal');

      // Check balance & deduct
      const pet = await manager.query(
        `SELECT coins FROM pets WHERE couple_key = $1 FOR UPDATE`,
        [coupleKey],
      );
      const balance = pet?.[0]?.coins ?? 0;
      if (balance < deal.deal_price) {
        throw new BadRequestException('Not enough coins');
      }

      const balanceAfter = balance - deal.deal_price;
      await manager.query(`UPDATE pets SET coins = $1 WHERE couple_key = $2`, [balanceAfter, coupleKey]);
      await manager.query(
        `INSERT INTO coin_transactions (couple_key, user_id, tx_type, amount, balance_before, balance_after, ref_type, ref_id, description)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)`,
        [coupleKey, userId, 'deal_purchase', -deal.deal_price, balance, balanceAfter, 'daily_deal', deal.id, `Deal: ${deal.item_name}`],
      );

      // Record purchase & increment count
      await manager.save(DailyDealPurchase, {
        deal_id: dealId,
        couple_key: coupleKey,
        user_id: userId,
      });
      deal.redeemed_count += 1;
      await manager.save(deal);

      return { deal, balance_after: balanceAfter };
    });
  }

  async getMissions(userId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const today = this.today();

    const missions = await this.missionRepo.find({
      where: { is_active: true },
      order: { sort_order: 'ASC' },
    });

    // Upsert missing progress rows
    for (const m of missions) {
      await this.progressRepo
        .createQueryBuilder()
        .insert()
        .into(UserMissionProgress)
        .values({
          couple_key: coupleKey,
          user_id: userId,
          mission_id: m.id,
          mission_date: today,
        })
        .orIgnore()
        .execute();
    }

    const progress = await this.progressRepo.find({
      where: { user_id: userId, mission_date: today },
    });

    const progressMap = new Map(progress.map((p) => [p.mission_id, p]));

    return missions.map((m) => {
      const p = progressMap.get(m.id);
      return {
        ...m,
        current_count: p?.current_count ?? 0,
        is_completed: p?.is_completed ?? false,
        is_claimed: p?.is_claimed ?? false,
      };
    });
  }

  async progressMission(userId: number, code: string, increment: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const today = this.today();

    const mission = await this.missionRepo.findOne({ where: { code, is_active: true } });
    if (!mission) throw new NotFoundException('Mission not found');

    // Upsert progress
    await this.progressRepo
      .createQueryBuilder()
      .insert()
      .into(UserMissionProgress)
      .values({
        couple_key: coupleKey,
        user_id: userId,
        mission_id: mission.id,
        mission_date: today,
      })
      .orIgnore()
      .execute();

    const progress = await this.progressRepo.findOne({
      where: { user_id: userId, mission_id: mission.id, mission_date: today },
    });

    if (!progress || progress.is_completed) return progress;

    progress.current_count = Math.min(progress.current_count + increment, mission.target_count);
    if (progress.current_count >= mission.target_count) {
      progress.is_completed = true;
      progress.completed_at = new Date();
    }

    return this.progressRepo.save(progress);
  }

  async claimMission(userId: number, missionId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const today = this.today();

    const progress = await this.progressRepo.findOne({
      where: { user_id: userId, mission_id: missionId, mission_date: today },
    });
    if (!progress) throw new NotFoundException('Mission progress not found');
    if (!progress.is_completed) throw new BadRequestException('Mission not completed');
    if (progress.is_claimed) throw new BadRequestException('Already claimed');

    const mission = await this.missionRepo.findOneBy({ id: missionId });
    if (!mission) throw new NotFoundException('Mission not found');

    // Award coins + xp
    await addCoins(
      this.dataSource, coupleKey, userId,
      mission.reward_coins, 'mission_reward', `Mission: ${mission.name}`, 'daily_mission', mission.id,
    );
    if (mission.reward_xp) {
      await this.dataSource.query(
        `UPDATE pets SET xp = COALESCE(xp, 0) + $1 WHERE couple_key = $2`,
        [mission.reward_xp, coupleKey],
      );
    }

    progress.is_claimed = true;
    progress.claimed_at = new Date();
    await this.progressRepo.save(progress);

    // Auto-complete 'complete_all' mission
    const allMissions = await this.missionRepo.find({ where: { is_active: true } });
    const completeAllMission = allMissions.find((m) => m.code === 'complete_all');
    if (completeAllMission && completeAllMission.id !== missionId) {
      const otherMissions = allMissions.filter((m) => m.code !== 'complete_all');
      const otherProgress = await this.progressRepo.find({
        where: { user_id: userId, mission_date: today },
      });
      const claimedOthers = otherProgress.filter(
        (p) => p.mission_id !== completeAllMission.id && p.is_claimed,
      );
      if (claimedOthers.length >= otherMissions.length) {
        await this.progressMission(userId, 'complete_all', completeAllMission.target_count);
      }
    }

    return progress;
  }

  async getTransactions(userId: number, query: { page?: number; limit?: number }) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const page = Math.max(query.page || 1, 1);
    const limit = Math.min(Math.max(query.limit || 20, 1), 200);
    const offset = (page - 1) * limit;

    const [items, total] = await this.txRepo.findAndCount({
      where: { couple_key: coupleKey },
      order: { created_at: 'DESC' },
      skip: offset,
      take: limit,
    });

    return { items, total, page, limit };
  }

  async getSummary(userId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const today = this.today();

    const [todayStats] = await this.dataSource.query(
      `SELECT
         COALESCE(SUM(CASE WHEN amount > 0 THEN amount ELSE 0 END), 0) AS today_earned,
         COALESCE(SUM(CASE WHEN amount < 0 THEN ABS(amount) ELSE 0 END), 0) AS today_spent
       FROM coin_transactions
       WHERE couple_key = $1 AND created_at::date = $2`,
      [coupleKey, today],
    );

    const [totalStats] = await this.dataSource.query(
      `SELECT COALESCE(SUM(CASE WHEN amount > 0 THEN amount ELSE 0 END), 0) AS total_earned
       FROM coin_transactions WHERE couple_key = $1`,
      [coupleKey],
    );

    const missionsCompleted = await this.progressRepo.count({
      where: { user_id: userId, mission_date: today, is_claimed: true },
    });

    const dealsCount = await this.dealPurchaseRepo.count({
      where: { user_id: userId },
    });

    return {
      today_earned: Number(todayStats?.today_earned ?? 0),
      today_spent: Number(todayStats?.today_spent ?? 0),
      total_earned: Number(totalStats?.total_earned ?? 0),
      missions_completed: missionsCompleted,
      deals_purchased: dealsCount,
    };
  }
}
