import {
  Controller,
  Post,
  Get,
  Put,
  Body,
  UseGuards,
} from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { AuthService } from './auth.service';
import { JwtAuthGuard } from './guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import {
  SignupDto,
  LoginDto,
  RefreshDto,
  GoogleAuthDto,
  FcmTokenDto,
  SetupProfileDto,
  UpdateProfileDto,
} from './dto/auth.dto';
import { sendResponse } from '../common/helpers/response.helper';

@ApiTags('Auth')
@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Post('signup')
  async signup(@Body() dto: SignupDto) {
    return sendResponse(await this.authService.signup(dto));
  }

  @Post('login')
  async login(@Body() dto: LoginDto) {
    return sendResponse(await this.authService.login(dto));
  }

  @Post('refresh')
  async refresh(@Body() dto: RefreshDto) {
    return sendResponse(await this.authService.refresh(dto.refreshToken));
  }

  @Post('logout')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  async logout(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.authService.logout(userId));
  }

  @Get('profile')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  async getProfile(@CurrentUser('userId') userId: number) {
    return sendResponse(await this.authService.getProfile(userId));
  }

  @Put('profile')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  async updateProfile(
    @CurrentUser('userId') userId: number,
    @Body() dto: UpdateProfileDto,
  ) {
    return sendResponse(await this.authService.updateProfile(userId, dto));
  }

  @Put('setup-profile')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  async setupProfile(
    @CurrentUser('userId') userId: number,
    @Body() dto: SetupProfileDto,
  ) {
    return sendResponse(await this.authService.setupProfile(userId, dto));
  }

  @Post('google')
  async googleAuth(@Body() dto: GoogleAuthDto) {
    return sendResponse(await this.authService.googleAuth(dto));
  }

  @Post('fcm-token')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  async fcmToken(
    @CurrentUser('userId') userId: number,
    @Body() dto: FcmTokenDto,
  ) {
    return sendResponse(await this.authService.upsertFcmToken(userId, dto));
  }
}
