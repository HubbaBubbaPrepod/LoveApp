import { IsString } from 'class-validator';

export class RemoveLockDto {
  @IsString()
  pin: string;
}
