import { IsInt } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class PlaceFurnitureDto {
  @ApiProperty()
  @IsInt()
  position_x: number;

  @ApiProperty()
  @IsInt()
  position_y: number;
}
