import { Injectable } from '@nestjs/common';
import { DataSource } from 'typeorm';
import { CoupleService } from '../shared/couple.service';

@Injectable()
export class WidgetService {
  constructor(
    private readonly dataSource: DataSource,
    private readonly coupleService: CoupleService,
  ) {}

  async getSummary(userId: number) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const partnerId = await this.coupleService.getPartnerId(userId);

    // Partner info
    let partner = null;
    if (partnerId) {
      const [row] = await this.dataSource.query(
        `SELECT nickname, avatar_url FROM users WHERE id = $1`,
        [partnerId],
      );
      partner = row ?? null;
    }

    // Days together
    const [relRow] = await this.dataSource.query(
      `SELECT relationship_start_date as start_date FROM relationship_info WHERE couple_key = $1`,
      [coupleKey],
    );
    let daysTogether = 0;
    if (relRow?.start_date) {
      const start = new Date(relRow.start_date);
      const now = new Date();
      daysTogether = Math.floor((now.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
    }

    // Next memorial
    const memorials = await this.dataSource.query(
      `SELECT title, event_date FROM memorial_days
       WHERE couple_key = $1 AND event_date >= CURRENT_DATE
       ORDER BY event_date ASC LIMIT 1`,
      [coupleKey],
    );
    const nextMemorial = memorials?.[0] ?? null;

    // Spark streak
    const [streakRow] = await this.dataSource.query(
      `SELECT current_streak FROM spark_streaks WHERE couple_key = $1`,
      [coupleKey],
    );
    const streak = streakRow?.current_streak ?? 0;

    // Pet mood
    const [petRow] = await this.dataSource.query(
      `SELECT mood FROM pets WHERE couple_key = $1`,
      [coupleKey],
    );
    const petMood = petRow?.mood ?? null;

    return {
      partner,
      days_together: daysTogether,
      next_memorial: nextMemorial,
      streak,
      pet_mood: petMood,
    };
  }
}
