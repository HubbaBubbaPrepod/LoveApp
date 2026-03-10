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
import { PhoneStatusService } from './phone-status.service';
import { UpdatePhoneStatusDto } from './dto/update-phone-status.dto';

@ApiTags('Phone Status')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('phonestatus')
export class PhoneStatusController {
  constructor(private readonly service: PhoneStatusService) {}

  @Post('update')
  async update(
    @CurrentUser('userId') userId: number,
    @Body() dto: UpdatePhoneStatusDto,
  ) {
    return sendResponse(await this.service.update(userId, dto));
  }

  @Get('partner')
  async getPartner(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getPartner(userId));
  }

  @Get('me')
  async getMe(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getMe(userId));
  }

  @Get('history')
  async getHistory(
    @CurrentUser('userId') userId: number,
    @Query('hours') hours?: string,
  ) {
    return sendResponse(
      await this.service.getHistory(userId, {
        hours: hours ? +hours : undefined,
      }),
    );
  }

  @Get('both')
  async getBoth(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.service.getBoth(userId));
  }
}
