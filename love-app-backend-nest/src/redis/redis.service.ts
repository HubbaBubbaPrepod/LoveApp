import { Injectable, Inject } from '@nestjs/common';
import Redis from 'ioredis';

@Injectable()
export class RedisService {
  constructor(@Inject('REDIS_CLIENT') private readonly client: Redis) {}

  async get<T = any>(key: string): Promise<T | null> {
    const val = await this.client.get(key);
    if (!val) return null;
    try {
      return JSON.parse(val);
    } catch {
      return val as unknown as T;
    }
  }

  async set(key: string, value: any, ttl = 300): Promise<void> {
    await this.client.set(key, JSON.stringify(value), 'EX', ttl);
  }

  async del(key: string): Promise<void> {
    await this.client.del(key);
  }

  async publish(channel: string, message: any): Promise<void> {
    await this.client.publish(channel, JSON.stringify(message));
  }

  getClient(): Redis {
    return this.client;
  }
}
