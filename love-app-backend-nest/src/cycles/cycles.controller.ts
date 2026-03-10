import {
  Controller,
  Get,
  Post,
  Put,
  Patch,
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
import { CyclesService } from './cycles.service';
import { CreateCycleDto } from './dto/create-cycle.dto';
import { UpdateDayDto } from './dto/update-day.dto';

@ApiTags('Cycles')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('cycles')
export class CyclesController {
  constructor(private readonly service: CyclesService) {}

  @Post()
  async create(
    @CurrentUser('userId') userId: number,
    @Body() dto: CreateCycleDto,
  ) {
    return sendResponse(await this.service.create(userId, dto));
  }

  @Get()
  async findAll(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.findAll(userId));
  }

  @Get('latest')
  async findLatest(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.findLatest(userId));
  }

  @Get('partner')
  async findPartner(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.findPartner(userId));
  }

  @Put(':id')
  async update(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: CreateCycleDto,
  ) {
    return sendResponse(await this.service.update(userId, id, dto));
  }

  @Patch(':id')
  async updateDay(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: UpdateDayDto,
  ) {
    return sendResponse(await this.service.updateDay(userId, id, dto));
  }

  @Delete(':id')
  async remove(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.remove(userId, id));
  }
}
