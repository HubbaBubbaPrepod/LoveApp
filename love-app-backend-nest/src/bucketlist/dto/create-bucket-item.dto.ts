import { IsString, IsOptional, IsIn } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

const CATEGORIES = [
  'travel', 'food', 'adventure', 'romance', 'culture',
  'sport', 'learning', 'creative', 'other',
] as const;

export class CreateBucketItemDto {
  @ApiProperty()
  @IsString()
  title: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  description?: string;

  @ApiPropertyOptional({ enum: CATEGORIES })
  @IsOptional()
  @IsString()
  @IsIn(CATEGORIES)
  category?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  emoji?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  image_url?: string;
}
