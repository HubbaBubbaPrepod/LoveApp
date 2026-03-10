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
import { GalleryService } from './gallery.service';
import { CreatePhotoDto } from './dto/create-photo.dto';

@ApiTags('Gallery')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('gallery')
export class GalleryController {
  constructor(private readonly service: GalleryService) {}

  @Post()
  async create(
    @CurrentUser('userId') userId: number,
    @Body() dto: CreatePhotoDto,
  ) {
    return sendResponse(await this.service.create(userId, dto));
  }

  @Get()
  async findAll(
    @CurrentUser('userId') userId: number,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
  ) {
    return sendResponse(
      await this.service.findAll(userId, {
        page: page ? +page : undefined,
        limit: limit ? +limit : undefined,
      }),
    );
  }

  @Put(':id')
  async update(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: { caption?: string },
  ) {
    return sendResponse(await this.service.updateCaption(userId, id, dto.caption ?? ''));
  }

  @Delete(':id')
  async remove(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.remove(userId, id));
  }
}
