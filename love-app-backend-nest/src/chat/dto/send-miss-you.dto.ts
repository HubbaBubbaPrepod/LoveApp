import { IsString, IsOptional } from 'class-validator';
import { ApiPropertyOptional } from '@nestjs/swagger';

export class SendMissYouDto {
  @ApiPropertyOptional({ default: '❤️' })
  @IsOptional()
  @IsString()
  emoji?: string = '❤️';

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  message?: string;
}
