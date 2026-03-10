import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { LoveLetter } from './entities/love-letter.entity';
import { LettersController } from './letters.controller';
import { LettersService } from './letters.service';

@Module({
  imports: [TypeOrmModule.forFeature([LoveLetter])],
  controllers: [LettersController],
  providers: [LettersService],
  exports: [LettersService],
})
export class LettersModule {}
