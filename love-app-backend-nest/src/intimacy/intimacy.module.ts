import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { IntimacyScore } from '../questions/entities/intimacy-score.entity';
import { IntimacyLog } from '../questions/entities/intimacy-log.entity';
import { IntimacyController } from './intimacy.controller';
import { IntimacyService } from './intimacy.service';

@Module({
  imports: [
    TypeOrmModule.forFeature([IntimacyScore, IntimacyLog]),
  ],
  controllers: [IntimacyController],
  providers: [IntimacyService],
  exports: [IntimacyService],
})
export class IntimacyModule {}
