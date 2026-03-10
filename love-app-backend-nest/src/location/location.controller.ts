import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Query,
  UseGuards,
} from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import { sendResponse } from '../common/helpers/response.helper';
import { LocationService } from './location.service';
import { UpdateLocationDto } from './dto/update-location.dto';
import { BatchLocationDto } from './dto/batch-location.dto';
import { UpdateSettingsDto } from './dto/update-settings.dto';

@ApiTags('Location')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('location')
export class LocationController {
  constructor(private readonly service: LocationService) {}

  @Post('update')
  async update(
    @CurrentUser('userId') userId: number,
    @Body() dto: UpdateLocationDto,
  ) {
    return sendResponse(await this.service.update(userId, dto));
  }

  @Post('batch')
  async batch(
    @CurrentUser('userId') userId: number,
    @Body() dto: BatchLocationDto,
  ) {
    return sendResponse(await this.service.batch(userId, dto));
  }

  @Get('latest')
  async getLatest(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getLatest(userId));
  }

  @Get('history')
  async getHistory(
    @CurrentUser('userId') userId: number,
    @Query('hours') hours?: string,
  ) {
    return sendResponse(
      await this.service.getHistory(userId, {
        hours: hours ? +hours : undefined,
      }),
    );
  }

  @Get('stats')
  async getStats(
    @CurrentUser('userId') userId: number,
    @Query('start_date') startDate?: string,
    @Query('end_date') endDate?: string,
  ) {
    return sendResponse(
      await this.service.getStats(userId, {
        start_date: startDate,
        end_date: endDate,
      }),
    );
  }

  @Get('settings')
  async getSettings(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getSettings(userId));
  }

  @Put('settings')
  async updateSettings(
    @CurrentUser('userId') userId: number,
    @Body() dto: UpdateSettingsDto,
  ) {
    return sendResponse(await this.service.updateSettings(userId, dto));
  }

  @Delete('history')
  async clearHistory(
    @CurrentUser('userId') userId: number,
    @Query('days') days?: string,
  ) {
    return sendResponse(
      await this.service.clearHistory(userId, days ? +days : 30),
    );
  }
}
