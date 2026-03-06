package com.example.loveapp.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.loveapp.data.entity.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    /** Upsert from WebSocket / SyncWorker – replaces by primary key. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes WHERE id = :noteId AND deletedAt IS NULL")
    suspend fun getNoteById(noteId: Int): Note?

    @Query("SELECT * FROM notes WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): Note?

    @Query("SELECT * FROM notes WHERE userId = :userId AND deletedAt IS NULL ORDER BY createdAt DESC")
    suspend fun getNotesByUser(userId: Int): List<Note>

    @Query("SELECT * FROM notes WHERE userId = :userId AND isPrivate = 0 AND deletedAt IS NULL ORDER BY createdAt DESC")
    suspend fun getSharedNotesByUser(userId: Int): List<Note>

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    suspend fun getAllNotes(): List<Note>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' AND deletedAt IS NULL ORDER BY createdAt DESC")
    suspend fun searchNotes(query: String): List<Note>

    /** Pending outbox items for this entity type. */
    @Query("SELECT * FROM notes WHERE syncPending = 1 AND deletedAt IS NULL")
    suspend fun getPendingSync(): List<Note>

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE userId = :userId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeNotesByUser(userId: Int): Flow<List<Note>>

    /** Paging 3 – lazy-load notes ordered by creation date. */
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun pagingSource(): PagingSource<Int, Note>

    /** Paging 3 – search with keyword filter. */
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    fun pagingSourceFiltered(query: String): PagingSource<Int, Note>
}
