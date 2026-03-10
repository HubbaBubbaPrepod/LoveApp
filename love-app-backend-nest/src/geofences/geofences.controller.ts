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
import { GeofencesService } from './geofences.service';
import { CreateGeofenceDto } from './dto/create-geofence.dto';
import { CreateGeofenceEventDto } from './dto/create-geofence-event.dto';

@ApiTags('Geofences')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('geofences')
export class GeofencesController {
  constructor(private readonly service: GeofencesService) {}

  @Post()
  async create(
    @CurrentUser('userId') userId: number,
    @Body() dto: CreateGeofenceDto,
  ) {
    return sendResponse(await this.service.create(userId, dto));
  }

  @Get()
  async findAll(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.findAll(userId));
  }

  @Get('events/recent')
  async getRecentEvents(
    @CurrentUser('userId') userId: number,
    @Query('limit') limit?: string,
  ) {
    return sendResponse(
      await this.service.getRecentEvents(userId, {
        limit: limit ? +limit : undefined,
      }),
    );
  }

  @Get(':id')
  async findOne(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.findOne(userId, id));
  }

  @Put(':id')
  async update(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: CreateGeofenceDto,
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

  @Post('event')
  async recordEvent(
    @CurrentUser('userId') userId: number,
    @Body() dto: CreateGeofenceEventDto,
  ) {
    return sendResponse(await this.service.recordEvent(userId, dto));
  }

  @Get(':id/events')
  async getEvents(
    @Param('id', ParseIntPipe) id: number,
    @Query('hours') hours?: string,
  ) {
    return sendResponse(
      await this.service.getEvents(id, {
        hours: hours ? +hours : undefined,
      }),
    );
  }
}
