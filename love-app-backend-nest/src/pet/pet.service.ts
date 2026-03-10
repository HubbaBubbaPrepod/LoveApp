import {
  Injectable,
  BadRequestException,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, DataSource } from 'typeorm';
import { CoupleService } from '../shared/couple.service';
import { Pet } from './entities/pet.entity';
import { PetType } from './entities/pet-type.entity';
import { PetEgg } from './entities/pet-egg.entity';
import { PetFurniture } from './entities/pet-furniture.entity';
import { PetOwnedFurniture } from './entities/pet-owned-furniture.entity';
import { PetAdventure } from './entities/pet-adventure.entity';
import { PetActiveAdventure } from './entities/pet-active-adventure.entity';
import { PetWish } from './entities/pet-wish.entity';
import { PetCollection } from './entities/pet-collection.entity';
import { PetSpinHistory } from './entities/pet-spin-history.entity';
import { PetLevelReward } from './entities/pet-level-reward.entity';
import { PetAction } from './entities/pet-action.entity';
import { UpdatePetDto } from './dto/update-pet.dto';
import { PlaceFurnitureDto } from './dto/place-furniture.dto';
import { CreateWishDto } from './dto/create-wish.dto';
import {
  MAX_STAT,
  DECAY_INTERVAL_MS,
  DECAY_AMOUNT,
  CARE_COOLDOWN_MS,
  ENERGY_REGEN_PER_HOUR,
  MAX_ENERGY,
  MAX_LEVEL,
  xpForLevel,
  calcLevel,
  computeMood,
} from './pet.constants';

@Injectable()
export class PetService {
  constructor(
    @InjectRepository(Pet) private petRepo: Repository<Pet>,
    @InjectRepository(PetType) private petTypeRepo: Repository<PetType>,
    @InjectRepository(PetEgg) private petEggRepo: Repository<PetEgg>,
    @InjectRepository(PetFurniture) private furnitureRepo: Repository<PetFurniture>,
    @InjectRepository(PetOwnedFurniture) private ownedFurnitureRepo: Repository<PetOwnedFurniture>,
    @InjectRepository(PetAdventure) private adventureRepo: Repository<PetAdventure>,
    @InjectRepository(PetActiveAdventure) private activeAdventureRepo: Repository<PetActiveAdventure>,
    @InjectRepository(PetWish) private wishRepo: Repository<PetWish>,
    @InjectRepository(PetCollection) private collectionRepo: Repository<PetCollection>,
    @InjectRepository(PetSpinHistory) private spinHistoryRepo: Repository<PetSpinHistory>,
    @InjectRepository(PetLevelReward) private levelRewardRepo: Repository<PetLevelReward>,
    @InjectRepository(PetAction) private actionRepo: Repository<PetAction>,
    private coupleService: CoupleService,
    private dataSource: DataSource,
  ) {}

  // ─── helpers ────────────────────────────────────────────

  private clamp(val: number, min = 0, max = MAX_STAT): number {
    return Math.max(min, Math.min(max, val));
  }

  private applyDecay(pet: Pet): Pet {
    if (!pet.last_decay) {
      pet.last_decay = new Date();
      return pet;
    }
    const now = Date.now();
    const elapsed = now - new Date(pet.last_decay).getTime();
    const intervals = Math.floor(elapsed / DECAY_INTERVAL_MS);
    if (intervals > 0) {
      pet.hunger = this.clamp(pet.hunger - DECAY_AMOUNT * intervals);
      pet.happiness = this.clamp(pet.happiness - DECAY_AMOUNT * intervals);
      pet.cleanliness = this.clamp(pet.cleanliness - DECAY_AMOUNT * intervals);
      const hoursElapsed = (intervals * DECAY_INTERVAL_MS) / (60 * 60 * 1000);
      pet.energy = this.clamp(pet.energy + Math.floor(hoursElapsed * ENERGY_REGEN_PER_HOUR));
      pet.last_decay = new Date(new Date(pet.last_decay).getTime() + intervals * DECAY_INTERVAL_MS);
      pet.mood = computeMood(pet.hunger, pet.happiness, pet.cleanliness);
    }
    return pet;
  }

