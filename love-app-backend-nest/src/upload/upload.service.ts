import { Injectable, BadRequestException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { ConfigService } from '@nestjs/config';
import { RedisService } from '../redis/redis.service';
import { User } from '../users/entities/user.entity';
import * as fs from 'fs';
import * as path from 'path';
import { pipeline } from 'stream/promises';
import { randomUUID } from 'crypto';
import { FastifyRequest } from 'fastify';

@Injectable()
export class UploadService {
  private readonly storagePath: string;
  private readonly serverUrl: string;

  constructor(
    @InjectRepository(User)
    private readonly userRepo: Repository<User>,
    private readonly config: ConfigService,
    private readonly redis: RedisService,
  ) {
    this.storagePath = this.config.get<string>('STORAGE_PATH', './storage');
    this.serverUrl = this.config.get<string>('SERVER_URL', 'http://localhost:3005');
  }

  async uploadProfile(userId: number, req: FastifyRequest): Promise<{ url: string }> {
    const data = await req.file();
    if (!data) throw new BadRequestException('No file uploaded');

    const ext = path.extname(data.filename) || '.jpg';
    const filename = `profile_${userId}_${randomUUID()}${ext}`;
    const uploadDir = path.join(this.storagePath, 'uploads');

    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }

    const filePath = path.join(uploadDir, filename);
    await pipeline(data.file, fs.createWriteStream(filePath));

    const url = `${this.serverUrl}/uploads/${filename}`;

    await this.userRepo.update(userId, { profile_image: url });
    await this.redis.del(`user:${userId}`);

    return { url };
  }

  async uploadImage(userId: number, req: FastifyRequest): Promise<{ url: string }> {
    const data = await req.file();
    if (!data) throw new BadRequestException('No file uploaded');

    const ext = path.extname(data.filename) || '.jpg';
    const filename = `img_${userId}_${randomUUID()}${ext}`;
    const uploadDir = path.join(this.storagePath, 'uploads');

    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }

    const filePath = path.join(uploadDir, filename);
    await pipeline(data.file, fs.createWriteStream(filePath));

    return { url: `/uploads/${filename}` };
  }
}
