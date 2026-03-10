package com.example.loveapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.loveapp.data.entity.AppLockSetting
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLockSettingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: AppLockSetting): Long

    @Query("SELECT * FROM app_lock_settings WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: Int): AppLockSetting?

    @Query("SELECT * FROM app_lock_settings WHERE userId = :userId LIMIT 1")
    fun observeByUserId(userId: Int): Flow<AppLockSetting?>

    @Query("DELETE FROM app_lock_settings WHERE userId = :userId")
    suspend fun deleteByUserId(userId: Int)
}