  private enrichPet(pet: Pet) {
    const { level, remainingXp } = calcLevel(pet.xp);
    const xpNeeded = xpForLevel(level);
    return {
      ...pet,
      level,
      xp_current: remainingXp,
      xp_needed: xpNeeded,
      xp_progress: xpNeeded > 0 ? remainingXp / xpNeeded : 1,
    };
  }

  private async getCoupleKey(userId: number): Promise<string> {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    if (!coupleKey) throw new BadRequestException('User is not in a couple');
    return coupleKey;
  }

  async ensurePet(coupleKey: string): Promise<Pet> {
    let pet = await this.petRepo.findOne({ where: { couple_key: coupleKey } });
    if (!pet) {
      pet = this.petRepo.create({ couple_key: coupleKey, last_decay: new Date() });
      pet = await this.petRepo.save(pet);
    }
    return pet;
  }

  private async checkLevelUpRewards(pet: Pet): Promise<any[]> {
    const { level: newLevel } = calcLevel(pet.xp);
    const { level: oldLevel } = calcLevel(pet.xp - 15); // rough check
    const rewards: any[] = [];
    if (newLevel > oldLevel) {
      for (let l = oldLevel + 1; l <= newLevel; l++) {
        const reward = await this.levelRewardRepo.findOne({ where: { level: l } });
        if (reward) {
          if (reward.reward_type === 'coins') {
            pet.coins += reward.reward_amount;
          }
          rewards.push(reward);
        }
      }
    }
    return rewards;
  }

  // ─── core ───────────────────────────────────────────────

  async getPet(userId: number) {
    const coupleKey = await this.getCoupleKey(userId);
    let pet = await this.ensurePet(coupleKey);
    pet = this.applyDecay(pet);
    await this.petRepo.save(pet);
    return this.enrichPet(pet);
  }

  async updatePet(userId: number, dto: UpdatePetDto) {
    const coupleKey = await this.getCoupleKey(userId);
    const pet = await this.ensurePet(coupleKey);
    if (dto.name !== undefined) pet.name = dto.name;
    if (dto.pet_type !== undefined) pet.pet_type = dto.pet_type;
    await this.petRepo.save(pet);
    return this.enrichPet(pet);
  }

  // ─── care actions ───────────────────────────────────────

  private async careAction(
    userId: number,
    statField: 'hunger' | 'happiness' | 'cleanliness',
    cooldownField: 'last_fed' | 'last_played' | 'last_cleaned',
    actionType: string,
  ) {
    const coupleKey = await this.getCoupleKey(userId);

    return this.dataSource.transaction(async (manager) => {
      const pet = await manager
        .createQueryBuilder(Pet, 'pet')
        .setLock('pessimistic_write')
        .where('pet.couple_key = :coupleKey', { coupleKey })
        .getOne();

      if (!pet) throw new NotFoundException('Pet not found');

      const now = new Date();
      if (pet[cooldownField]) {
        const elapsed = now.getTime() - new Date(pet[cooldownField]).getTime();
        if (elapsed < CARE_COOLDOWN_MS) {
          const remainingMs = CARE_COOLDOWN_MS - elapsed;
          throw new BadRequestException(
            `Please wait ${Math.ceil(remainingMs / 60000)} minutes before doing this again`,
          );
        }
      }

      pet[statField] = this.clamp(pet[statField] + 20);
      pet[cooldownField] = now;
      pet.xp += 15;
      pet.mood = computeMood(pet.hunger, pet.happiness, pet.cleanliness);

      const rewards = await this.checkLevelUpRewards(pet);
      await manager.save(Pet, pet);

      await manager.save(PetAction, manager.create(PetAction, {
        couple_key: coupleKey,
        user_id: userId,
        action_type: actionType,
        xp_gained: 15,
      }));

      return { pet: this.enrichPet(pet), rewards };
    });
  }

  async feed(userId: number) {
    return this.careAction(userId, 'hunger', 'last_fed', 'feed');
  }

  async play(userId: number) {
    return this.careAction(userId, 'happiness', 'last_played', 'play');
  }

  async clean(userId: number) {
    return this.careAction(userId, 'cleanliness', 'last_cleaned', 'clean');
  }

  // ─── pet types ──────────────────────────────────────────

  async getTypes() {
    return this.petTypeRepo.find({ order: { sort_order: 'ASC' } });
  }

  // ─── eggs ───────────────────────────────────────────────

