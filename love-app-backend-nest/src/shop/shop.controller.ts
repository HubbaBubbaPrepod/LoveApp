import {
  Controller,
  Get,
  Post,
  Param,
  Query,
  Body,
  UseGuards,
  ParseIntPipe,
} from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import { sendResponse } from '../common/helpers/response.helper';
import { ShopService } from './shop.service';

@ApiTags('Shop')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('shop')
export class ShopController {
  constructor(private readonly service: ShopService) {}

  @Get('balance')
  async getBalance(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getBalance(userId));
  }

  @Get('items')
  async getItems(
    @CurrentUser('userId') userId: number,
    @Query('category') category?: string,
  ) {
    return sendResponse(await this.service.getItems(userId, category));
  }

  @Post('buy/:id')
  async buy(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.buy(userId, id));
  }

  @Get('daily-deals')
  async getDailyDeals(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getDailyDeals(userId));
  }

  @Post('daily-deals/buy/:id')
  async buyDeal(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.buyDeal(userId, id));
  }

  @Get('missions')
  async getMissions(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getMissions(userId));
  }

  @Post('missions/progress')
  async progressMission(
    @CurrentUser('userId') userId: number,
    @Body('code') code: string,
    @Body('increment') increment: number,
  ) {
    return sendResponse(
      await this.service.progressMission(userId, code, increment || 1),
    );
  }

  @Post('missions/claim/:id')
  async claimMission(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return sendResponse(await this.service.claimMission(userId, id));
  }

  @Get('transactions')
  async getTransactions(
    @CurrentUser('userId') userId: number,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
  ) {
    return sendResponse(
      await this.service.getTransactions(userId, {
        page: page ? +page : undefined,
        limit: limit ? +limit : undefined,
      }),
    );
  }

  @Get('summary')
  async getSummary(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getSummary(userId));
  }
}
