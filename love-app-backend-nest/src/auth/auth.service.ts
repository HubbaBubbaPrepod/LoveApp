import {
  Injectable,
  BadRequestException,
  UnauthorizedException,
  ConflictException,
  Inject,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as bcrypt from 'bcryptjs';
import * as crypto from 'crypto';
import { WINSTON_MODULE_PROVIDER } from 'nest-winston';
import { Logger } from 'winston';
import { User } from '../users/entities/user.entity';
import { RefreshToken } from './entities/refresh-token.entity';
import { FcmToken } from './entities/fcm-token.entity';
import { RedisService } from '../redis/redis.service';
import {
  SignupDto,
  LoginDto,
  GoogleAuthDto,
  FcmTokenDto,
  SetupProfileDto,
  UpdateProfileDto,
} from './dto/auth.dto';

@Injectable()
export class AuthService {
  constructor(
    @InjectRepository(User) private readonly userRepo: Repository<User>,
    @InjectRepository(RefreshToken)
    private readonly refreshRepo: Repository<RefreshToken>,
    @InjectRepository(FcmToken)
    private readonly fcmRepo: Repository<FcmToken>,
    private readonly jwt: JwtService,
    private readonly config: ConfigService,
    private readonly redis: RedisService,
    @Inject(WINSTON_MODULE_PROVIDER) private readonly logger: Logger,
  ) {}

  private generateAccessToken(userId: number): string {
    return this.jwt.sign(
      { userId },
      { expiresIn: this.config.get('JWT_ACCESS_EXPIRES', '15m') },
    );
  }

  private async generateRefreshToken(userId: number): Promise<string> {
    const token = crypto.randomBytes(64).toString('hex');
    const expiresIn = this.config.get('JWT_REFRESH_EXPIRES', '30d');
    const days = parseInt(expiresIn) || 30;
    const expires_at = new Date(Date.now() + days * 24 * 60 * 60 * 1000);

    await this.refreshRepo.save({
      user_id: userId,
      token,
      expires_at,
    });

    return token;
  }

  async signup(dto: SignupDto) {
    const existing = await this.userRepo.findOne({
      where: [{ email: dto.email }, { username: dto.username }],
    });
    if (existing) throw new ConflictException('User already exists');

    const password_hash = await bcrypt.hash(dto.password, 10);
    const user = await this.userRepo.save({
      username: dto.username,
      email: dto.email,
      password_hash,
      display_name: dto.display_name || dto.username,
      gender: dto.gender,
    });

    const accessToken = this.generateAccessToken(user.id);
    const refreshToken = await this.generateRefreshToken(user.id);

    return {
      user: { id: user.id, username: user.username, email: user.email, display_name: user.display_name },
      accessToken,
      refreshToken,
    };
  }

  async login(dto: LoginDto) {
    const user = await this.userRepo
      .createQueryBuilder('u')
      .addSelect('u.password_hash')
      .where('u.email = :email', { email: dto.email })
      .getOne();

    if (!user || !user.password_hash) {
      throw new UnauthorizedException('Invalid credentials');
    }

    const valid = await bcrypt.compare(dto.password, user.password_hash);
    if (!valid) throw new UnauthorizedException('Invalid credentials');

    const accessToken = this.generateAccessToken(user.id);
    const refreshToken = await this.generateRefreshToken(user.id);

    return {
      user: {
        id: user.id,
        username: user.username,
        email: user.email,
        display_name: user.display_name,
        gender: user.gender,
        profile_image: user.profile_image,
        is_premium: user.is_premium,
      },
      accessToken,
      refreshToken,
    };
  }

  async refresh(token: string) {
    const stored = await this.refreshRepo.findOne({ where: { token } });
    if (!stored || new Date(stored.expires_at) < new Date()) {
      throw new UnauthorizedException('Invalid refresh token');
    }

    await this.refreshRepo.delete(stored.id);

    const accessToken = this.generateAccessToken(stored.user_id);
    const refreshToken = await this.generateRefreshToken(stored.user_id);

    return { accessToken, refreshToken };
  }

  async logout(userId: number) {
    await this.refreshRepo.delete({ user_id: userId });
    return { message: 'Logged out' };
  }

  async getProfile(userId: number) {
    const cached = await this.redis.get(`profile:${userId}`);
    if (cached) return cached;

    const user = await this.userRepo.findOne({ where: { id: userId } });
    if (!user) throw new BadRequestException('User not found');

    const profile = {
      id: user.id,
      username: user.username,
      email: user.email,
      display_name: user.display_name,
      profile_image: user.profile_image,
      avatar_url: user.avatar_url,
      gender: user.gender,
      is_premium: user.is_premium,
      created_at: user.created_at,
    };

    await this.redis.set(`profile:${userId}`, profile, 300);
    return profile;
  }

  async updateProfile(userId: number, dto: UpdateProfileDto) {
    await this.userRepo.update(userId, { ...dto, updated_at: new Date() });
    await this.redis.del(`profile:${userId}`);
    return this.getProfile(userId);
  }

  async setupProfile(userId: number, dto: SetupProfileDto) {
    if (!dto.gender) throw new BadRequestException('Gender is required');
    await this.userRepo.update(userId, {
      gender: dto.gender,
      display_name: dto.display_name,
      updated_at: new Date(),
    });
    await this.redis.del(`profile:${userId}`);
    return this.getProfile(userId);
  }

  async googleAuth(dto: GoogleAuthDto) {
    const webClientId = this.config.get('GOOGLE_WEB_CLIENT_ID');
    const androidClientId = this.config.get('GOOGLE_ANDROID_CLIENT_ID');

    let payload: any;
    try {
      const res = await fetch(
        `https://oauth2.googleapis.com/tokeninfo?id_token=${encodeURIComponent(dto.idToken)}`,
      );
      payload = await res.json();
    } catch {
      throw new UnauthorizedException('Failed to verify Google token');
    }

    if (
      payload.aud !== webClientId &&
      payload.aud !== androidClientId
    ) {
      throw new UnauthorizedException('Invalid Google token audience');
    }

    const googleId = payload.sub;
    const email = payload.email;
    const name = payload.name || payload.email;
    const picture = payload.picture;

    let user = await this.userRepo.findOne({
      where: [{ google_id: googleId }, { email }],
    });

    if (!user) {
      user = await this.userRepo.save({
        google_id: googleId,
        email,
        display_name: name,
        profile_image: picture,
        username: email.split('@')[0],
      });
    } else if (!user.google_id) {
      await this.userRepo.update(user.id, { google_id: googleId });
    }

    const accessToken = this.generateAccessToken(user.id);
    const refreshToken = await this.generateRefreshToken(user.id);

    return {
      user: {
        id: user.id,
        username: user.username,
        email: user.email,
        display_name: user.display_name,
        profile_image: user.profile_image,
        gender: user.gender,
        is_premium: user.is_premium,
      },
      accessToken,
      refreshToken,
    };
  }

  async upsertFcmToken(userId: number, dto: FcmTokenDto) {
    const existing = await this.fcmRepo.findOne({
      where: { user_id: userId },
    });

    if (existing) {
      await this.fcmRepo.update(existing.user_id, {
        fcm_token: dto.fcm_token,
        device_id: dto.device_id,
      });
    } else {
      await this.fcmRepo.save({
        user_id: userId,
        fcm_token: dto.fcm_token,
        device_id: dto.device_id,
      });
    }

    return { message: 'FCM token saved' };
  }
}