  async getEggs(userId: number) {
    const coupleKey = await this.getCoupleKey(userId);
    return this.petEggRepo.find({ where: { couple_key: coupleKey }, order: { created_at: 'DESC' } });
  }

  async hatchEgg(userId: number, eggId: number) {
    const coupleKey = await this.getCoupleKey(userId);
    const egg = await this.petEggRepo.findOne({ where: { id: eggId, couple_key: coupleKey } });
    if (!egg) throw new NotFoundException('Egg not found');
    if (egg.is_hatched) throw new BadRequestException('Egg already hatched');

    const existing = await this.collectionRepo.findOne({
      where: { couple_key: coupleKey, pet_type_id: egg.pet_type_id },
    });

    return this.dataSource.transaction(async (manager) => {
      const eggRepo = manager.getRepository(PetEgg);
      egg.is_hatched = true;
      egg.hatched_at = new Date();

      if (existing) {
        const pet = await this.ensurePet(coupleKey);
        pet.coins += 50;
        await manager.getRepository(Pet).save(pet);
        await eggRepo.save(egg);
        return { duplicate: true, coins_awarded: 50 };
      }

      await manager.getRepository(PetCollection).save(
        manager.getRepository(PetCollection).create({ couple_key: coupleKey, pet_type_id: egg.pet_type_id }),
      );
      await eggRepo.save(egg);
      return { duplicate: false, pet_type_id: egg.pet_type_id };
    });
  }

  // ─── furniture ──────────────────────────────────────────

  async getShop() {
    return this.furnitureRepo.find({
      where: { is_available: true },
      order: { sort_order: 'ASC' },
    });
  }

  async getOwnedFurniture(userId: number) {
    const coupleKey = await this.getCoupleKey(userId);
    return this.ownedFurnitureRepo.find({ where: { couple_key: coupleKey } });
  }

  async buyFurniture(userId: number, furnitureId: number) {
    const coupleKey = await this.getCoupleKey(userId);
    const furniture = await this.furnitureRepo.findOne({ where: { id: furnitureId } });
    if (!furniture) throw new NotFoundException('Furniture not found');

    return this.dataSource.transaction(async (manager) => {
      const pet = await manager
        .createQueryBuilder(Pet, 'pet')
        .setLock('pessimistic_write')
        .where('pet.couple_key = :coupleKey', { coupleKey })
        .getOne();

      if (!pet) throw new NotFoundException('Pet not found');
      if (pet.coins < furniture.price_coins) {
        throw new BadRequestException('Not enough coins');
      }

      pet.coins -= furniture.price_coins;
      await manager.save(Pet, pet);

      const owned = manager.create(PetOwnedFurniture, {
        couple_key: coupleKey,
        furniture_id: furnitureId,
      });
      await manager.save(PetOwnedFurniture, owned);

      return owned;
    });
  }

  async placeFurniture(userId: number, ownedId: number, dto: PlaceFurnitureDto) {
    const coupleKey = await this.getCoupleKey(userId);
    const owned = await this.ownedFurnitureRepo.findOne({
      where: { id: ownedId, couple_key: coupleKey },
    });
    if (!owned) throw new NotFoundException('Owned furniture not found');

    owned.is_placed = true;
    owned.position_x = dto.position_x;
    owned.position_y = dto.position_y;
    return this.ownedFurnitureRepo.save(owned);
  }

  // ─── adventures ─────────────────────────────────────────

  async getAdventures() {
    return this.adventureRepo.find({
      where: { is_active: true },
      order: { sort_order: 'ASC' },
    });
  }

  async startAdventure(userId: number, adventureId: number) {
    const coupleKey = await this.getCoupleKey(userId);
    const adventure = await this.adventureRepo.findOne({ where: { id: adventureId, is_active: true } });
    if (!adventure) throw new NotFoundException('Adventure not found');

    return this.dataSource.transaction(async (manager) => {
      const pet = await manager
        .createQueryBuilder(Pet, 'pet')
        .setLock('pessimistic_write')
        .where('pet.couple_key = :coupleKey', { coupleKey })
        .getOne();

      if (!pet) throw new NotFoundException('Pet not found');
      if (pet.energy < adventure.energy_cost) {
        throw new BadRequestException('Not enough energy');
      }

      const active = await this.activeAdventureRepo.findOne({
        where: { couple_key: coupleKey, is_claimed: false },
      });
      if (active) throw new BadRequestException('Already on an adventure');

      pet.energy -= adventure.energy_cost;
      await manager.save(Pet, pet);

      const now = new Date();
      const endsAt = new Date(now.getTime() + adventure.duration_minutes * 60 * 1000);
      const activeAdventure = manager.create(PetActiveAdventure, {
        couple_key: coupleKey,
        adventure_id: adventureId,
        started_at: now,
        ends_at: endsAt,
      });
      return manager.save(PetActiveAdventure, activeAdventure);
    });
  }

