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
import { MomentsService } from './moments.service';
import { CreateMomentDto } from './dto/create-moment.dto';

@ApiTags('Moments')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('moments')
export class MomentsController {
  constructor(private readonly service: MomentsService) {}

  @Post()
  async create(
    @CurrentUser('userId') userId: number,
    @Body() dto: CreateMomentDto,
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

  @Delete(':id')
  async remove(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.remove(userId, id));
  }
}
