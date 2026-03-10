import { IsNumber, IsOptional, IsBoolean, IsString } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class UpdateLocationDto {
  @ApiProperty()
  @IsNumber()
  latitude: number;

  @ApiProperty()
  @IsNumber()
  longitude: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  accuracy?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  altitude?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  speed?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  bearing?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  provider?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  address?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsBoolean()
  is_moving?: boolean;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  battery_level?: number;
}
