import { IsNumber, IsString } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class AnswerDto {
  @ApiProperty()
  @IsNumber()
  session_id: number;

  @ApiProperty()
  @IsNumber()
  round_number: number;

  @ApiProperty()
  @IsString()
  answer: string;
}
