import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ChatMessage } from './entities/chat-message.entity';
import { StickerPack } from './entities/sticker-pack.entity';
import { Sticker } from './entities/sticker.entity';
import { UserStickerPack } from './entities/user-sticker-pack.entity';
import { MissYouCounter } from './entities/miss-you-counter.entity';
import { ChatController } from './chat.controller';
import { ChatService } from './chat.service';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      ChatMessage,
      StickerPack,
      Sticker,
      UserStickerPack,
      MissYouCounter,
    ]),
  ],
  controllers: [ChatController],
  providers: [ChatService],
  exports: [ChatService],
})
export class ChatModule {}
