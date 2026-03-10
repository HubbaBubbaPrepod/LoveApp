import { IsString, IsOptional, IsNumber, IsBoolean } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class CreateEventDto {
  @ApiProperty()
  @IsNumber()
  calendar_id: number;

  @ApiProperty()
  @IsString()
  title: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  description?: string;

  @ApiProperty()
  @IsString()
  event_date: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  start_time?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  end_time?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  location?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  color?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  icon?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsBoolean()
  is_recurring?: boolean;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  recurrence_rule?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  reminder_minutes?: number;
}
