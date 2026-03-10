import { IsOptional, IsBoolean, IsNumber } from 'class-validator';
import { ApiPropertyOptional } from '@nestjs/swagger';

export class UpdateSettingsDto {
  @ApiPropertyOptional()
  @IsOptional()
  @IsBoolean()
  sharing_enabled?: boolean;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  update_interval?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsBoolean()
  show_address?: boolean;

  @ApiPropertyOptional()
  @IsOptional()
  @IsBoolean()
  show_speed?: boolean;

  @ApiPropertyOptional()
  @IsOptional()
  @IsBoolean()
  show_battery?: boolean;
}
