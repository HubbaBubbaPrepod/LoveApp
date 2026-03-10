import {
  Controller,
  Post,
  Get,
  Put,
  Delete,
  Body,
  Param,
  Query,
  UseGuards,
  ParseIntPipe,
  Res,
} from '@nestjs/common';
import type { FastifyReply } from 'fastify';
import { AdminService } from './admin.service';
import { AdminGuard } from './guards/admin.guard';
import { AdminLoginDto } from './dto/admin-login.dto';
import { AdminUpdateUserDto } from './dto/admin-update-user.dto';
import { sendResponse } from '../common/helpers/response.helper';

@Controller('admin')
export class AdminController {
  constructor(private readonly adminService: AdminService) {}

  @Post('login')
  async login(@Body() dto: AdminLoginDto) {
    const result = await this.adminService.login(dto);
    return sendResponse(result, 'Admin login successful');
  }

  // --- Stats ---
  @Get('stats')
  @UseGuards(AdminGuard)
  async getStats() {
    const stats = await this.adminService.getStats();
    return sendResponse(stats);
  }

  @Get('stats/timeline')
  @UseGuards(AdminGuard)
  async getTimeline(@Query('days') days?: number) {
    const data = await this.adminService.getTimeline(days || 30);
    return sendResponse(data);
  }

  @Get('stats/activities-by-type')
  @UseGuards(AdminGuard)
  async getActivitiesByType() {
    const data = await this.adminService.getActivitiesByType();
    return sendResponse(data);
  }

  // --- Users ---
  @Get('users')
  @UseGuards(AdminGuard)
  async getUsers(
    @Query() query: { _start?: number; _end?: number; _sort?: string; _order?: string; q?: string },
    @Res({ passthrough: true }) reply: FastifyReply,
  ) {
    const { data, total } = await this.adminService.getUsers(query);
    reply.header('X-Total-Count', total);
    reply.header('Access-Control-Expose-Headers', 'X-Total-Count');
    return data;
  }

  @Get('users/:id')
  @UseGuards(AdminGuard)
  async getUser(@Param('id', ParseIntPipe) id: number) {
    const user = await this.adminService.getUser(id);
    return user;
  }

  @Put('users/:id')
  @UseGuards(AdminGuard)
  async updateUser(
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: AdminUpdateUserDto,
  ) {
    const user = await this.adminService.updateUser(id, dto);
    return user;
  }

  @Delete('users/:id')
  @UseGuards(AdminGuard)
  async deleteUser(@Param('id', ParseIntPipe) id: number) {
    return this.adminService.deleteUser(id);
  }

  // --- Activities ---
  @Get('activities')
  @UseGuards(AdminGuard)
  async getActivities(
    @Query() query: { _start?: number; _end?: number; _sort?: string; _order?: string },
    @Res({ passthrough: true }) reply: FastifyReply,
  ) {
    const { data, total } = await this.adminService.getActivities(query);
    reply.header('X-Total-Count', total);
    reply.header('Access-Control-Expose-Headers', 'X-Total-Count');
    return data;
  }

  @Delete('activities/:id')
  @UseGuards(AdminGuard)
  async deleteActivity(@Param('id', ParseIntPipe) id: number) {
    return this.adminService.deleteActivity(id);
  }

  // --- Moods ---
  @Get('moods')
  @UseGuards(AdminGuard)
  async getMoods(
    @Query() query: { _start?: number; _end?: number; _sort?: string; _order?: string },
    @Res({ passthrough: true }) reply: FastifyReply,
  ) {
    const { data, total } = await this.adminService.getMoods(query);
    reply.header('X-Total-Count', total);
    reply.header('Access-Control-Expose-Headers', 'X-Total-Count');
    return data;
  }

  @Delete('moods/:id')
  @UseGuards(AdminGuard)
  async deleteMood(@Param('id', ParseIntPipe) id: number) {
    return this.adminService.deleteMood(id);
  }

  // --- Notes ---
  @Get('notes')
  @UseGuards(AdminGuard)
  async getNotes(
    @Query() query: { _start?: number; _end?: number; _sort?: string; _order?: string },
    @Res({ passthrough: true }) reply: FastifyReply,
  ) {
    const { data, total } = await this.adminService.getNotes(query);
    reply.header('X-Total-Count', total);
    reply.header('Access-Control-Expose-Headers', 'X-Total-Count');
    return data;
  }

  @Delete('notes/:id')
  @UseGuards(AdminGuard)
  async deleteNote(@Param('id', ParseIntPipe) id: number) {
    return this.adminService.deleteNote(id);
  }

  // --- Wishes ---
  @Get('wishes')
  @UseGuards(AdminGuard)
  async getWishes(
    @Query() query: { _start?: number; _end?: number; _sort?: string; _order?: string },
    @Res({ passthrough: true }) reply: FastifyReply,
  ) {
    const { data, total } = await this.adminService.getWishes(query);
    reply.header('X-Total-Count', total);
    reply.header('Access-Control-Expose-Headers', 'X-Total-Count');
    return data;
  }

  @Delete('wishes/:id')
  @UseGuards(AdminGuard)
  async deleteWish(@Param('id', ParseIntPipe) id: number) {
    return this.adminService.deleteWish(id);
  }
}
