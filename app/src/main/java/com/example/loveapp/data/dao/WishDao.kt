package com.example.loveapp.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.loveapp.data.entity.Wish
import kotlinx.coroutines.flow.Flow

@Dao
interface WishDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWish(wish: Wish): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(wish: Wish): Long

    @Update
    suspend fun updateWish(wish: Wish)

    @Delete
    suspend fun deleteWish(wish: Wish)

    @Query("SELECT * FROM wishes WHERE id = :wishId AND deletedAt IS NULL")
    suspend fun getWishById(wishId: Int): Wish?

    @Query("SELECT * FROM wishes WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): Wish?

    @Query("SELECT * FROM wishes WHERE userId = :userId AND deletedAt IS NULL ORDER BY priority DESC, createdAt DESC")
    suspend fun getWishesByUser(userId: Int): List<Wish>

    @Query("SELECT * FROM wishes WHERE userId = :userId AND isCompleted = 0 AND deletedAt IS NULL ORDER BY priority DESC, createdAt DESC")
    suspend fun getActiveWishesByUser(userId: Int): List<Wish>

    @Query("SELECT * FROM wishes WHERE userId = :userId AND isCompleted = 1 AND deletedAt IS NULL")
    suspend fun getCompletedWishesByUser(userId: Int): List<Wish>

    @Query("SELECT * FROM wishes WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    suspend fun getAllWishes(): List<Wish>

    @Query("SELECT * FROM wishes WHERE syncPending = 1 AND deletedAt IS NULL")
    suspend fun getPendingSync(): List<Wish>

    @Query("SELECT * FROM wishes WHERE deletedAt IS NULL ORDER BY priority DESC, createdAt DESC")
    fun observeAllWishes(): Flow<List<Wish>>

    @Query("SELECT * FROM wishes WHERE userId = :userId AND isCompleted = 0 AND deletedAt IS NULL ORDER BY priority DESC, createdAt DESC")
    fun observeActiveWishesByUser(userId: Int): Flow<List<Wish>>

    /** Paging 3 – lazy-load all wishes. */
    @Query("SELECT * FROM wishes WHERE deletedAt IS NULL ORDER BY priority DESC, createdAt DESC")
    fun pagingSource(): PagingSource<Int, Wish>

    /** Paging 3 – search by title/description. */
    @Query("SELECT * FROM wishes WHERE deletedAt IS NULL AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') ORDER BY priority DESC, createdAt DESC")
    fun pagingSourceFiltered(query: String): PagingSource<Int, Wish>
}
