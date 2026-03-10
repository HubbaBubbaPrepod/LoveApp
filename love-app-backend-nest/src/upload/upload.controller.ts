import { Controller, Post, Req, UseGuards } from '@nestjs/common';
import type { FastifyRequest } from 'fastify';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import { sendResponse } from '../common/helpers/response.helper';
import { UploadService } from './upload.service';

@Controller('upload')
@UseGuards(JwtAuthGuard)
export class UploadController {
  constructor(private readonly uploadService: UploadService) {}

  @Post('profile')
  async uploadProfile(
    @CurrentUser('userId') userId: number,
    @Req() req: FastifyRequest,
  ) {
    const result = await this.uploadService.uploadProfile(userId, req);
    return sendResponse(result, 'Profile image uploaded');
  }

  @Post('image')
  async uploadImage(
    @CurrentUser('userId') userId: number,
    @Req() req: FastifyRequest,
  ) {
    const result = await this.uploadService.uploadImage(userId, req);
    return sendResponse(result, 'Image uploaded');
  }
}
