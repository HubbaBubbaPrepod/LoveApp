import {
  Controller,
  Get,
  Post,
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
import { SleepService } from './sleep.service';
import { CreateSleepDto } from './dto/create-sleep.dto';

@ApiTags('Sleep')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('sleep')
export class SleepController {
  constructor(private readonly service: SleepService) {}

  @Post()
  async upsert(
    @CurrentUser('userId') userId: number,
    @Body() dto: CreateSleepDto,
  ) {
    return sendResponse(await this.service.upsert(userId, dto));
  }

  @Get()
  async findAll(
    @CurrentUser('userId') userId: number,
    @Query('limit') limit?: string,
    @Query('page') page?: string,
  ) {
    return sendResponse(
      await this.service.findAll(userId, {
        limit: limit ? +limit : undefined,
        page: page ? +page : undefined,
      }),
    );
  }

  @Get('partner')
  async findPartner(
    @CurrentUser('userId') userId: number,
    @Query('limit') limit?: string,
  ) {
    return sendResponse(
      await this.service.findPartner(userId, {
        limit: limit ? +limit : undefined,
      }),
    );
  }

  @Get('stats')
  async getStats(
    @CurrentUser('userId') userId: number,
    @Query('days') days?: string,
  ) {
    return sendResponse(
      await this.service.getStats(userId, {
        days: days ? +days : undefined,
      }),
    );
  }

  @Delete(':id')
  async remove(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.remove(userId, id));
  }
}
