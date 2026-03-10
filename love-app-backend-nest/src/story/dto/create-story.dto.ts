import { IsString, IsOptional, MaxLength, IsIn } from 'class-validator';
import { ApiPropertyOptional } from '@nestjs/swagger';

export class CreateStoryDto {
  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  @MaxLength(200)
  title?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  @MaxLength(5000)
  content?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  @IsIn(['text', 'photo', 'video', 'milestone'])
  entry_type?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  entry_date?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  media_url?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  emoji?: string;
}
