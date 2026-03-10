import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { BucketListItem } from './entities/bucket-list-item.entity';
import { BucketlistController } from './bucketlist.controller';
import { BucketlistService } from './bucketlist.service';

@Module({
  imports: [TypeOrmModule.forFeature([BucketListItem])],
  controllers: [BucketlistController],
  providers: [BucketlistService],
  exports: [BucketlistService],
})
export class BucketlistModule {}
