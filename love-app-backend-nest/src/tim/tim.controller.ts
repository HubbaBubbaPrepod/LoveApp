import { Controller, Get, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import { sendResponse } from '../common/helpers/response.helper';
import { TimService } from './tim.service';

@ApiTags('TIM')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('tim')
export class TimController {
  constructor(private readonly service: TimService) {}

  @Get('usersig')
  async getUserSig(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getUserSig(userId));
  }
}
