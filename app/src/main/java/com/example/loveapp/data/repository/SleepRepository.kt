package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.SleepEntryRequest
import com.example.loveapp.data.api.models.SleepStatsResponse
import com.example.loveapp.data.dao.OutboxDao
import com.example.loveapp.data.dao.SleepEntryDao
import com.example.loveapp.data.entity.OutboxEntry
import com.example.loveapp.data.entity.SleepEntry
import com.example.loveapp.utils.DateUtils
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SleepRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val sleepEntryDao: SleepEntryDao,
    private val outboxDao: OutboxDao,
    private val authRepository: AuthRepository
) {
    private val gson = Gson()

    fun observeByUser(userId: Int): Flow<List<SleepEntry>> =
        sleepEntryDao.observeByUser(userId)

    fun observeAll(): Flow<List<SleepEntry>> = sleepEntryDao.observeAll()

    suspend fun saveSleepEntry(
        date: String, bedtime: String?, wakeTime: String?,
        durationMinutes: Int?, quality: Int?, note: String = ""
    ): Result<SleepEntry> {
        val local = SleepEntry(userId = 0, date = date, bedtime = bedtime, wakeTime = wakeTime,
            durationMinutes = durationMinutes, quality = quality, note = note, syncPending = true)
        val localId = sleepEntryDao.upsert(local).toInt()
        val saved = local.copy(id = localId)
        return try {
            val token = authRepository.getToken() ?: return enqueue("create", saved, localId)
            val resp = apiService.createSleepEntry("Bearer $token",
                SleepEntryRequest(date, bedtime, wakeTime, durationMinutes, quality, note))
            if (resp.success && resp.data != null) {
                val synced = saved.copy(serverId = resp.data.id, userId = resp.data.userId, syncPending = false)
                sleepEntryDao.upsert(synced); Result.success(synced)
            } else enqueue("create", saved, localId)
        } catch (e: Exception) { enqueue("create", saved, localId) }
    }

    suspend fun deleteSleepEntry(localId: Int): Result<Unit> {
        val ex = sleepEntryDao.getById(localId) ?: return Result.success(Unit)
        val sd = ex.copy(deletedAt = System.currentTimeMillis(), syncPending = true)
        sleepEntryDao.upsert(sd)
        return try {
            val token = authRepository.getToken() ?: return Result.success(Unit)
            val sId = sd.serverId ?: return Result.success(Unit)
            apiService.deleteSleepEntry("Bearer $token", sId)
            Result.success(Unit)
        } catch (e: Exception) { Result.success(Unit) }
    }

    suspend fun getStats(days: Int = 7): Result<SleepStatsResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.getSleepStats("Bearer $token", days)
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun refreshFromServer() {
        val token = authRepository.getToken() ?: return
        try {
            val resp = apiService.getSleepEntries("Bearer $token", 1, 50)
            if (resp.success && resp.data != null) {
                resp.data.items.forEach { s ->
                    val ex = sleepEntryDao.getByServerId(s.id)
                    sleepEntryDao.upsert(SleepEntry(
                        id = ex?.id ?: 0, userId = s.userId, date = s.date,
                        bedtime = s.bedtime, wakeTime = s.wakeTime,
                        durationMinutes = s.durationMinutes, quality = s.quality,
                        note = s.note, serverId = s.id, syncPending = false
                    ))
                }
            }
        } catch (_: Exception) {}
    }

    private suspend fun enqueue(action: String, entry: SleepEntry, localId: Int): Result<SleepEntry> {
        outboxDao.enqueue(OutboxEntry(entityType = "sleep_entry", action = action,
            payload = gson.toJson(entry), localId = localId, serverId = entry.serverId))
        return Result.success(entry)
    }
}
