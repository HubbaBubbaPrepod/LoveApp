import { Module } from '@nestjs/common';
import { BullModule } from '@nestjs/bullmq';
import { ImageProcessor } from './processors/image.processor';
import { NotificationProcessor } from './processors/notification.processor';
import { QueueService } from './queue.service';
import { FirebaseModule } from '../firebase/firebase.module';

@Module({
  imports: [
    BullModule.registerQueue(
      { name: 'imageProcessing' },
      { name: 'notifications' },
    ),
    FirebaseModule,
  ],
  providers: [ImageProcessor, NotificationProcessor, QueueService],
  exports: [QueueService],
})
export class QueuesModule {}
