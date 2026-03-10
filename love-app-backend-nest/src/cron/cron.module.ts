import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Pet } from '../pet/entities/pet.entity.js';
import { CronTasksService } from './cron-tasks.service.js';

@Module({
  imports: [TypeOrmModule.forFeature([Pet])],
  providers: [CronTasksService],
})
export class CronModule {}
