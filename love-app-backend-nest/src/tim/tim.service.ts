import { Injectable, ServiceUnavailableException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

const USER_SIG_EXPIRE = 604800; // 7 days

@Injectable()
export class TimService {
  private readonly sdkAppId: number;
  private readonly secretKey: string;

  constructor(private readonly config: ConfigService) {
    this.sdkAppId = Number(this.config.get('TIM_SDK_APP_ID')) || 0;
    this.secretKey = this.config.get('TIM_SECRET_KEY') || '';
  }

  async getUserSig(userId: number) {
    if (!this.sdkAppId || !this.secretKey) {
      throw new ServiceUnavailableException('TIM is not configured');
    }

    let TLSSigAPIv2: any;
    try {
      TLSSigAPIv2 = require('tls-sig-api-v2');
    } catch {
      throw new ServiceUnavailableException('tls-sig-api-v2 dependency is not installed');
    }

    const api = new TLSSigAPIv2.Api(this.sdkAppId, this.secretKey);
    const userSig = api.genSig(String(userId), USER_SIG_EXPIRE);

    return {
      sdkAppId: this.sdkAppId,
      userId: String(userId),
      userSig,
      expireIn: USER_SIG_EXPIRE,
    };
  }
}
