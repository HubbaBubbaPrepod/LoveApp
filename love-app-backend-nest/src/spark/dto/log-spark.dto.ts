import { IsString, IsIn } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export const VALID_SPARK_TYPES = [
  'kiss',
  'hug',
  'message',
  'call',
  'interaction',
  'manual',
  'love_touch',
  'miss_you',
  'mood',
  'game_complete',
] as const;

export class LogSparkDto {
  @ApiProperty({ enum: VALID_SPARK_TYPES })
  @IsString()
  @IsIn(VALID_SPARK_TYPES)
  spark_type: string;
}
