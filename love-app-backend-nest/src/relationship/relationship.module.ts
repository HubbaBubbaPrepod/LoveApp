import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { RelationshipInfo } from './entities/relationship-info.entity';
import { User } from '../users/entities/user.entity';
import { RelationshipController } from './relationship.controller';
import { PartnerController } from './partner.controller';
import { RelationshipService } from './relationship.service';

@Module({
  imports: [TypeOrmModule.forFeature([RelationshipInfo, User])],
  controllers: [RelationshipController, PartnerController],
  providers: [RelationshipService],
  exports: [RelationshipService],
})
export class RelationshipModule {}
