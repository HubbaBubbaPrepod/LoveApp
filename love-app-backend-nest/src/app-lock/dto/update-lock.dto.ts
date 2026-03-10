import { IsString, IsOptional, IsBoolean, Matches } from 'class-validator';

export class UpdateLockDto {
  @IsString()
  current_pin: string;

  @IsOptional()
  @IsString()
  @Matches(/^\d{4,8}$/, { message: 'PIN must be 4-8 digits' })
  new_pin?: string;

  @IsOptional()
  @IsBoolean()
  is_biometric?: boolean;
}
