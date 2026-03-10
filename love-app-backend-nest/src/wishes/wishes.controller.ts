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
import { WishesService } from './wishes.service';
import { CreateWishDto } from './dto/create-wish.dto';
import { UpdateWishDto } from './dto/update-wish.dto';

@ApiTags('Wishes')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('wishes')
export class WishesController {
  constructor(private readonly service: WishesService) {}

  @Post()
  async create(
    @CurrentUser('userId') userId: number,
    @Body() dto: CreateWishDto,
  ) {
    return sendResponse(await this.service.create(userId, dto));
  }

  @Get()
  async findAll(
    @CurrentUser('userId') userId: number,
    @Query('category') category?: string,
    @Query('is_completed') is_completed?: string,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
  ) {
    return sendResponse(
      await this.service.findAll(userId, {
        category,
        is_completed,
        page: page ? +page : undefined,
        limit: limit ? +limit : undefined,
      }),
    );
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
    @Body() dto: UpdateWishDto,
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

  @Post(':id/complete')
  async complete(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.complete(userId, id));
  }
}
