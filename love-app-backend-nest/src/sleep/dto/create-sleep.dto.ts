import { IsString, IsOptional, IsNumber } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class CreateSleepDto {
  @ApiProperty()
  @IsString()
  date: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  bedtime?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  wake_time?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  duration_minutes?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  quality?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  note?: string;
}
