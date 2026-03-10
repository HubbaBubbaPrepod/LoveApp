import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { CustomCalendar } from './entities/custom-calendar.entity';
import { CustomCalendarEvent } from './entities/custom-calendar-event.entity';
import { CalendarsController } from './calendars.controller';
import { CalendarsService } from './calendars.service';

@Module({
  imports: [TypeOrmModule.forFeature([CustomCalendar, CustomCalendarEvent])],
  controllers: [CalendarsController],
  providers: [CalendarsService],
  exports: [CalendarsService],
})
export class CalendarsModule {}