  async claimAdventure(userId: number, activeId: number) {
    const coupleKey = await this.getCoupleKey(userId);

    return this.dataSource.transaction(async (manager) => {
      const active = await manager.findOne(PetActiveAdventure, {
        where: { id: activeId, couple_key: coupleKey },
      });
      if (!active) throw new NotFoundException('Active adventure not found');
      if (active.is_claimed) throw new BadRequestException('Already claimed');
      if (new Date() < new Date(active.ends_at)) {
        throw new BadRequestException('Adventure not finished yet');
      }

      const adventure = await this.adventureRepo.findOneOrFail({ where: { id: active.adventure_id } });
      const rewardCoins =
        Math.floor(Math.random() * (adventure.coin_reward_max - adventure.coin_reward_min + 1)) +
        adventure.coin_reward_min;
      const rewardXp = adventure.xp_reward;

      active.is_claimed = true;
      active.reward_coins = rewardCoins;
      active.reward_xp = rewardXp;
      await manager.save(PetActiveAdventure, active);

      const pet = await manager
        .createQueryBuilder(Pet, 'pet')
        .setLock('pessimistic_write')
        .where('pet.couple_key = :coupleKey', { coupleKey })
        .getOne();
      if (!pet) throw new NotFoundException('Pet not found');

      pet.coins += rewardCoins;
      pet.xp += rewardXp;
      pet.mood = computeMood(pet.hunger, pet.happiness, pet.cleanliness);
      await manager.save(Pet, pet);

      return { reward_coins: rewardCoins, reward_xp: rewardXp, pet: this.enrichPet(pet) };
    });
  }

  // ─── wishes ─────────────────────────────────────────────

  async getWishes(userId: number) {
    const coupleKey = await this.getCoupleKey(userId);
    return this.wishRepo.find({ where: { couple_key: coupleKey }, order: { created_at: 'DESC' } });
  }

  async createWish(userId: number, dto: CreateWishDto) {
    const coupleKey = await this.getCoupleKey(userId);
    const wish = this.wishRepo.create({
      couple_key: coupleKey,
      user_id: userId,
      content: dto.content,
    });
    return this.wishRepo.save(wish);
  }

  async deleteWish(userId: number, wishId: number) {
    const coupleKey = await this.getCoupleKey(userId);
    const wish = await this.wishRepo.findOne({ where: { id: wishId, couple_key: coupleKey } });
    if (!wish) throw new NotFoundException('Wish not found');
    await this.wishRepo.remove(wish);
    return { deleted: true };
  }

  async fulfillWish(userId: number, wishId: number) {
    const coupleKey = await this.getCoupleKey(userId);
    const wish = await this.wishRepo.findOne({ where: { id: wishId, couple_key: coupleKey } });
    if (!wish) throw new NotFoundException('Wish not found');
    if (wish.is_fulfilled) throw new BadRequestException('Wish already fulfilled');

    wish.is_fulfilled = true;
    wish.fulfilled_at = new Date();
    await this.wishRepo.save(wish);

    const pet = await this.ensurePet(coupleKey);
    pet.xp += wish.xp_reward;
    pet.mood = computeMood(pet.hunger, pet.happiness, pet.cleanliness);
    await this.petRepo.save(pet);

    return { wish, xp_gained: wish.xp_reward };
  }

  // ─── daily check-in ────────────────────────────────────

