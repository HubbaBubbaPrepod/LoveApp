import { Injectable } from '@nestjs/common';
import { DataSource } from 'typeorm';
import { UpdateChatSettingsDto } from './dto/update-chat-settings.dto';

const WALLPAPERS = [
  { id: 'stars', name: 'Звёзды', url: '/wallpapers/stars.jpg' },
  { id: 'hearts', name: 'Сердечки', url: '/wallpapers/hearts.jpg' },
  { id: 'sunset', name: 'Закат', url: '/wallpapers/sunset.jpg' },
  { id: 'flowers', name: 'Цветы', url: '/wallpapers/flowers.jpg' },
  { id: 'sky', name: 'Небо', url: '/wallpapers/sky.jpg' },
  { id: 'ocean', name: 'Океан', url: '/wallpapers/ocean.jpg' },
];

@Injectable()
export class ChatSettingsService {
  constructor(private readonly dataSource: DataSource) {}

  async get(userId: number) {
    const [row] = await this.dataSource.query(
      `SELECT * FROM chat_settings WHERE user_id = $1`,
      [userId],
    );
    return row ?? {
      user_id: userId,
      wallpaper_url: null,
      bubble_color: 'pink',
      bubble_shape: 'rounded',
    };
  }

  async update(userId: number, dto: UpdateChatSettingsDto) {
    await this.dataSource.query(
      `INSERT INTO chat_settings (user_id, wallpaper_url, bubble_color, bubble_shape)
       VALUES ($1, $2, $3, $4)
       ON CONFLICT (user_id) DO UPDATE SET
         wallpaper_url = COALESCE($2, chat_settings.wallpaper_url),
         bubble_color = COALESCE($3, chat_settings.bubble_color),
         bubble_shape = COALESCE($4, chat_settings.bubble_shape),
         updated_at = NOW()`,
      [userId, dto.wallpaper_url ?? null, dto.bubble_color ?? null, dto.bubble_shape ?? null],
    );
    return this.get(userId);
  }

  getWallpapers() {
    return WALLPAPERS;
  }
}
