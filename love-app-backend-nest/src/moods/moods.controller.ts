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
import { MoodsService } from './moods.service';
import { CreateMoodDto } from './dto/create-mood.dto';

@ApiTags('Moods')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('moods')
export class MoodsController {
  constructor(private readonly service: MoodsService) {}

  @Post()
  async create(
    @CurrentUser('userId') userId: number,
    @Body() dto: CreateMoodDto,
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
    @Body() dto: CreateMoodDto,
  ) {
    return sendResponse(await this.service.update(userId, id, dto));
  }

  @Get('partner')
  async findPartner(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.findPartner(userId));
  }

  @Get('analytics')
  async analytics(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.analytics(userId));
  }

  @Delete(':id')
  async remove(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.remove(userId, id));
  }
}
