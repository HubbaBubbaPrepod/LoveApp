import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { SleepEntry } from './entities/sleep-entry.entity';
import { SleepController } from './sleep.controller';
import { SleepService } from './sleep.service';

@Module({
  imports: [TypeOrmModule.forFeature([SleepEntry])],
  controllers: [SleepController],
  providers: [SleepService],
  exports: [SleepService],
})
export class SleepModule {}
