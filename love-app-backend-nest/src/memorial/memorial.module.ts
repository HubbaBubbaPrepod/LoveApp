import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { MemorialDay } from './entities/memorial-day.entity';
import { MemorialController } from './memorial.controller';
import { MemorialService } from './memorial.service';

@Module({
  imports: [TypeOrmModule.forFeature([MemorialDay])],
  controllers: [MemorialController],
  providers: [MemorialService],
  exports: [MemorialService],
})
export class MemorialModule {}
