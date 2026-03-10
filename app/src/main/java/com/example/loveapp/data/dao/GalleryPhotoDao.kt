package com.example.loveapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.loveapp.data.entity.GalleryPhoto
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryPhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(photo: GalleryPhoto): Long

    @Query("SELECT * FROM gallery_photos WHERE coupleKey = :coupleKey AND deletedAt IS NULL ORDER BY timestamp DESC")
    fun observePhotos(coupleKey: String): Flow<List<GalleryPhoto>>

    @Query("SELECT * FROM gallery_photos WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): GalleryPhoto?

    @Query("SELECT * FROM gallery_photos WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: Int): GalleryPhoto?

    @Query("SELECT COUNT(*) FROM gallery_photos WHERE coupleKey = :coupleKey AND deletedAt IS NULL")
    fun observeCount(coupleKey: String): Flow<Int>

    @Query("UPDATE gallery_photos SET deletedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Int, now: Long = System.currentTimeMillis())
}
