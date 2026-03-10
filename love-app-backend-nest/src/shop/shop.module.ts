import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ShopItem } from './entities/shop-item.entity';
import { CoinTransaction } from './entities/coin-transaction.entity';
import { DailyDeal } from './entities/daily-deal.entity';
import { DailyDealPurchase } from './entities/daily-deal-purchase.entity';
import { DailyMission } from './entities/daily-mission.entity';
import { UserMissionProgress } from './entities/user-mission-progress.entity';
import { ShopController } from './shop.controller';
import { ShopService } from './shop.service';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      ShopItem,
      CoinTransaction,
      DailyDeal,
      DailyDealPurchase,
      DailyMission,
      UserMissionProgress,
    ]),
  ],
  controllers: [ShopController],
  providers: [ShopService],
  exports: [ShopService],
})
export class ShopModule {}
