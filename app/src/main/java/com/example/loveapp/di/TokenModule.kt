package com.example.loveapp.di

import android.content.Context
import com.example.loveapp.utils.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TokenModule {
    
    @Singleton
    @Provides
    fun provideTokenManager(
        @ApplicationContext context: Context
    ): TokenManager {
        return TokenManager.getInstance(context)
    }
}

