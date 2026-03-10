import { IsString, IsOptional, IsNumber } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class CreateActivityDto {
  @ApiProperty()
  @IsString()
  event_date: string;

  @ApiProperty()
  @IsString()
  activity_type: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  description?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  category?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  duration_minutes?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  start_time?: string;
}
