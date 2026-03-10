import {
  IsNumber,
  IsOptional,
  IsBoolean,
  IsString,
  IsIn,
  Min,
  Max,
} from 'class-validator';
import { ApiPropertyOptional } from '@nestjs/swagger';

const VALID_SCREEN_STATUSES = ['on', 'off', 'locked', 'unlocked'] as const;
const VALID_NETWORK_TYPES = ['wifi', 'cellular', 'none', 'ethernet', 'vpn'] as const;

export class UpdatePhoneStatusDto {
  @ApiPropertyOptional({ minimum: 0, maximum: 100 })
  @IsOptional()
  @IsNumber()
  @Min(0)
  @Max(100)
  battery_level?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsBoolean()
  is_charging?: boolean;

  @ApiPropertyOptional({ enum: VALID_SCREEN_STATUSES })
  @IsOptional()
  @IsString()
  @IsIn(VALID_SCREEN_STATUSES)
  screen_status?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  wifi_name?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsBoolean()
  is_active?: boolean;

  @ApiPropertyOptional()
  @IsOptional()
  @IsBoolean()
  app_in_foreground?: boolean;

  @ApiPropertyOptional({ enum: VALID_NETWORK_TYPES })
  @IsOptional()
  @IsString()
  @IsIn(VALID_NETWORK_TYPES)
  network_type?: string;
}
