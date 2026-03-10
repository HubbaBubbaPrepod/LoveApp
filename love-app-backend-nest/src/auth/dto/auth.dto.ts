import { IsString, IsEmail, IsOptional, MinLength } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class SignupDto {
  @ApiProperty()
  @IsString()
  username: string;

  @ApiProperty()
  @IsEmail()
  email: string;

  @ApiProperty()
  @IsString()
  @MinLength(6)
  password: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  display_name?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  gender?: string;
}

export class LoginDto {
  @ApiProperty()
  @IsEmail()
  email: string;

  @ApiProperty()
  @IsString()
  password: string;
}

export class RefreshDto {
  @ApiProperty()
  @IsString()
  refreshToken: string;
}

export class GoogleAuthDto {
  @ApiProperty()
  @IsString()
  idToken: string;
}

export class FcmTokenDto {
  @ApiProperty()
  @IsString()
  fcm_token: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  device_id?: string;
}

export class SetupProfileDto {
  @ApiProperty()
  @IsString()
  gender: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  display_name?: string;
}

export class UpdateProfileDto {
  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  display_name?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  profile_image?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  avatar_url?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  gender?: string;
}
