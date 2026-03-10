import {
  Controller,
  Get,
  Post,
  Body,
  Param,
  ParseIntPipe,
  UseGuards,
} from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import { sendResponse } from '../common/helpers/response.helper';
import { LoveTouchService } from './love-touch.service';
import { EndSessionDto } from './dto/end-session.dto';

@ApiTags('Love Touch')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('lovetouch')
export class LoveTouchController {
  constructor(private readonly service: LoveTouchService) {}

  @Post('start')
  async start(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.start(userId));
  }

  @Post(':id/join')
  async join(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.join(userId, id));
  }

  @Post(':id/end')
  async end(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: EndSessionDto,
  ) {
    return sendResponse(await this.service.end(userId, id, dto));
  }

  @Get('history')
  async history(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getHistory(userId));
  }
}