  async checkin(userId: number) {
    const coupleKey = await this.getCoupleKey(userId);
    const pet = await this.ensurePet(coupleKey);

    const today = new Date().toISOString().slice(0, 10);
    if (pet.last_checkin === today) {
      throw new BadRequestException('Already checked in today');
    }

    const yesterday = new Date(Date.now() - 86400000).toISOString().slice(0, 10);
    const isConsecutive = pet.last_checkin === yesterday;
    pet.streak_days = isConsecutive ? pet.streak_days + 1 : 1;
    pet.last_checkin = today;

    const xpBonus = 10 + Math.min(pet.streak_days, 7) * 5;
    pet.xp += xpBonus;
    pet.mood = computeMood(pet.hunger, pet.happiness, pet.cleanliness);
    await this.petRepo.save(pet);

    return {
      streak_days: pet.streak_days,
      xp_gained: xpBonus,
      pet: this.enrichPet(pet),
    };
  }

  // ─── GET check-in status ────────────────────────────────

  async getCheckinStatus(userId: number) {
    const coupleKey = await this.getCoupleKey(userId);
    const pet = await this.ensurePet(coupleKey);
    const today = new Date().toISOString().slice(0, 10);
    return {
      checked_in_today: pet.last_checkin === today,
      streak: pet.checkin_streak || pet.streak_days,
      total_checkins: pet.total_checkins || 0,
      last_checkin: pet.last_checkin,
    };
  }

  // ─── daily spin ─────────────────────────────────────────

  async spin(userId: number) {
    const coupleKey = await this.getCoupleKey(userId);
    const today = new Date().toISOString().slice(0, 10);

    const already = await this.spinHistoryRepo
      .createQueryBuilder('s')
      .where('s.couple_key = :coupleKey', { coupleKey })
      .andWhere('s.user_id = :userId', { userId })
      .andWhere('DATE(s.spun_at) = :today', { today })
      .getOne();
    if (already) throw new BadRequestException('Already spun today');

    const rewards = [
      { type: 'coins', amount: 5, weight: 30, detail: '5 монет' },
      { type: 'coins', amount: 10, weight: 25, detail: '10 монет' },
      { type: 'coins', amount: 25, weight: 15, detail: '25 монет' },
      { type: 'coins', amount: 50, weight: 8, detail: '50 монет' },
      { type: 'xp', amount: 10, weight: 20, detail: '10 XP' },
      { type: 'xp', amount: 25, weight: 10, detail: '25 XP' },
      { type: 'energy', amount: 30, weight: 12, detail: '30 энергии' },
      { type: 'egg_common', amount: 1, weight: 8, detail: 'Обычное яйцо!' },
      { type: 'egg_uncommon', amount: 1, weight: 4, detail: 'Необычное яйцо!' },
      { type: 'egg_rare', amount: 1, weight: 2, detail: 'Редкое яйцо!' },
      { type: 'coins', amount: 100, weight: 1, detail: '100 монет! Джекпот!' },
    ];

    const totalWeight = rewards.reduce((s, r) => s + r.weight, 0);
    let roll = Math.random() * totalWeight;
    let reward = rewards[0];
    for (const r of rewards) {
      roll -= r.weight;
      if (roll <= 0) { reward = r; break; }
    }

    return this.dataSource.transaction(async (manager) => {
      const pet = await manager
        .createQueryBuilder(Pet, 'pet')
        .setLock('pessimistic_write')
        .where('pet.couple_key = :coupleKey', { coupleKey })
        .getOne();
      if (!pet) throw new NotFoundException('Pet not found');

      if (reward.type === 'coins') {
        pet.coins += reward.amount;
      } else if (reward.type === 'xp') {
        pet.xp += reward.amount;
      } else if (reward.type === 'energy') {
        pet.energy = Math.min(MAX_ENERGY, pet.energy + reward.amount);
      } else if (reward.type.startsWith('egg_')) {
        const rarity = reward.type.replace('egg_', '');
        const types = await manager
          .createQueryBuilder(PetType, 't')
          .where('t.rarity = :rarity', { rarity })
          .orderBy('RANDOM()')
          .limit(1)
          .getOne();
        const petTypeCode = types?.code as string ?? 'cat';
        await manager
          .createQueryBuilder()
          .insert()
          .into(PetEgg)
          .values({
            couple_key: coupleKey,
            user_id: userId,
            pet_type_code: petTypeCode,
            rarity,
            source: 'spin',
          } as any)
          .execute();
      }

      await manager.save(Pet, pet);
      await manager.save(PetSpinHistory, manager.create(PetSpinHistory, {
        couple_key: coupleKey,
        user_id: userId,
        reward_type: reward.type,
        reward_amount: reward.amount,
        reward_detail: reward.detail,
      }));

      return {
        reward_type: reward.type,
        reward_amount: reward.amount,
        reward_detail: reward.detail,
      };
    });
  }

