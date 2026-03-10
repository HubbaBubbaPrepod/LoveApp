import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, DataSource, IsNull, MoreThanOrEqual } from 'typeorm';
import { AchievementDefinition } from './entities/achievement-definition.entity';
import { UserAchievement } from './entities/user-achievement.entity';
import { CoupleAwareService } from '../shared/couple-aware.service';
import { CoupleService } from '../shared/couple.service';
import { Note } from '../notes/entities/note.entity.js';
import { Wish } from '../wishes/entities/wish.entity.js';
import { MoodEntry } from '../moods/entities/mood-entry.entity.js';
import { GameSession } from '../games/entities/game-session.entity.js';
import { LoveLetter } from '../letters/entities/love-letter.entity.js';
import { BucketListItem } from '../bucketlist/entities/bucket-list-item.entity.js';
import { CustomCalendarEvent } from '../calendars/entities/custom-calendar-event.entity.js';
import { GalleryPhoto } from '../gallery/entities/gallery-photo.entity.js';
import { ArtCanvas } from '../art/entities/art-canvas.entity.js';
import { ActivityLog } from '../activities/entities/activity-log.entity.js';
import { ChatMessage } from '../chat/entities/chat-message.entity.js';
import { MenstrualCycle } from '../cycles/entities/menstrual-cycle.entity.js';
import { Pet } from '../pet/entities/pet.entity.js';
import { RelationshipInfo } from '../relationship/entities/relationship-info.entity.js';

/* ── simple counter helper ────────────────────────────── */
const cnt = (repo: Repository<any>, where: Record<string, any>) =>
  repo.count({ where });

@Injectable()
export class AchievementsService extends CoupleAwareService {
  constructor(
    @InjectRepository(AchievementDefinition)
    private readonly defRepo: Repository<AchievementDefinition>,
    @InjectRepository(UserAchievement)
    private readonly userAchRepo: Repository<UserAchievement>,
    coupleService: CoupleService,
    private readonly dataSource: DataSource,
  ) {
    super(coupleService);
  }

  async getAll(userId: number) {
    const definitions = await this.defRepo
      .createQueryBuilder('d')
      .leftJoinAndMapOne(
        'd.user_unlock',
        UserAchievement,
        'ua',
        'ua.achievement_id = d.id AND ua.user_id = :userId',
        { userId },
      )
      .orderBy('d.sort_order', 'ASC')
      .getMany();

    return definitions.map((d: any) => ({
      ...d,
      unlocked: !!d.user_unlock,
      unlocked_at: d.user_unlock?.unlocked_at || null,
      user_unlock: undefined,
    }));
  }

