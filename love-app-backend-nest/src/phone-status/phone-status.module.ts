import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { PhoneStatus } from './entities/phone-status.entity';
import { PhoneStatusHistory } from './entities/phone-status-history.entity';
import { PhoneStatusController } from './phone-status.controller';
import { PhoneStatusService } from './phone-status.service';

@Module({
  imports: [TypeOrmModule.forFeature([PhoneStatus, PhoneStatusHistory])],
  controllers: [PhoneStatusController],
  providers: [PhoneStatusService],
  exports: [PhoneStatusService],
})
export class PhoneStatusModule {}
