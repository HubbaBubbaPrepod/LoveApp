package com.example.loveapp.di

import android.content.Context
import com.example.loveapp.data.LoveAppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Singleton
    @Provides
    fun provideLoveAppDatabase(
        @ApplicationContext context: Context
    ): LoveAppDatabase {
        return LoveAppDatabase.getDatabase(context)
    }

    @Singleton
    @Provides
    fun provideUserDao(database: LoveAppDatabase) = database.userDao()

    @Singleton
    @Provides
    fun provideNoteDao(database: LoveAppDatabase) = database.noteDao()

    @Singleton
    @Provides
    fun provideWishDao(database: LoveAppDatabase) = database.wishDao()

    @Singleton
    @Provides
    fun provideMoodEntryDao(database: LoveAppDatabase) = database.moodEntryDao()

    @Singleton
    @Provides
    fun provideActivityLogDao(database: LoveAppDatabase) = database.activityLogDao()

    @Singleton
    @Provides
    fun provideMenstrualCycleDao(database: LoveAppDatabase) = database.menstrualCycleDao()

    @Singleton
    @Provides
    fun provideCustomCalendarDao(database: LoveAppDatabase) = database.customCalendarDao()

    @Singleton
    @Provides
    fun provideCustomCalendarEventDao(database: LoveAppDatabase) = database.customCalendarEventDao()

    @Singleton
    @Provides
    fun provideRelationshipInfoDao(database: LoveAppDatabase) = database.relationshipInfoDao()
}
