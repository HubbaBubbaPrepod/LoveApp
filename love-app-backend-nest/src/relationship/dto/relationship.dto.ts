import { IsOptional, IsString } from 'class-validator';
import { ApiPropertyOptional } from '@nestjs/swagger';

export class GenerateCodeDto {}

export class LinkCodeDto {
  @ApiPropertyOptional()
  @IsString()
  code: string;
}

export class UpdateRelationshipDto {
  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  start_date?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  anniversary_date?: string;
}
