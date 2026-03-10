import { Processor, WorkerHost } from '@nestjs/bullmq';
import { Inject } from '@nestjs/common';
import { Job } from 'bullmq';
import { WINSTON_MODULE_PROVIDER } from 'nest-winston';
import { Logger } from 'winston';
import { FirebaseService } from '../../firebase/firebase.service';

@Processor('notifications')
export class NotificationProcessor extends WorkerHost {
  constructor(
    private readonly firebase: FirebaseService,
    @Inject(WINSTON_MODULE_PROVIDER) private readonly logger: Logger,
  ) {
    super();
  }

  async process(job: Job): Promise<any> {
    const { type, title, body, data } = job.data;

    const stringData: Record<string, string> = {};
    if (data) {
      for (const [k, v] of Object.entries(data)) {
        stringData[k] = String(v);
      }
    }

    try {
      if (type === 'push') {
        const { fcmToken } = job.data;
        await this.firebase.messaging.send({
          token: fcmToken,
          notification: { title, body },
          data: stringData,
          android: { priority: 'high' },
        });
        this.logger.info(`Push sent to ${fcmToken.substring(0, 10)}...`);
      } else if (type === 'multicast') {
        const { tokens } = job.data;
        await this.firebase.messaging.sendEachForMulticast({
          tokens,
          notification: { title, body },
          data: stringData,
        });
        this.logger.info(`Multicast sent to ${tokens.length} devices`);
      }
    } catch (err: any) {
      this.logger.error(`Notification failed: ${err.message}`);
      throw err;
    }
  }
}
