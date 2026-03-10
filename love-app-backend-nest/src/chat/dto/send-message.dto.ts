import { IsNumber, IsString, IsOptional, IsIn } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class SendMessageDto {
  @ApiProperty()
  @IsNumber()
  receiver_id: number;

  @ApiProperty({ enum: ['text','image','voice','video','location','sticker','drawing','emoji','custom'] })
  @IsString()
  @IsIn(['text','image','voice','video','location','sticker','drawing','emoji','custom'])
  message_type: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  content?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  image_url?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  voice_url?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  voice_duration?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  video_url?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  video_duration?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  video_thumbnail_url?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  latitude?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  longitude?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  location_name?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  sticker_id?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  drawing_data?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  emoji?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  reply_to_id?: number;
}
