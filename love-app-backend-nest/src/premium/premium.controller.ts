import {
  Controller,
  Get,
  Post,
  Body,
  UseGuards,
} from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import { sendResponse } from '../common/helpers/response.helper';
import { PremiumService } from './premium.service';
import { VerifyPurchaseDto } from './dto/verify-purchase.dto';
import { RestoreDto } from './dto/restore.dto';

@ApiTags('Premium')
@Controller('premium')
export class PremiumController {
  constructor(private readonly service: PremiumService) {}

  @Get('plans')
  async getPlans() {
    return sendResponse(await this.service.getPlans());
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Get('status')
  async getStatus(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getStatus(userId));
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Post('verify')
  async verify(
    @CurrentUser('userId') userId: number,
    @Body() dto: VerifyPurchaseDto,
  ) {
    return sendResponse(await this.service.verify(userId, dto));
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Post('restore')
  async restore(
    @CurrentUser('userId') userId: number,
    @Body() dto: RestoreDto,
  ) {
    return sendResponse(await this.service.restore(userId, dto));
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Post('cancel')
  async cancel(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.cancel(userId));
  }
}
