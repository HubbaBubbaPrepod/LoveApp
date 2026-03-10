import { IsString } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class RestoreDto {
  @ApiProperty()
  @IsString()
  purchase_token: string;
}
