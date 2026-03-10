import {
  Controller,
  Get,
  Query,
  UseGuards,
} from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import { sendResponse } from '../common/helpers/response.helper';
import { IntimacyService } from './intimacy.service';

@ApiTags('Intimacy')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('intimacy')
export class IntimacyController {
  constructor(private readonly service: IntimacyService) {}

  @Get()
  async get(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.get(userId));
  }

  @Get('history')
  async getHistory(
    @CurrentUser('userId') userId: number,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
  ) {
    return sendResponse(
      await this.service.getHistory(userId, {
        page: page ? +page : undefined,
        limit: limit ? +limit : undefined,
      }),
    );
  }

  @Get('levels')
  async getLevels() {
    return sendResponse(this.service.getLevels());
  }
}
