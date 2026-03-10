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
import { StoryService } from './story.service';
import { CreateStoryDto } from './dto/create-story.dto';
import { UpdateStoryDto } from './dto/update-story.dto';

@ApiTags('Story')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('story')
export class StoryController {
  constructor(private readonly service: StoryService) {}

  @Post()
  async create(
    @CurrentUser('userId') userId: number,
    @Body() dto: CreateStoryDto,
  ) {
    return sendResponse(await this.service.create(userId, dto));
  }

  @Get()
  async findAll(
    @CurrentUser('userId') userId: number,
    @Query('page') page?: string,
    @Query('pageSize') pageSize?: string,
  ) {
    return sendResponse(
      await this.service.findAll(userId, {
        page: page ? +page : undefined,
        pageSize: pageSize ? +pageSize : undefined,
      }),
    );
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

  @Put(':id')
  async update(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: UpdateStoryDto,
  ) {
    return sendResponse(await this.service.update(userId, id, dto));
  }

  @Delete(':id')
  async remove(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.remove(userId, id));
  }
}
