import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { SubscriptionPlan } from './entities/subscription-plan.entity';
import { Subscription } from './entities/subscription.entity';
import { PremiumController } from './premium.controller';
import { PremiumService } from './premium.service';

@Module({
  imports: [TypeOrmModule.forFeature([SubscriptionPlan, Subscription])],
  controllers: [PremiumController],
  providers: [PremiumService],
  exports: [PremiumService],
})
export class PremiumModule {}
