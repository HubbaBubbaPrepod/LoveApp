import { Injectable, ServiceUnavailableException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

interface TenorResult {
  id: string;
  title: string;
  url: string;
  gif: string;
  tinygif: string;
}

@Injectable()
export class GifService {
  private readonly apiKey: string;
  private readonly baseUrl = 'https://tenor.googleapis.com/v2';

  constructor(private readonly config: ConfigService) {
    this.apiKey = this.config.get('TENOR_API_KEY') || '';
  }

  private ensureConfigured() {
    if (!this.apiKey) {
      throw new ServiceUnavailableException('Tenor API key is not configured');
    }
  }

  private mapResults(results: any[]): TenorResult[] {
    return (results || []).map((r: any) => ({
      id: r.id,
      title: r.title || '',
      url: r.url || '',
      gif: r.media_formats?.gif?.url || '',
      tinygif: r.media_formats?.tinygif?.url || '',
    }));
  }

  async search(query: string, limit?: number, pos?: string) {
    this.ensureConfigured();
    const params = new URLSearchParams({
      key: this.apiKey,
      q: query,
      limit: String(Math.min(limit || 20, 50)),
      media_filter: 'gif,tinygif',
      locale: 'ru_RU',
    });
    if (pos) params.set('pos', pos);

    const response = await fetch(`${this.baseUrl}/search?${params}`);
    const data = await response.json();

    return {
      results: this.mapResults(data.results),
      next: data.next || null,
    };
  }

  async trending(limit?: number) {
    this.ensureConfigured();
    const params = new URLSearchParams({
      key: this.apiKey,
      limit: String(Math.min(limit || 20, 50)),
      media_filter: 'gif,tinygif',
      locale: 'ru_RU',
    });

    const response = await fetch(`${this.baseUrl}/featured?${params}`);
    const data = await response.json();

    return {
      results: this.mapResults(data.results),
      next: data.next || null,
    };
  }
}
