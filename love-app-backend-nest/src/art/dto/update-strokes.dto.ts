import { IsString } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class UpdateStrokesDto {
  @ApiProperty({ description: 'JSON string of strokes data' })
  @IsString()
  strokes_data: string;
}
