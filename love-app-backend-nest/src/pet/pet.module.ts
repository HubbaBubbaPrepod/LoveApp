import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { PetController } from './pet.controller';
import { PetService } from './pet.service';
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
import { SharedModule } from '../shared/shared.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      Pet,
      PetType,
      PetEgg,
      PetFurniture,
      PetOwnedFurniture,
      PetAdventure,
      PetActiveAdventure,
      PetWish,
      PetCollection,
      PetSpinHistory,
      PetLevelReward,
      PetAction,
    ]),
    SharedModule,
  ],
  controllers: [PetController],
  providers: [PetService],
  exports: [PetService],
})
export class PetModule {}
