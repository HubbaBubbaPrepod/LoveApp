import { IsString, IsOptional } from 'class-validator';
import { ApiPropertyOptional } from '@nestjs/swagger';

export class UpdateChatSettingsDto {
  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  wallpaper_url?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  bubble_color?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  bubble_shape?: string;
}
