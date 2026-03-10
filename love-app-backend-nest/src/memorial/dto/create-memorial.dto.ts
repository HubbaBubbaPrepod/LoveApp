import { IsString, IsOptional, IsBoolean, IsNumber } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class CreateMemorialDto {
  @ApiProperty()
  @IsString()
  title: string;

  @ApiProperty()
  @IsString()
  date: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  type?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  icon?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  color_hex?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsBoolean()
  repeat_yearly?: boolean;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  reminder_days?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  note?: string;
}
