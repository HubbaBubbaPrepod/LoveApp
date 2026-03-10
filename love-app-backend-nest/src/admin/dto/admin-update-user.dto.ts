import { IsString, IsOptional, IsIn } from 'class-validator';

export class AdminUpdateUserDto {
  @IsString()
  @IsOptional()
  display_name?: string;

  @IsString()
  @IsOptional()
  email?: string;

  @IsString()
  @IsOptional()
  @IsIn(['user', 'admin'])
  role?: string;
}
