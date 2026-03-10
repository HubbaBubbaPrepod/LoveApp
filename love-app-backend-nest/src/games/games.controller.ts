import {
  Controller,
  Get,
  Post,
  Body,
  Param,
  UseGuards,
  ParseIntPipe,
} from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import { sendResponse } from '../common/helpers/response.helper';
import { GamesService } from './games.service';
import { StartGameDto } from './dto/start-game.dto';
import { AnswerDto } from './dto/answer.dto';

@ApiTags('Games')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('games')
export class GamesController {
  constructor(private readonly service: GamesService) {}

  @Post('start')
  async start(
    @CurrentUser('userId') userId: number,
    @Body() dto: StartGameDto,
  ) {
    return sendResponse(await this.service.start(userId, dto));
  }

  @Get('session/:id')
  async getSession(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.getSession(userId, id));
  }

  @Post('answer')
  async answer(
    @CurrentUser('userId') userId: number,
    @Body() dto: AnswerDto,
  ) {
    return sendResponse(await this.service.answer(userId, dto));
  }

  @Get()
  async getRecent(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getRecent(userId));
  }

  @Get('questions/:type')
  async getQuestionsByType(@Param('type') type: string) {
    return sendResponse(await this.service.getQuestionsByType(type));
  }

  @Get('compatibility')
  async getCompatibility(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getCompatibility(userId));
  }
}
