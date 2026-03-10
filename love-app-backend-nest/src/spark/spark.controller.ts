import {
  Controller,
  Get,
  Post,
  Body,
  Query,
  UseGuards,
} from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import { sendResponse } from '../common/helpers/response.helper';
import { SparkService } from './spark.service';
import { LogSparkDto } from './dto/log-spark.dto';

@ApiTags('Spark')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('spark')
export class SparkController {
  constructor(private readonly service: SparkService) {}

  @Get()
  async get(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.get(userId));
  }

  @Post('log')
  async log(
    @CurrentUser('userId') userId: number,
    @Body() dto: LogSparkDto,
  ) {
    return sendResponse(await this.service.log(userId, dto));
  }

  @Get('history')
  async history(
    @CurrentUser('userId') userId: number,
    @Query('limit') limit?: string,
  ) {
    return sendResponse(
      await this.service.getHistory(userId, {
        limit: limit ? +limit : undefined,
      }),
    );
  }

  @Get('breakdown')
  async breakdown(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getBreakdown(userId));
  }
}
