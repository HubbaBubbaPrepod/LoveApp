import { IsOptional, IsString } from 'class-validator';

export class SendMissYouDto {
  @IsOptional()
  @IsString()
  emoji?: string = '❤️';

  @IsOptional()
  @IsString()
  message?: string;
}
