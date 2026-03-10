import { IsString, Matches } from 'class-validator';

export class SetPinDto {
  @IsString()
  @Matches(/^\d{4,8}$/, { message: 'PIN must be 4-8 digits' })
  pin: string;
}
