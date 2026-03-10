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
import { LettersService } from './letters.service';
import { CreateLetterDto } from './dto/create-letter.dto';

@ApiTags('Letters')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('letters')
export class LettersController {
  constructor(private readonly service: LettersService) {}

  @Post()
  async create(
    @CurrentUser('userId') userId: number,
    @Body() dto: CreateLetterDto,
  ) {
    return sendResponse(await this.service.create(userId, dto));
  }

  @Get()
  async findAll(
    @CurrentUser('userId') userId: number,
    @Query('filter') filter?: 'all' | 'available' | 'sealed',
  ) {
    return sendResponse(await this.service.findAll(userId, filter || 'all'));
  }

  @Get('stats')
  async getStats(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getStats(userId));
  }

  @Get(':id')
  async findOne(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.findOne(userId, id));
  }

  @Delete(':id')
  async remove(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.remove(userId, id));
  }
}
