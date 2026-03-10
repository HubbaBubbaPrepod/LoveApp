import { IsNumber, IsString, IsOptional } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class VerifyPurchaseDto {
  @ApiProperty()
  @IsNumber()
  plan_id: number;

  @ApiProperty()
  @IsString()
  purchase_token: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  order_id?: string;
}
