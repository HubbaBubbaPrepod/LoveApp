import { IsOptional, IsInt, Min } from 'class-validator';

export class EndSessionDto {
  @IsOptional()
  @IsInt()
  @Min(0)
  hearts_count?: number = 0;
}
