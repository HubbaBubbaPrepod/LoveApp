import {
  IsString,
  IsOptional,
  IsNumber,
  IsIn,
  Min,
  Max,
  IsNotEmpty,
} from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export const VALID_CATEGORIES = [
  'restaurant',
  'cafe',
  'park',
  'cinema',
  'hotel',
  'home',
  'travel',
  'date',
  'other',
] as const;

export class CreatePlaceDto {
  @ApiProperty()
  @IsString()
  @IsNotEmpty()
  name: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  address?: string;

  @ApiPropertyOptional({ enum: VALID_CATEGORIES })
  @IsOptional()
  @IsString()
  @IsIn(VALID_CATEGORIES)
  category?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  note?: string;

  @ApiProperty()
  @IsNumber()
  @Min(-90)
  @Max(90)
  latitude: number;

  @ApiProperty()
  @IsNumber()
  @Min(-180)
  @Max(180)
  longitude: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  image_url?: string;
}
