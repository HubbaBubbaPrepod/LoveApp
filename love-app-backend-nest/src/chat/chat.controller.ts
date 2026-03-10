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
import { ChatService } from './chat.service';
import { SendMessageDto } from './dto/send-message.dto';
import { SendMissYouDto } from './dto/send-miss-you.dto';

@ApiTags('Chat')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('chat')
export class ChatController {
  constructor(private readonly service: ChatService) {}

  @Post()
  async send(
    @CurrentUser('userId') userId: number,
    @Body() dto: SendMessageDto,
  ) {
    return sendResponse(await this.service.send(userId, dto));
  }

  @Get()
  async findAll(
    @CurrentUser('userId') userId: number,
    @Query('partner_id', ParseIntPipe) partnerId: number,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
  ) {
    return sendResponse(
      await this.service.findAll(userId, partnerId, {
        page: page ? +page : undefined,
        limit: limit ? +limit : undefined,
      }),
    );
  }

  @Put(':id/read')
  async markRead(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.markRead(userId, id));
  }

  @Put('read-all')
  async markAllRead(
    @CurrentUser('userId') userId: number,
    @Query('partner_id', ParseIntPipe) partnerId: number,
  ) {
    return sendResponse(await this.service.markAllRead(userId, partnerId));
  }

  @Delete(':id')
  async remove(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.remove(userId, id));
  }

  /* ── Stickers ── */

  @Get('stickers/packs')
  async getStickerPacks(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getStickerPacks(userId));
  }

  @Get('stickers/pack/:id')
  async getStickerPack(@Param('id', ParseIntPipe) id: number) {
    return sendResponse(await this.service.getStickerPack(id));
  }

  @Post('stickers/acquire/:packId')
  async acquirePack(
    @CurrentUser('userId') userId: number,
    @Param('packId', ParseIntPipe) packId: number,
  ) {
    return sendResponse(await this.service.acquirePack(userId, packId));
  }

  /* ── Miss You ── */

  @Post('missyou')
  async sendMissYou(
    @CurrentUser('userId') userId: number,
    @Body() dto: SendMissYouDto,
  ) {
    return sendResponse(await this.service.sendMissYou(userId, dto));
  }

  @Get('missyou')
  async getMissYouToday(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getMissYouToday(userId));
  }
}
