import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  UseGuards,
} from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import { sendResponse } from '../common/helpers/response.helper';
import { AppLockService } from './app-lock.service';
import { SetPinDto } from './dto/set-pin.dto';
import { VerifyPinDto } from './dto/verify-pin.dto';
import { UpdateLockDto } from './dto/update-lock.dto';
import { RemoveLockDto } from './dto/remove-lock.dto';

@ApiTags('App Lock')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('applock')
export class AppLockController {
  constructor(private readonly service: AppLockService) {}

  @Get()
  async getStatus(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getStatus(userId));
  }

  @Post()
  async setPin(
    @CurrentUser('userId') userId: number,
    @Body() dto: SetPinDto,
  ) {
    return sendResponse(await this.service.setPin(userId, dto));
  }

  @Post('verify')
  async verify(
    @CurrentUser('userId') userId: number,
    @Body() dto: VerifyPinDto,
  ) {
    return sendResponse(await this.service.verify(userId, dto));
  }

  @Put()
  async updateLock(
    @CurrentUser('userId') userId: number,
    @Body() dto: UpdateLockDto,
  ) {
    return sendResponse(await this.service.updateLock(userId, dto));
  }

  @Delete()
  async removeLock(
    @CurrentUser('userId') userId: number,
    @Body() dto: RemoveLockDto,
  ) {
    return sendResponse(await this.service.removeLock(userId, dto));
  }
}
