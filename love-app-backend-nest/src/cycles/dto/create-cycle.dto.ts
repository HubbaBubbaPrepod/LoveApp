import { IsString, IsOptional, IsNumber, IsObject } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class CreateCycleDto {
  @ApiProperty()
  @IsString()
  start_date: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  end_date?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  cycle_length?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  period_length?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsObject()
  symptoms?: Record<string, any>;

  @ApiPropertyOptional()
  @IsOptional()
  @IsObject()
  mood?: Record<string, any>;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  notes?: string;
}
