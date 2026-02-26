package com.example.loveapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.loveapp.data.dao.ActivityLogDao
import com.example.loveapp.data.dao.CustomCalendarDao
import com.example.loveapp.data.dao.CustomCalendarEventDao
import com.example.loveapp.data.dao.MenstrualCycleDao
import com.example.loveapp.data.dao.MoodEntryDao
import com.example.loveapp.data.dao.NoteDao
import com.example.loveapp.data.dao.RelationshipInfoDao
import com.example.loveapp.data.dao.UserDao
import com.example.loveapp.data.dao.WishDao
import com.example.loveapp.data.entity.ActivityLog
import com.example.loveapp.data.entity.CustomCalendar
import com.example.loveapp.data.entity.CustomCalendarEvent
import com.example.loveapp.data.entity.MenstrualCycleEntry
import com.example.loveapp.data.entity.MoodEntry
import com.example.loveapp.data.entity.Note
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
        RelationshipInfo::class
    ],
    version = 2,
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

    companion object {
        @Volatile
        private var Instance: LoveAppDatabase? = null

        fun getDatabase(context: Context): LoveAppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    LoveAppDatabase::class.java,
                    "loveapp_database"
                ).fallbackToDestructiveMigration()
                    .build().also { Instance = it }
            }
        }
    }
}
