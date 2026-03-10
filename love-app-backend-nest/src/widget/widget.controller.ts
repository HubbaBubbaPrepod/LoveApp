import { Controller, Get, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import { sendResponse } from '../common/helpers/response.helper';
import { WidgetService } from './widget.service';

@ApiTags('Widget')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('widget')
export class WidgetController {
  constructor(private readonly service: WidgetService) {}

  @Get('summary')
  async getSummary(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getSummary(userId));
  }
}
