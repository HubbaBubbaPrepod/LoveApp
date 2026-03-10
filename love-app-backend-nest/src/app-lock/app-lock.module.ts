import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AppLockSetting } from './entities/app-lock-setting.entity';
import { AppLockController } from './app-lock.controller';
import { AppLockService } from './app-lock.service';

@Module({
  imports: [TypeOrmModule.forFeature([AppLockSetting])],
  controllers: [AppLockController],
  providers: [AppLockService],
  exports: [AppLockService],
})
export class AppLockModule {}
