import {
  Controller,
  Get,
  Post,
  Body,
  Query,
  UseGuards,
} from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import { sendResponse } from '../common/helpers/response.helper';
import { MissYouService } from './miss-you.service';
import { SendMissYouDto } from './dto/send-miss-you.dto';

@ApiTags('Miss You')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('missyou')
export class MissYouController {
  constructor(private readonly service: MissYouService) {}

  @Post()
  async send(
    @CurrentUser('userId') userId: number,
    @Body() dto: SendMissYouDto,
  ) {
    return sendResponse(await this.service.send(userId, dto));
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

  @Get('today')
  async getToday(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getToday(userId));
  }
}
