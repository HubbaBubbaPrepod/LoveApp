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
import { BucketlistService } from './bucketlist.service';
import { CreateBucketItemDto } from './dto/create-bucket-item.dto';
import { UpdateBucketItemDto } from './dto/update-bucket-item.dto';

@ApiTags('Bucket List')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('bucketlist')
export class BucketlistController {
  constructor(private readonly service: BucketlistService) {}

  @Post()
  async create(
    @CurrentUser('userId') userId: number,
    @Body() dto: CreateBucketItemDto,
  ) {
    return sendResponse(await this.service.create(userId, dto));
  }

  @Get()
  async findAll(
    @CurrentUser('userId') userId: number,
    @Query('category') category?: string,
    @Query('is_completed') is_completed?: string,
  ) {
    return sendResponse(
      await this.service.findAll(userId, { category, is_completed }),
    );
  }

  @Put(':id')
  async update(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: UpdateBucketItemDto,
  ) {
    return sendResponse(await this.service.update(userId, id, dto));
  }

  @Post(':id/complete')
  async complete(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.complete(userId, id));
  }

  @Post(':id/uncomplete')
  async uncomplete(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.uncomplete(userId, id));
  }

  @Delete(':id')
  async remove(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.remove(userId, id));
  }
}
