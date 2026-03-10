import { IsNumber, IsOptional, IsString, IsIn } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class CreateGeofenceEventDto {
  @ApiProperty()
  @IsNumber()
  geofence_id: number;

  @ApiProperty({ enum: ['enter', 'exit', 'dwell'] })
  @IsString()
  @IsIn(['enter', 'exit', 'dwell'])
  event_type: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  latitude?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  longitude?: number;
}
