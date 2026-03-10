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
import { TasksService } from './tasks.service';
import { CreateTaskDto } from './dto/create-task.dto';

@ApiTags('Tasks')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('tasks')
export class TasksController {
  constructor(private readonly service: TasksService) {}

  @Get()
  async findAll(
    @CurrentUser('userId') userId: number,
    @Query('date') date?: string,
  ) {
    return sendResponse(
      await this.service.findAll(userId, { date }),
    );
  }

  @Post()
  async create(
    @CurrentUser('userId') userId: number,
    @Body() dto: CreateTaskDto,
  ) {
    return sendResponse(await this.service.create(userId, dto));
  }

  @Put(':id/complete')
  async complete(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.complete(userId, id));
  }

  @Delete(':id')
  async remove(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.remove(userId, id));
  }
}