  async getProgress(userId: number) {
    const coupleKey = await this.coupleKey(userId);
    const ds = this.dataSource;

    const noteRepo      = ds.getRepository(Note);
    const wishRepo      = ds.getRepository(Wish);
    const moodRepo      = ds.getRepository(MoodEntry);
    const gameRepo      = ds.getRepository(GameSession);
    const letterRepo    = ds.getRepository(LoveLetter);
    const bucketRepo    = ds.getRepository(BucketListItem);
    const calendarRepo  = ds.getRepository(CustomCalendarEvent);
    const galleryRepo   = ds.getRepository(GalleryPhoto);
    const artRepo       = ds.getRepository(ArtCanvas);
    const activityRepo  = ds.getRepository(ActivityLog);
    const chatRepo      = ds.getRepository(ChatMessage);
    const cycleRepo     = ds.getRepository(MenstrualCycle);
    const petRepo       = ds.getRepository(Pet);

    const [
      notesCount,
      wishesCount,
      moodCount,
      gamesPlayed,
      lettersCount,
      bucketCount,
      bucketCompleted,
      calendarCount,
      galleryCount,
      artCount,
      activitiesCount,
      chatMessages,
      moodStreak,
      daysConnected,
      gamesWon,
      achievementsUnlocked,
      notesShared,
      wishesCompleted,
      cyclesLogged,
      petLevel,
    ] = await Promise.all([
      cnt(noteRepo,     { user_id: userId, deleted_at: IsNull() }),
      cnt(wishRepo,     { user_id: userId, deleted_at: IsNull() }),
      cnt(moodRepo,     { user_id: userId }),
      cnt(gameRepo,     { couple_key: coupleKey, status: 'finished' }),
      cnt(letterRepo,   { sender_id: userId, deleted_at: IsNull() }),
      cnt(bucketRepo,   { couple_key: coupleKey, deleted_at: IsNull() }),
      cnt(bucketRepo,   { couple_key: coupleKey, is_completed: true, deleted_at: IsNull() }),
      cnt(calendarRepo, { couple_key: coupleKey }),
      cnt(galleryRepo,  { couple_key: coupleKey, deleted_at: IsNull() }),
      cnt(artRepo,      { couple_key: coupleKey }),
      cnt(activityRepo, { user_id: userId }),
      cnt(chatRepo,     { couple_key: coupleKey }),
      /* mood_streak – distinct calendar days with mood entries */
      moodRepo.createQueryBuilder('m')
        .select('COUNT(DISTINCT DATE(m.created_at))', 'c')
        .where('m.user_id = :userId', { userId })
        .getRawOne().then(r => +(r?.c ?? 0)),
      /* days_connected – days since relationship start_date */
      ds.getRepository(RelationshipInfo)
        .createQueryBuilder('r')
        .select("COALESCE(EXTRACT(DAY FROM NOW() - MIN(r.start_date::timestamp)), 0)::int", 'c')
        .where('r.user_id = :userId', { userId })
        .getRawOne().then(r => +(r?.c ?? 0)),
      cnt(gameRepo,     { couple_key: coupleKey, status: 'finished', compatibility_score: MoreThanOrEqual(80) }),
      cnt(this.userAchRepo, { user_id: userId }),
      cnt(noteRepo,     { user_id: userId, is_private: false, deleted_at: IsNull() }),
      cnt(wishRepo,     { user_id: userId, is_completed: true, deleted_at: IsNull() }),
      cnt(cycleRepo,    { user_id: userId }),
      /* pet_level – highest pet level for the couple */
      petRepo.createQueryBuilder('p')
        .select('COALESCE(MAX(p.level), 0)', 'c')
        .where('p.couple_key = :coupleKey', { coupleKey })
        .getRawOne().then(r => +(r?.c ?? 0)),
    ]);

    return {
      total_notes: notesCount,
      total_wishes: wishesCount,
      total_moods: moodCount,
      mood_streak: moodStreak,
      games_played: gamesPlayed,
      games_won: gamesWon,
      total_letters: lettersCount,
      total_bucket_items: bucketCount,
      bucket_completed: bucketCompleted,
      total_calendar_events: calendarCount,
      total_gallery_items: galleryCount,
      total_art: artCount,
      total_activities: activitiesCount,
      total_chat_messages: chatMessages,
      days_connected: daysConnected,
      achievements_unlocked: achievementsUnlocked,
      notes_shared: notesShared,
      wishes_completed: wishesCompleted,
      cycles_logged: cyclesLogged,
      pet_level: petLevel,
    };
  }

  async check(userId: number) {
    const progress = await this.getProgress(userId);
    const coupleKey = await this.coupleKey(userId);

    const definitions = await this.defRepo.find();
    const existing = await this.userAchRepo.find({ where: { user_id: userId } });
    const unlockedIds = new Set(existing.map((e) => e.achievement_id));

    const progressMap: Record<string, number> = {
      total_notes: progress.total_notes,
      total_wishes: progress.total_wishes,
      total_moods: progress.total_moods,
      mood_streak: progress.mood_streak,
      games_played: progress.games_played,
      games_won: progress.games_won,
      total_letters: progress.total_letters,
      total_bucket_items: progress.total_bucket_items,
      bucket_completed: progress.bucket_completed,
      total_calendar_events: progress.total_calendar_events,
      total_gallery_items: progress.total_gallery_items,
      total_art: progress.total_art,
      total_activities: progress.total_activities,
      total_chat_messages: progress.total_chat_messages,
      days_connected: progress.days_connected,
      achievements_unlocked: progress.achievements_unlocked,
      notes_shared: progress.notes_shared,
      wishes_completed: progress.wishes_completed,
      cycles_logged: progress.cycles_logged,
      pet_level: progress.pet_level,
    };

    /* batch-save new unlocks in a single transaction */
    const toUnlock = definitions.filter(
      (def) => !unlockedIds.has(def.id) && (progressMap[def.code] ?? 0) >= def.target_value,
    );

    let newUnlocks: UserAchievement[] = [];
    if (toUnlock.length > 0) {
      newUnlocks = await this.dataSource.transaction(async (manager) => {
        const repo = manager.getRepository(UserAchievement);
        const entities = toUnlock.map((def) =>
          repo.create({ user_id: userId, achievement_id: def.id, couple_key: coupleKey }),
        );
        return repo.save(entities);
      });

      await this.broadcast(userId, 'achievement', 'unlock', newUnlocks);
    }

    return { checked: definitions.length, new_unlocks: newUnlocks.length, unlocks: newUnlocks };
  }
}
