import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ArtCanvas } from './entities/art-canvas.entity';
import { CanvasStroke } from './entities/canvas-stroke.entity';
import { ArtController } from './art.controller';
import { ArtService } from './art.service';

@Module({
  imports: [TypeOrmModule.forFeature([ArtCanvas, CanvasStroke])],
  controllers: [ArtController],
  providers: [ArtService],
  exports: [ArtService],
})
export class ArtModule {}
