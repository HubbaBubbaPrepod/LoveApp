import { IsString, IsOptional, IsNumber, IsIn, Min, Max } from 'class-validator';
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

  @ApiPropertyOptional({ minimum: 1, maximum: 10 })
  @IsOptional()
  @IsNumber()
  @Min(1)
  @Max(10)
  energy_level?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  activities?: string;
}
