import { IsString, IsOptional, IsIn } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

const VALID_MOOD_TYPES = [
  'rad', 'good', 'neutral', 'bad', 'awful',
  'happy', 'sad', 'anxious', 'excited', 'tired',
  'angry', 'loving', 'grateful', 'confused',
] as const;

export class CreateMoodDto {
  @ApiProperty({ enum: VALID_MOOD_TYPES })
  @IsString()
  @IsIn(VALID_MOOD_TYPES)
  mood_type: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  note?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  date?: string;

  @ApiPropertyOptional()
  @IsOptional()
  metadata?: Record<string, any>;
}
