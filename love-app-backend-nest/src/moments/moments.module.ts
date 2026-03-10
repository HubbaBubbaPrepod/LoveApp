import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Moment } from './entities/moment.entity';
import { MomentsController } from './moments.controller';
import { MomentsService } from './moments.service';

@Module({
  imports: [TypeOrmModule.forFeature([Moment])],
  controllers: [MomentsController],
  providers: [MomentsService],
  exports: [MomentsService],
})
export class MomentsModule {}
