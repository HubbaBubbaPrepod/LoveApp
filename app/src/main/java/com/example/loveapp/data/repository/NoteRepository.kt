package com.example.loveapp.data.repository

import androidx.paging.PagingSource
import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.NoteRequest
import com.example.loveapp.data.api.models.NoteResponse
import com.example.loveapp.data.dao.NoteDao
import com.example.loveapp.data.dao.OutboxDao
import com.example.loveapp.data.entity.Note
import com.example.loveapp.data.entity.OutboxEntry
import com.example.loveapp.utils.DateUtils
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Room = Single Source of Truth.
 * Every mutation writes to Room first (optimistic), then tries REST.
 * If offline / failed → enqueues to outbox for background delivery.
 */
class NoteRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val noteDao: NoteDao,
    private val outboxDao: OutboxDao,
    private val authRepository: AuthRepository
) {
    private val gson = Gson()

    // ─── Live Flows from Room (drives UI) ────────────────────────────────
    fun observeAllNotes(): Flow<List<Note>> = noteDao.observeAllNotes()
    fun observeNotesByUser(userId: Int): Flow<List<Note>> = noteDao.observeNotesByUser(userId)

    /** PagingSource for Paging 3 – pass to [Pager] in the ViewModel. */
    fun pagingSource(query: String = ""): PagingSource<Int, Note> =
        if (query.isBlank()) noteDao.pagingSource()
        else noteDao.pagingSourceFiltered(query)

    // ─── Write: optimistic → REST → fallback to outbox ───────────────────
    suspend fun createNote(
        title: String, content: String, userId: Int,
        isPrivate: Boolean = false, tags: String = ""
    ): Result<Note> {
        val local = Note(title = title, content = content, userId = userId,
            isPrivate = isPrivate, tags = tags, syncPending = true)
        val localId = noteDao.insertNote(local).toInt()
        val saved = local.copy(id = localId)
        return try {
            val token = authRepository.getToken() ?: return enqueue("create", saved, localId)
            val resp = apiService.createNote("Bearer $token", NoteRequest(title, content, isPrivate, tags))
            if (resp.success && resp.data != null) {
                val synced = saved.copy(serverId = resp.data.id, syncPending = false)
                noteDao.upsert(synced); Result.success(synced)
            } else enqueue("create", saved, localId)
        } catch (e: Exception) { enqueue("create", saved, localId) }
    }

    suspend fun updateNote(serverId: Int, title: String, content: String,
                           isPrivate: Boolean, tags: String): Result<Note> {
        val ex = noteDao.getByServerId(serverId) ?: noteDao.getNoteById(serverId)
                 ?: return Result.failure(Exception("Note not found"))
        val updated = ex.copy(title = title, content = content, isPrivate = isPrivate,
            tags = tags, updatedAt = System.currentTimeMillis(), syncPending = true)
        noteDao.upsert(updated)
        return try {
            val token = authRepository.getToken() ?: return enqueue("update", updated, ex.id)
            val sId = updated.serverId ?: return enqueue("update", updated, ex.id)
            val resp = apiService.updateNote("Bearer $token", sId, NoteRequest(title, content, isPrivate, tags))
            if (resp.success) { noteDao.upsert(updated.copy(syncPending = false)); Result.success(updated.copy(syncPending = false)) }
            else enqueue("update", updated, ex.id)
        } catch (e: Exception) { enqueue("update", updated, ex.id) }
    }

    suspend fun deleteNote(serverId: Int): Result<Unit> {
        val ex = noteDao.getByServerId(serverId) ?: noteDao.getNoteById(serverId)
                 ?: return Result.success(Unit)
        val sd = ex.copy(deletedAt = System.currentTimeMillis(), syncPending = true)
        noteDao.upsert(sd)
        return try {
            val token = authRepository.getToken() ?: return enqueueDelete(sd, ex.id)
            val sId = sd.serverId ?: return enqueueDelete(sd, ex.id)
            apiService.deleteNote("Bearer $token", sId)
            noteDao.upsert(sd.copy(syncPending = false)); Result.success(Unit)
        } catch (e: Exception) { enqueueDelete(sd, ex.id) }
    }

    // ─── REST read (for initial load / refresh) ──────────────────────────
    suspend fun getNotes(page: Int = 1): Result<List<NoteResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getNotes("Bearer $token", page)
        if (response.success && response.data != null) Result.success(response.data.items)
        else Result.failure(Exception(response.message ?: "Failed to get notes"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getNoteById(id: Int): Result<NoteResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getNote("Bearer $token", id)
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message ?: "Failed to get note"))
    } catch (e: Exception) { Result.failure(e) }

    /** Pulls all server notes and upserts into Room. */
    suspend fun refreshFromServer() {
        val token = authRepository.getToken() ?: return
        val resp = apiService.getNotes("Bearer $token", 1)
        if (resp.success && resp.data != null) {
            resp.data.items.forEach { n ->
                val ex = n.id?.let { noteDao.getByServerId(it) }
                noteDao.upsert(Note(
                    id = ex?.id ?: 0, title = n.title ?: "", content = n.content ?: "",
                    userId = n.userId ?: 0, isPrivate = n.isPrivate ?: false, tags = n.tags ?: "",
                    createdAt = DateUtils.parseIsoTs(n.createdAt),
                    updatedAt = DateUtils.parseIsoTs(n.updatedAt),
                    displayName = n.displayName,
                    userAvatar  = n.userAvatar,
                    serverId = n.id, syncPending = false))
            }
        }
    }

    suspend fun getCurrentUserId(): Int? = authRepository.getUserId()

    // ─── Outbox helpers ───────────────────────────────────────────────────
    private suspend fun enqueue(action: String, note: Note, localId: Int): Result<Note> {
        outboxDao.enqueue(OutboxEntry(entityType = "note", action = action,
            payload = gson.toJson(note), localId = localId, serverId = note.serverId))
        return Result.success(note)
    }
    private suspend fun enqueueDelete(note: Note, localId: Int): Result<Unit> {
        outboxDao.enqueue(OutboxEntry(entityType = "note", action = "delete",
            payload = gson.toJson(mapOf("id" to note.serverId)), localId = localId, serverId = note.serverId))
        return Result.success(Unit)
    }
}
