package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.MoodRequest
import com.example.loveapp.data.api.models.MoodResponse
import com.example.loveapp.data.dao.MoodEntryDao
import com.example.loveapp.data.dao.OutboxDao
import com.example.loveapp.data.entity.MoodEntry
import com.example.loveapp.data.entity.OutboxEntry
import com.example.loveapp.utils.DateUtils
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MoodRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val moodEntryDao: MoodEntryDao,
    private val outboxDao: OutboxDao,
    private val authRepository: AuthRepository
) {
    private val gson = Gson()

    // ─── Live Flows ───────────────────────────────────────────────────────
    fun observeAllMoods(): Flow<List<MoodEntry>> = moodEntryDao.observeAllMoods()
    fun observeMoodsByUser(userId: Int): Flow<List<MoodEntry>> =
        moodEntryDao.observeMoodsByUser(userId)

    // ─── Write: Room-first ────────────────────────────────────────────────
    suspend fun createMood(moodType: String, date: String, note: String = ""): Result<MoodEntry> {
        val local = MoodEntry(moodType = moodType, date = date, note = note, userId = 0, syncPending = true)
        val localId = moodEntryDao.insertMoodEntry(local).toInt()
        val saved = local.copy(id = localId)
        return try {
            val token = authRepository.getToken() ?: return enqueue("create", saved, localId)
            val resp = apiService.createMood("Bearer $token", MoodRequest(moodType, date, note))
            if (resp.success && resp.data != null) {
                val synced = saved.copy(serverId = resp.data.id, syncPending = false)
                moodEntryDao.upsert(synced); Result.success(synced)
            } else enqueue("create", saved, localId)
        } catch (e: Exception) { enqueue("create", saved, localId) }
    }

    suspend fun updateMood(localId: Int, moodType: String, note: String = ""): Result<MoodEntry> {
        val ex = moodEntryDao.getMoodEntryById(localId) ?: return Result.failure(Exception("Not found"))
        val updated = ex.copy(moodType = moodType, note = note, syncPending = true)
        moodEntryDao.upsert(updated)
        return try {
            val token = authRepository.getToken() ?: return enqueue("update", updated, localId)
            val sId = updated.serverId ?: return enqueue("update", updated, localId)
            val resp = apiService.updateMood("Bearer $token", sId, MoodRequest(moodType, ex.date, note))
            if (resp.success) { moodEntryDao.upsert(updated.copy(syncPending = false)); Result.success(updated.copy(syncPending = false)) }
            else enqueue("update", updated, localId)
        } catch (e: Exception) { enqueue("update", updated, localId) }
    }

    suspend fun deleteMood(localId: Int): Result<Unit> {
        val ex = moodEntryDao.getMoodEntryById(localId) ?: return Result.success(Unit)
        val sd = ex.copy(deletedAt = System.currentTimeMillis(), syncPending = true)
        moodEntryDao.upsert(sd)
        return try {
            val token = authRepository.getToken() ?: return enqueueDelete(sd, localId)
            val sId = sd.serverId ?: return enqueueDelete(sd, localId)
            apiService.deleteMood("Bearer $token", sId)
            moodEntryDao.upsert(sd.copy(syncPending = false)); Result.success(Unit)
        } catch (e: Exception) { enqueueDelete(sd, localId) }
    }

    // ─── REST reads ───────────────────────────────────────────────────────
    suspend fun getMoods(date: String? = null, startDate: String? = null,
                         endDate: String? = null, page: Int = 1): Result<List<MoodResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getMoods("Bearer $token", date, startDate, endDate, page, 500)
        if (response.success && response.data != null) Result.success(response.data.items)
        else Result.failure(Exception(response.message ?: "Failed to get moods"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getPartnerMoods(date: String? = null, startDate: String? = null,
                                endDate: String? = null): Result<List<MoodResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getPartnerMoods("Bearer $token", date, startDate, endDate)
        if (response.success && response.data != null) Result.success(response.data.items)
        else Result.failure(Exception(response.message ?: "Failed to get partner moods"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun refreshFromServer() {
        val token = authRepository.getToken() ?: return
        val resp = apiService.getMoods("Bearer $token", null, null, null, 1, 500)
        if (resp.success && resp.data != null) {
            resp.data.items.forEach { m ->
                val ex = m.id?.let { moodEntryDao.getByServerId(it) }
                moodEntryDao.upsert(MoodEntry(id = ex?.id ?: 0, moodType = m.moodType ?: "",
                    date = m.date ?: "", note = m.note ?: "", userId = m.userId ?: 0,
                    timestamp = DateUtils.parseIsoTs(m.timestamp),
                    serverId = m.id, syncPending = false))
            }
        }
    }

    private suspend fun enqueue(action: String, mood: MoodEntry, localId: Int): Result<MoodEntry> {
        outboxDao.enqueue(OutboxEntry(entityType = "mood", action = action,
            payload = gson.toJson(mood), localId = localId, serverId = mood.serverId))
        return Result.success(mood)
    }
    private suspend fun enqueueDelete(mood: MoodEntry, localId: Int): Result<Unit> {
        outboxDao.enqueue(OutboxEntry(entityType = "mood", action = "delete",
            payload = gson.toJson(mapOf("id" to mood.serverId)), localId = localId, serverId = mood.serverId))
        return Result.success(Unit)
    }
}

