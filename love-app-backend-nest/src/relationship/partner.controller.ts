import {
  Controller,
  Get,
  Put,
  Post,
  Body,
  UseGuards,
} from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import { RelationshipService } from './relationship.service';
import {
  UpdateRelationshipDto,
  LinkCodeDto,
} from './dto/relationship.dto';
import { sendResponse } from '../common/helpers/response.helper';

@ApiTags('Partner')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('partner')
export class PartnerController {
  constructor(private readonly service: RelationshipService) {}

  @Get()
  async get(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.get(userId));
  }

  @Put()
  async update(
    @CurrentUser('userId') userId: number,
    @Body() dto: UpdateRelationshipDto,
  ) {
    return sendResponse(await this.service.update(userId, dto));
  }

  @Post('generate-code')
  async generateCode(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.generateCode(userId));
  }

  @Post('link')
  async link(
    @CurrentUser('userId') userId: number,
    @Body() dto: LinkCodeDto,
  ) {
    return sendResponse(await this.service.link(userId, dto.code));
  }
}
