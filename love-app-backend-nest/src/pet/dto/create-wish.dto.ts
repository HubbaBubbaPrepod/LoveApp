import { IsNotEmpty, IsString } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class CreateWishDto {
  @ApiProperty()
  @IsNotEmpty()
  @IsString()
  content: string;
}
