package com.example.loveapp.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.tencent.mmkv.MMKV
import com.example.loveapp.storage.MMKVManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides Phase-1 library singletons:
 * MMKV, Google Location, Google Places.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoreLibsModule {

    @Singleton
    @Provides
    fun provideMMKV(): MMKV = MMKVManager.defaultInstance()

    @Singleton
    @Provides
    fun provideFusedLocationClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @Singleton
    @Provides
    fun providePlacesClient(
        @ApplicationContext context: Context
    ): PlacesClient = Places.createClient(context)

    @Singleton
    @Provides
    fun provideGeofencingClient(
        @ApplicationContext context: Context
    ): GeofencingClient =
        LocationServices.getGeofencingClient(context)
}
