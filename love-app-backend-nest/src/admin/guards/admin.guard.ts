import {
  Injectable,
  CanActivate,
  ExecutionContext,
  UnauthorizedException,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';

@Injectable()
export class AdminGuard implements CanActivate {
  constructor(
    private readonly jwtService: JwtService,
    private readonly config: ConfigService,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest();
    const authHeader = request.headers.authorization;
    if (!authHeader?.startsWith('Bearer ')) {
      throw new UnauthorizedException('Missing admin token');
    }

    const token = authHeader.slice(7);
    const secret =
      this.config.get<string>('ADMIN_JWT_SECRET') ||
      this.config.get<string>('JWT_SECRET') + '_admin_panel';

    try {
      const payload = await this.jwtService.verifyAsync(token, { secret });
      if (!['admin', 'superadmin'].includes(payload.role)) {
        throw new UnauthorizedException('Insufficient privileges');
      }
      request.user = payload;
      return true;
    } catch (err) {
      if (err instanceof UnauthorizedException) throw err;
      throw new UnauthorizedException('Invalid admin token');
    }
  }
}
