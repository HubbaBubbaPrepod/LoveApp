import { Global, Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { CoupleService } from './couple.service';
import { FcmService } from './fcm.service';
import { RelationshipInfo } from '../relationship/entities/relationship-info.entity';
import { FcmToken } from '../auth/entities/fcm-token.entity';
import { User } from '../users/entities/user.entity';
import { FirebaseModule } from '../firebase/firebase.module';

@Global()
@Module({
  imports: [
    TypeOrmModule.forFeature([RelationshipInfo, FcmToken, User]),
    FirebaseModule,
  ],
  providers: [CoupleService, FcmService],
  exports: [CoupleService, FcmService],
})
export class SharedModule {}
