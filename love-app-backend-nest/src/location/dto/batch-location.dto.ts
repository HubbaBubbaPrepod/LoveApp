import {
  IsArray,
  IsNumber,
  IsOptional,
  IsDateString,
  ValidateNested,
  ArrayMaxSize,
} from 'class-validator';
import { Type } from 'class-transformer';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class BatchLocationPointDto {
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
  @IsDateString()
  recorded_at?: string;
}

export class BatchLocationDto {
  @ApiProperty({ type: [BatchLocationPointDto], maxItems: 500 })
  @IsArray()
  @ArrayMaxSize(500)
  @ValidateNested({ each: true })
  @Type(() => BatchLocationPointDto)
  points: BatchLocationPointDto[];
}
