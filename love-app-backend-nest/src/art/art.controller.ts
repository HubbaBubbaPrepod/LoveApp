import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  UseGuards,
  ParseIntPipe,
} from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import { sendResponse } from '../common/helpers/response.helper';
import { ArtService } from './art.service';
import { CreateCanvasDto } from './dto/create-canvas.dto';
import { UpdateStrokesDto } from './dto/update-strokes.dto';

@ApiTags('Art')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('art')
export class ArtController {
  constructor(private readonly service: ArtService) {}

  @Post()
  async create(
    @CurrentUser('userId') userId: number,
    @Body() dto: CreateCanvasDto,
  ) {
    return sendResponse(await this.service.create(userId, dto));
  }

  @Get()
  async findAll(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.findAll(userId));
  }

  @Get(':id/strokes')
  async getStrokes(@Param('id', ParseIntPipe) id: number) {
    return sendResponse(await this.service.getStrokes(id));
  }

  @Put(':id/strokes')
  async updateStrokes(
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: UpdateStrokesDto,
  ) {
    return sendResponse(await this.service.updateStrokes(id, dto));
  }

  @Put(':id/thumbnail')
  async uploadThumbnail(
    @Param('id', ParseIntPipe) id: number,
    @Body('thumbnail_url') thumbnailUrl: string,
  ) {
    return sendResponse(await this.service.uploadThumbnail(id, thumbnailUrl));
  }

  @Delete(':id')
  async remove(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.remove(userId, id));
  }
}
