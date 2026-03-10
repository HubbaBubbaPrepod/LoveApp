import {
  Controller,
  Get,
  Put,
  Body,
  UseGuards,
} from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import { sendResponse } from '../common/helpers/response.helper';
import { ChatSettingsService } from './chat-settings.service';
import { UpdateChatSettingsDto } from './dto/update-chat-settings.dto';

@ApiTags('Chat Settings')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('chatsettings')
export class ChatSettingsController {
  constructor(private readonly service: ChatSettingsService) {}

  @Get()
  async get(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.get(userId));
  }

  @Put()
  async update(
    @CurrentUser('userId') userId: number,
    @Body() dto: UpdateChatSettingsDto,
  ) {
    return sendResponse(await this.service.update(userId, dto));
  }

  @Get('wallpapers')
  async getWallpapers() {
    return sendResponse(this.service.getWallpapers());
  }
}
