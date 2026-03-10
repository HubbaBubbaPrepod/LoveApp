import { IsString, IsOptional } from 'class-validator';
import { ApiPropertyOptional } from '@nestjs/swagger';

export class CreateCanvasDto {
  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  title?: string;
}