  // ─── collections ────────────────────────────────────────

  async getCollections(userId: number) {
    const coupleKey = await this.getCoupleKey(userId);
    const items = await this.collectionRepo.find({
      where: { couple_key: coupleKey },
      order: { collection_type: 'ASC', unlocked_at: 'ASC' },
    });

    const grouped: Record<string, any[]> = {};
    for (const item of items) {
      if (!grouped[item.collection_type]) grouped[item.collection_type] = [];
      grouped[item.collection_type].push(item);
    }

    return { collections: grouped, total: items.length };
  }

  // ─── passport ───────────────────────────────────────────

  async getPassport(userId: number) {
    const coupleKey = await this.getCoupleKey(userId);
    const pet = await this.ensurePet(coupleKey);

    const [collCounts, eggStats, adventureCount, wishStats, furnitureCount] =
      await Promise.all([
        this.collectionRepo
          .createQueryBuilder('c')
          .select('c.collection_type', 'collection_type')
          .addSelect('COUNT(*)', 'count')
          .where('c.couple_key = :coupleKey', { coupleKey })
          .groupBy('c.collection_type')
          .getRawMany(),
        this.petEggRepo
          .createQueryBuilder('e')
          .select('COUNT(*)', 'total')
          .addSelect("COUNT(*) FILTER (WHERE e.is_hatched = true)", 'hatched')
          .where('e.couple_key = :coupleKey', { coupleKey })
          .getRawOne(),
        this.activeAdventureRepo
          .createQueryBuilder('a')
          .where('a.couple_key = :coupleKey AND a.is_claimed = true', { coupleKey })
          .getCount(),
        this.wishRepo
          .createQueryBuilder('w')
          .select('COUNT(*)', 'total')
          .addSelect("COUNT(*) FILTER (WHERE w.is_fulfilled = true)", 'fulfilled')
          .where('w.couple_key = :coupleKey', { coupleKey })
          .getRawOne(),
        this.ownedFurnitureRepo.count({ where: { couple_key: coupleKey } }),
      ]);

    const [totalTypes, totalFurniture, totalAdventures] = await Promise.all([
      this.petTypeRepo.count(),
      this.furnitureRepo.count(),
      this.adventureRepo.count(),
    ]);

    const collMap: Record<string, number> = {};
    for (const c of collCounts) collMap[c.collection_type] = +c.count;

    return {
      pet: this.enrichPet(pet),
      stats: {
        total_checkins: pet.total_checkins || 0,
        checkin_streak: pet.checkin_streak || pet.streak_days,
        adventure_count: pet.adventure_count || 0,
        coins: pet.coins,
      },
      collections: {
        pets: { unlocked: collMap['pet'] || 0, total: totalTypes },
        furniture: { owned: furnitureCount, total: totalFurniture },
        adventures: { completed: adventureCount, total: totalAdventures },
      },
      eggs: {
        total: +(eggStats?.total ?? 0),
        hatched: +(eggStats?.hatched ?? 0),
      },
      wishes: {
        total: +(wishStats?.total ?? 0),
        fulfilled: +(wishStats?.fulfilled ?? 0),
      },
    };
  }

  // ─── action history ─────────────────────────────────────

  async getHistory(userId: number, limit = 20) {
    const coupleKey = await this.getCoupleKey(userId);
    const take = Math.min(50, Math.max(1, limit));
    const items = await this.actionRepo
      .createQueryBuilder('a')
      .leftJoin('users', 'u', 'u.id = a.user_id')
      .addSelect('u.display_name', 'display_name')
      .where('a.couple_key = :coupleKey', { coupleKey })
      .orderBy('a.created_at', 'DESC')
      .limit(take)
      .getRawAndEntities();

    return { items: items.raw };
  }

  // ─── level rewards ──────────────────────────────────────

  async getLevelRewards() {
    return {
      items: await this.levelRewardRepo.find({ order: { level: 'ASC' } }),
    };
  }
}
