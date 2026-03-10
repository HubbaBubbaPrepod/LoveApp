import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  UseGuards,
  ParseIntPipe,
} from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import { sendResponse } from '../common/helpers/response.helper';
import { MemorialService } from './memorial.service';
import { CreateMemorialDto } from './dto/create-memorial.dto';
import { UpdateMemorialDto } from './dto/update-memorial.dto';

@ApiTags('Memorial')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('memorial')
export class MemorialController {
  constructor(private readonly service: MemorialService) {}

  @Post()
  async create(
    @CurrentUser('userId') userId: number,
    @Body() dto: CreateMemorialDto,
  ) {
    return sendResponse(await this.service.create(userId, dto));
  }

  @Get()
  async findAll(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.findAll(userId));
  }

  @Put(':id')
  async update(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: UpdateMemorialDto,
  ) {
    return sendResponse(await this.service.update(userId, id, dto));
  }

  @Delete(':id')
  async remove(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.remove(userId, id));
  }
}
