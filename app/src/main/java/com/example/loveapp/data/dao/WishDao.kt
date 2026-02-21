package com.example.loveapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.loveapp.data.entity.Wish

@Dao
interface WishDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWish(wish: Wish): Long

    @Update
    suspend fun updateWish(wish: Wish)

    @Delete
    suspend fun deleteWish(wish: Wish)

    @Query("SELECT * FROM wishes WHERE id = :wishId")
    suspend fun getWishById(wishId: Int): Wish?

    @Query("SELECT * FROM wishes WHERE userId = :userId ORDER BY priority DESC, createdAt DESC")
    suspend fun getWishesByUser(userId: Int): List<Wish>

    @Query("SELECT * FROM wishes WHERE userId = :userId AND isCompleted = 0 ORDER BY priority DESC, createdAt DESC")
    suspend fun getActiveWishesByUser(userId: Int): List<Wish>

    @Query("SELECT * FROM wishes WHERE userId = :userId AND isCompleted = 1")
    suspend fun getCompletedWishesByUser(userId: Int): List<Wish>

    @Query("SELECT * FROM wishes ORDER BY createdAt DESC")
    suspend fun getAllWishes(): List<Wish>
}
