import { IsString, IsIn } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class StartGameDto {
  @ApiProperty({ enum: ['would_you_rather', 'truth_or_dare', 'quiz'] })
  @IsString()
  @IsIn(['would_you_rather', 'truth_or_dare', 'quiz'])
  game_type: string;
}
