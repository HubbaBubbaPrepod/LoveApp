import { Injectable } from '@nestjs/common';
import { InjectQueue } from '@nestjs/bullmq';
import { Queue } from 'bullmq';

@Injectable()
export class QueueService {
  constructor(
    @InjectQueue('imageProcessing') private readonly imageQueue: Queue,
    @InjectQueue('notifications') private readonly notifQueue: Queue,
  ) {}

  async addImageJob(data: {
    inputPath: string;
    outputPath: string;
    width?: number;
    quality?: number;
    format?: string;
  }) {
    return this.imageQueue.add('process', data, {
      attempts: 3,
      backoff: { type: 'exponential', delay: 2000 },
      removeOnComplete: 100,
      removeOnFail: 200,
    });
  }

  async addPushNotification(data: {
    type: 'push';
    fcmToken: string;
    title: string;
    body: string;
    data?: Record<string, string>;
  }) {
    return this.notifQueue.add('send', data, {
      attempts: 3,
      backoff: { type: 'exponential', delay: 3000 },
      removeOnComplete: 50,
      removeOnFail: 100,
    });
  }

  async addMulticastNotification(data: {
    type: 'multicast';
    tokens: string[];
    title: string;
    body: string;
    data?: Record<string, string>;
  }) {
    return this.notifQueue.add('send', data, {
      attempts: 3,
      backoff: { type: 'exponential', delay: 3000 },
      removeOnComplete: 50,
      removeOnFail: 100,
    });
  }
}
