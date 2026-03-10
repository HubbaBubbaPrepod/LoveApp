import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  Query,
  UseGuards,
  ParseIntPipe,
} from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import { sendResponse } from '../common/helpers/response.helper';
import { ActivitiesService } from './activities.service';
import { CreateActivityDto } from './dto/create-activity.dto';
import { CreateCustomTypeDto } from './dto/create-custom-type.dto';

@ApiTags('Activities')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('activities')
export class ActivitiesController {
  constructor(private readonly service: ActivitiesService) {}

  @Post()
  async create(
    @CurrentUser('userId') userId: number,
    @Body() dto: CreateActivityDto,
  ) {
    return sendResponse(await this.service.create(userId, dto));
  }

  @Get()
  async findAll(
    @CurrentUser('userId') userId: number,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
    @Query('category') category?: string,
    @Query('start_date') startDate?: string,
    @Query('end_date') endDate?: string,
  ) {
    return sendResponse(
      await this.service.findAll(userId, {
        page: page ? +page : undefined,
        limit: limit ? +limit : undefined,
        category,
        start_date: startDate,
        end_date: endDate,
      }),
    );
  }

  @Get('partner')
  async findPartner(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.findPartner(userId));
  }

  @Put(':id')
  async update(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: CreateActivityDto,
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

  // ─── Custom Activity Types ──────────────────────────────

  @Post('types')
  async createType(
    @CurrentUser('userId') userId: number,
    @Body() dto: CreateCustomTypeDto,
  ) {
    return sendResponse(await this.service.createType(userId, dto));
  }

  @Get('types')
  async getTypes(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getTypes(userId));
  }

  @Delete('types/:id')
  async deleteType(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.deleteType(userId, id));
  }
}
