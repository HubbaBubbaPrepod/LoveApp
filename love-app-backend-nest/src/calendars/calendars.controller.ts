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
import { CalendarsService } from './calendars.service';
import { CreateCalendarDto } from './dto/create-calendar.dto';
import { CreateEventDto } from './dto/create-event.dto';

@ApiTags('Calendars')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('calendars')
export class CalendarsController {
  constructor(private readonly service: CalendarsService) {}

  // ─── Calendars ──────────────────────────────────────────

  @Post()
  async create(
    @CurrentUser('userId') userId: number,
    @Body() dto: CreateCalendarDto,
  ) {
    return sendResponse(await this.service.create(userId, dto));
  }

  @Get()
  async findAll(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.findAll(userId));
  }

  @Get('partner')
  async findPartner(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.findPartner(userId));
  }

  @Put(':id')
  async update(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: CreateCalendarDto,
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

  // ─── Events ─────────────────────────────────────────────

  @Post('events')
  async createEvent(
    @CurrentUser('userId') userId: number,
    @Body() dto: CreateEventDto,
  ) {
    return sendResponse(await this.service.createEvent(userId, dto));
  }

  @Get('events')
  async findEvents(
    @CurrentUser('userId') userId: number,
    @Query('calendar_id') calendarId?: string,
    @Query('start_date') startDate?: string,
    @Query('end_date') endDate?: string,
  ) {
    return sendResponse(
      await this.service.findEvents(userId, {
        calendar_id: calendarId ? +calendarId : undefined,
        start_date: startDate,
        end_date: endDate,
      }),
    );
  }

  @Put('events/:id')
  async updateEvent(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: CreateEventDto,
  ) {
    return sendResponse(await this.service.updateEvent(userId, id, dto));
  }

  @Delete('events/:id')
  async removeEvent(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.removeEvent(userId, id));
  }
}
