import { IsString, IsOptional, IsObject } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class UpdateDayDto {
  @ApiProperty()
  @IsString()
  date: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsObject()
  symptoms?: Record<string, any>;

  @ApiPropertyOptional()
  @IsOptional()
  @IsObject()
  mood?: Record<string, any>;
}
