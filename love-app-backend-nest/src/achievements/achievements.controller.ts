import {
  Controller,
  Get,
  Post,
  UseGuards,
} from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import { sendResponse } from '../common/helpers/response.helper';
import { AchievementsService } from './achievements.service';

@ApiTags('Achievements')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('achievements')
export class AchievementsController {
  constructor(private readonly service: AchievementsService) {}

  @Get()
  async getAll(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getAll(userId));
  }

  @Get('progress')
  async getProgress(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getProgress(userId));
  }

  @Post('check')
  async check(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.check(userId));
  }
}
