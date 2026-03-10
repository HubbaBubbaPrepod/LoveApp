import { PartialType } from '@nestjs/swagger';
import { CreateMemorialDto } from './create-memorial.dto';

export class UpdateMemorialDto extends PartialType(CreateMemorialDto) {}
