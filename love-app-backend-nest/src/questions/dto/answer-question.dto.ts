import { IsString, IsNotEmpty } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class AnswerQuestionDto {
  @ApiProperty()
  @IsString()
  @IsNotEmpty()
  answer: string;
}
