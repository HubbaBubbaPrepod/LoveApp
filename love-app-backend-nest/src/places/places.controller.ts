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
import { PlacesService } from './places.service';
import { CreatePlaceDto } from './dto/create-place.dto';
import { UpdatePlaceDto } from './dto/update-place.dto';

@ApiTags('Places')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('places')
export class PlacesController {
  constructor(private readonly service: PlacesService) {}

  @Post()
  async create(
    @CurrentUser('userId') userId: number,
    @Body() dto: CreatePlaceDto,
  ) {
    return sendResponse(await this.service.create(userId, dto));
  }

  @Get()
  async findAll(
    @CurrentUser('userId') userId: number,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
    @Query('category') category?: string,
  ) {
    return sendResponse(
      await this.service.findAll(userId, {
        page: page ? +page : undefined,
        limit: limit ? +limit : undefined,
        category,
      }),
    );
  }

  @Put(':id')
  async update(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: UpdatePlaceDto,
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
