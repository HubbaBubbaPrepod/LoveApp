import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ChatSetting } from './entities/chat-setting.entity';
import { ChatSettingsController } from './chat-settings.controller';
import { ChatSettingsService } from './chat-settings.service';

@Module({
  imports: [TypeOrmModule.forFeature([ChatSetting])],
  controllers: [ChatSettingsController],
  providers: [ChatSettingsService],
  exports: [ChatSettingsService],
})
export class ChatSettingsModule {}
