import { Controller, Get, Query, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { sendResponse } from '../common/helpers/response.helper';
import { GifService } from './gif.service';

@ApiTags('GIF')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('gif')
export class GifController {
  constructor(private readonly service: GifService) {}

  @Get('search')
  async search(
    @Query('q') q: string,
    @Query('limit') limit?: string,
    @Query('pos') pos?: string,
  ) {
    return sendResponse(
      await this.service.search(q, limit ? +limit : undefined, pos),
    );
  }

  @Get('trending')
  async trending(@Query('limit') limit?: string) {
    return sendResponse(
      await this.service.trending(limit ? +limit : undefined),
    );
  }
}
