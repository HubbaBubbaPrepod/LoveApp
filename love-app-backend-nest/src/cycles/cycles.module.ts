import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { MenstrualCycle } from './entities/menstrual-cycle.entity';
import { CyclesController } from './cycles.controller';
import { CyclesService } from './cycles.service';

@Module({
  imports: [TypeOrmModule.forFeature([MenstrualCycle])],
  controllers: [CyclesController],
  providers: [CyclesService],
  exports: [CyclesService],
})
export class CyclesModule {}
