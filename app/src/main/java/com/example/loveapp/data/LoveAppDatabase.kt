package com.example.loveapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.loveapp.data.dao.ActivityLogDao
import com.example.loveapp.data.dao.CustomCalendarDao
import com.example.loveapp.data.dao.CustomCalendarEventDao
import com.example.loveapp.data.dao.MenstrualCycleDao
import com.example.loveapp.data.dao.MoodEntryDao
import com.example.loveapp.data.dao.NoteDao
import com.example.loveapp.data.dao.OutboxDao
import com.example.loveapp.data.dao.RelationshipInfoDao
import com.example.loveapp.data.dao.UserDao
import com.example.loveapp.data.dao.WishDao
import com.example.loveapp.data.entity.ActivityLog
import com.example.loveapp.data.entity.CustomCalendar
import com.example.loveapp.data.entity.CustomCalendarEvent
import com.example.loveapp.data.entity.MenstrualCycleEntry
import com.example.loveapp.data.entity.MoodEntry
import com.example.loveapp.data.entity.Note
import com.example.loveapp.data.entity.OutboxEntry
import com.example.loveapp.data.entity.RelationshipInfo
import com.example.loveapp.data.entity.User
import com.example.loveapp.data.entity.Wish

@Database(
    entities = [
        User::class,
        Note::class,
        Wish::class,
        MoodEntry::class,
        ActivityLog::class,
        MenstrualCycleEntry::class,
        CustomCalendar::class,
        CustomCalendarEvent::class,
        RelationshipInfo::class,
        OutboxEntry::class          // offline sync queue
    ],
    version = 5,
    exportSchema = false
)
abstract class LoveAppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun noteDao(): NoteDao
    abstract fun wishDao(): WishDao
    abstract fun moodEntryDao(): MoodEntryDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun menstrualCycleDao(): MenstrualCycleDao
    abstract fun customCalendarDao(): CustomCalendarDao
    abstract fun customCalendarEventDao(): CustomCalendarEventDao
    abstract fun relationshipInfoDao(): RelationshipInfoDao
    abstract fun outboxDao(): OutboxDao

    companion object {
        @Volatile
        private var Instance: LoveAppDatabase? = null

        /**
         * Migration 2 → 3:
         *  - Adds sync-metadata columns (serverId, syncPending, serverUpdatedAt, deletedAt)
         *    to all entity tables.
         *  - Creates the outbox table for offline sync.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            private val syncColumns = """
                ADD COLUMN serverId INTEGER DEFAULT NULL,
                ADD COLUMN syncPending INTEGER NOT NULL DEFAULT 1,
                ADD COLUMN serverUpdatedAt INTEGER DEFAULT NULL,
                ADD COLUMN deletedAt INTEGER DEFAULT NULL
            """.trimIndent()

            private val tables = listOf(
                "notes", "wishes", "mood_entries", "activity_logs",
                "menstrual_cycle", "custom_calendars",
                "custom_calendar_events", "relationship_info"
            )

            override fun migrate(db: SupportSQLiteDatabase) {
                // SQLite does not support multiple ADD COLUMN in one ALTER TABLE;
                // issue a separate statement per column per table.
                for (table in tables) {
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN serverId INTEGER DEFAULT NULL")
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN syncPending INTEGER NOT NULL DEFAULT 1")
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN serverUpdatedAt INTEGER DEFAULT NULL")
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN deletedAt INTEGER DEFAULT NULL")
                }

                // Outbox table for queuing offline mutations
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS outbox (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entityType TEXT NOT NULL,
                        action TEXT NOT NULL,
                        payload TEXT NOT NULL,
                        localId INTEGER NOT NULL,
                        serverId INTEGER DEFAULT NULL,
                        createdAt INTEGER NOT NULL,
                        retryCount INTEGER NOT NULL DEFAULT 0,
                        retryAfter INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_outbox_entity ON outbox (entityType, localId)")
            }
        }

        /**
         * Migration 3 → 4:
         *  Recreates the outbox table without SQLite DEFAULT clauses and without the
         *  extra index. The previous migration created DEFAULT 0 / DEFAULT NULL and an
         *  index that Room's schema validator does not expect (Room manages Kotlin-level
         *  defaults itself), causing an IllegalStateException on every DB open.
         *  Data is preserved via INSERT INTO ... SELECT.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS index_outbox_entity")
                db.execSQL("ALTER TABLE outbox RENAME TO outbox_old")
                db.execSQL("""
                    CREATE TABLE outbox (
                        id         INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entityType TEXT    NOT NULL,
                        action     TEXT    NOT NULL,
                        payload    TEXT    NOT NULL,
                        localId    INTEGER NOT NULL,
                        serverId   INTEGER,
                        createdAt  INTEGER NOT NULL,
                        retryCount INTEGER NOT NULL,
                        retryAfter INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO outbox (id, entityType, action, payload, localId, serverId, createdAt, retryCount, retryAfter)
                    SELECT              id, entityType, action, payload, localId, serverId, createdAt, retryCount, retryAfter
                    FROM outbox_old
                """.trimIndent())
                db.execSQL("DROP TABLE outbox_old")
            }
        }

        /**
         * Migration 4 → 5:
         *  Adds displayName / userAvatar to notes (partner display in tile).
         *  Adds isPrivate, emoji, displayName, userAvatar to wishes.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN displayName TEXT")
                db.execSQL("ALTER TABLE notes ADD COLUMN userAvatar TEXT")
                db.execSQL("ALTER TABLE wishes ADD COLUMN isPrivate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE wishes ADD COLUMN emoji TEXT")
                db.execSQL("ALTER TABLE wishes ADD COLUMN displayName TEXT")
                db.execSQL("ALTER TABLE wishes ADD COLUMN userAvatar TEXT")
            }
        }

        fun getDatabase(context: Context): LoveAppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    LoveAppDatabase::class.java,
                    "loveapp_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration() // safety net for dev builds
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
