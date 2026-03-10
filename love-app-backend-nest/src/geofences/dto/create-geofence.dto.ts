import {
  IsString,
  IsNumber,
  IsOptional,
  IsBoolean,
  IsIn,
} from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

const VALID_CATEGORIES = [
  'home', 'work', 'school', 'restaurant', 'cafe', 'cinema', 'park',
  'beach', 'bar', 'concert', 'museum', 'gym', 'hospital', 'airport',
  'shop', 'hotel', 'travel', 'date', 'other',
] as const;

export class CreateGeofenceDto {
  @ApiProperty()
  @IsString()
  name: string;

  @ApiProperty()
  @IsNumber()
  latitude: number;

  @ApiProperty()
  @IsNumber()
  longitude: number;

  @ApiProperty()
  @IsNumber()
  radius_meters: number;

  @ApiPropertyOptional({ enum: VALID_CATEGORIES })
  @IsOptional()
  @IsString()
  @IsIn(VALID_CATEGORIES)
  category?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  address?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  icon?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  color?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsBoolean()
  notify_on_enter?: boolean;

  @ApiPropertyOptional()
  @IsOptional()
  @IsBoolean()
  notify_on_exit?: boolean;
}
