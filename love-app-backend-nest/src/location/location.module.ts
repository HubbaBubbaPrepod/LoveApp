import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { LocationUpdate } from './entities/location-update.entity';
import { LocationHistory } from './entities/location-history.entity';
import { LocationSettings } from './entities/location-settings.entity';
import { LocationController } from './location.controller';
import { LocationService } from './location.service';

@Module({
  imports: [
    TypeOrmModule.forFeature([LocationUpdate, LocationHistory, LocationSettings]),
  ],
  controllers: [LocationController],
  providers: [LocationService],
  exports: [LocationService],
})
export class LocationModule {}
