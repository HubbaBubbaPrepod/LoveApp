import {
  Controller,
  Get,
  Put,
  Post,
  Delete,
  Body,
  Param,
  Query,
  ParseIntPipe,
  UseGuards,
} from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import { sendResponse } from '../common/helpers/response.helper';
import { PetService } from './pet.service';
import { UpdatePetDto } from './dto/update-pet.dto';
import { PlaceFurnitureDto } from './dto/place-furniture.dto';
import { CreateWishDto } from './dto/create-wish.dto';

@Controller('pet')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
@ApiTags('Pet')
export class PetController {
  constructor(private readonly petService: PetService) {}

  // ─── core ───────────────────────────────────────────────

  @Get()
  async getPet(@CurrentUser('id') userId: number) {
    const data = await this.petService.getPet(userId);
    return sendResponse('Pet retrieved', data);
  }

  @Put()
  async updatePet(@CurrentUser('id') userId: number, @Body() dto: UpdatePetDto) {
    const data = await this.petService.updatePet(userId, dto);
    return sendResponse('Pet updated', data);
  }

  @Post('feed')
  async feed(@CurrentUser('id') userId: number) {
    const data = await this.petService.feed(userId);
    return sendResponse('Pet fed', data);
  }

  @Post('play')
  async play(@CurrentUser('id') userId: number) {
    const data = await this.petService.play(userId);
    return sendResponse('Played with pet', data);
  }

  @Post('clean')
  async clean(@CurrentUser('id') userId: number) {
    const data = await this.petService.clean(userId);
    return sendResponse('Pet cleaned', data);
  }

  // ─── types ──────────────────────────────────────────────

  @Get('types')
  async getTypes() {
    const data = await this.petService.getTypes();
    return sendResponse('Pet types retrieved', data);
  }

  // ─── eggs ───────────────────────────────────────────────

  @Get('eggs')
  async getEggs(@CurrentUser('id') userId: number) {
    const data = await this.petService.getEggs(userId);
    return sendResponse('Eggs retrieved', data);
  }

  @Post('eggs/hatch/:id')
  async hatchEgg(@CurrentUser('id') userId: number, @Param('id', ParseIntPipe) id: number) {
    const data = await this.petService.hatchEgg(userId, id);
    return sendResponse('Egg hatched', data);
  }

  // ─── furniture ──────────────────────────────────────────

  @Get('furniture/shop')
  async getShop() {
    const data = await this.petService.getShop();
    return sendResponse('Shop retrieved', data);
  }

  @Get('furniture/owned')
  async getOwnedFurniture(@CurrentUser('id') userId: number) {
    const data = await this.petService.getOwnedFurniture(userId);
    return sendResponse('Owned furniture retrieved', data);
  }

  @Post('furniture/buy/:id')
  async buyFurniture(@CurrentUser('id') userId: number, @Param('id', ParseIntPipe) id: number) {
    const data = await this.petService.buyFurniture(userId, id);
    return sendResponse('Furniture purchased', data);
  }

  @Put('furniture/place/:id')
  async placeFurniture(
    @CurrentUser('id') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: PlaceFurnitureDto,
  ) {
    const data = await this.petService.placeFurniture(userId, id, dto);
    return sendResponse('Furniture placed', data);
  }

  // ─── adventures ─────────────────────────────────────────

  @Get('adventures')
  async getAdventures() {
    const data = await this.petService.getAdventures();
    return sendResponse('Adventures retrieved', data);
  }

  @Post('adventures/start/:id')
  async startAdventure(@CurrentUser('id') userId: number, @Param('id', ParseIntPipe) id: number) {
    const data = await this.petService.startAdventure(userId, id);
    return sendResponse('Adventure started', data);
  }

  @Post('adventures/claim/:id')
  async claimAdventure(@CurrentUser('id') userId: number, @Param('id', ParseIntPipe) id: number) {
    const data = await this.petService.claimAdventure(userId, id);
    return sendResponse('Adventure claimed', data);
  }

  // ─── wishes ─────────────────────────────────────────────

  @Get('wishes')
  async getWishes(@CurrentUser('id') userId: number) {
    const data = await this.petService.getWishes(userId);
    return sendResponse('Wishes retrieved', data);
  }

  @Post('wishes')
  async createWish(@CurrentUser('id') userId: number, @Body() dto: CreateWishDto) {
    const data = await this.petService.createWish(userId, dto);
    return sendResponse('Wish created', data);
  }

  @Delete('wishes/:id')
  async deleteWish(@CurrentUser('id') userId: number, @Param('id', ParseIntPipe) id: number) {
    const data = await this.petService.deleteWish(userId, id);
    return sendResponse('Wish deleted', data);
  }

  @Post('wishes/fulfill/:id')
  async fulfillWish(@CurrentUser('id') userId: number, @Param('id', ParseIntPipe) id: number) {
    const data = await this.petService.fulfillWish(userId, id);
    return sendResponse('Wish fulfilled', data);
  }

  // ─── check-in ───────────────────────────────────────────

  @Post('checkin')
  async checkin(@CurrentUser('id') userId: number) {
    const data = await this.petService.checkin(userId);
    return sendResponse('Checked in', data);
  }

  @Get('checkin')
  async getCheckinStatus(@CurrentUser('id') userId: number) {
    const data = await this.petService.getCheckinStatus(userId);
    return sendResponse('Check-in status', data);
  }

  // ─── spin ───────────────────────────────────────────────

  @Post('spin')
  async spin(@CurrentUser('id') userId: number) {
    const data = await this.petService.spin(userId);
    return sendResponse('Spin result', data);
  }

  // ─── collections ────────────────────────────────────────

  @Get('collections')
  async getCollections(@CurrentUser('id') userId: number) {
    const data = await this.petService.getCollections(userId);
    return sendResponse('Collections', data);
  }

  // ─── passport ───────────────────────────────────────────

  @Get('passport')
  async getPassport(@CurrentUser('id') userId: number) {
    const data = await this.petService.getPassport(userId);
    return sendResponse('Passport', data);
  }

  // ─── history ────────────────────────────────────────────

  @Get('history')
  async getHistory(
    @CurrentUser('id') userId: number,
    @Query('limit') limit?: string,
  ) {
    const data = await this.petService.getHistory(userId, limit ? +limit : 20);
    return sendResponse('Pet history', data);
  }

  // ─── level rewards ──────────────────────────────────────

  @Get('level-rewards')
  async getLevelRewards() {
    const data = await this.petService.getLevelRewards();
    return sendResponse('Level rewards', data);
  }
}
