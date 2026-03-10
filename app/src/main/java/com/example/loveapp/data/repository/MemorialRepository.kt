package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.MemorialDayRequest
import com.example.loveapp.data.dao.MemorialDayDao
import com.example.loveapp.data.dao.OutboxDao
import com.example.loveapp.data.entity.MemorialDay
import com.example.loveapp.data.entity.OutboxEntry
import com.example.loveapp.utils.DateUtils
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MemorialRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val memorialDayDao: MemorialDayDao,
    private val outboxDao: OutboxDao,
    private val authRepository: AuthRepository
) {
    private val gson = Gson()

    fun observeAll(): Flow<List<MemorialDay>> = memorialDayDao.observeAll()

    suspend fun createMemorialDay(
        title: String, date: String, type: String = "custom",
        icon: String = "💕", colorHex: String = "#FF6B9D",
        repeatYearly: Boolean = true, reminderDays: Int = 1, note: String = ""
    ): Result<MemorialDay> {
        val local = MemorialDay(userId = 0, title = title, date = date, type = type,
            icon = icon, colorHex = colorHex, repeatYearly = repeatYearly,
            reminderDays = reminderDays, note = note, syncPending = true)
        val localId = memorialDayDao.upsert(local).toInt()
        val saved = local.copy(id = localId)
        return try {
            val token = authRepository.getToken() ?: return enqueue("create", saved, localId)
            val resp = apiService.createMemorialDay("Bearer $token",
                MemorialDayRequest(title, date, type, icon, colorHex, repeatYearly, reminderDays, note))
            if (resp.success && resp.data != null) {
                val synced = saved.copy(serverId = resp.data.id, userId = resp.data.userId, syncPending = false)
                memorialDayDao.upsert(synced); Result.success(synced)
            } else enqueue("create", saved, localId)
        } catch (e: Exception) { enqueue("create", saved, localId) }
    }

    suspend fun updateMemorialDay(localId: Int, title: String, date: String, type: String,
                                  icon: String, colorHex: String, repeatYearly: Boolean,
                                  reminderDays: Int, note: String): Result<MemorialDay> {
        val ex = memorialDayDao.getById(localId) ?: return Result.failure(Exception("Not found"))
        val updated = ex.copy(title = title, date = date, type = type, icon = icon,
            colorHex = colorHex, repeatYearly = repeatYearly, reminderDays = reminderDays,
            note = note, syncPending = true)
        memorialDayDao.upsert(updated)
        return try {
            val token = authRepository.getToken() ?: return enqueue("update", updated, localId)
            val sId = updated.serverId ?: return enqueue("update", updated, localId)
            val resp = apiService.updateMemorialDay("Bearer $token", sId,
                MemorialDayRequest(title, date, type, icon, colorHex, repeatYearly, reminderDays, note))
            if (resp.success) { memorialDayDao.upsert(updated.copy(syncPending = false)); Result.success(updated) }
            else enqueue("update", updated, localId)
        } catch (e: Exception) { enqueue("update", updated, localId) }
    }

    suspend fun deleteMemorialDay(localId: Int): Result<Unit> {
        val ex = memorialDayDao.getById(localId) ?: return Result.success(Unit)
        val sd = ex.copy(deletedAt = System.currentTimeMillis(), syncPending = true)
        memorialDayDao.upsert(sd)
        return try {
            val token = authRepository.getToken() ?: return Result.success(Unit)
            val sId = sd.serverId ?: return Result.success(Unit)
            apiService.deleteMemorialDay("Bearer $token", sId)
            Result.success(Unit)
        } catch (e: Exception) { Result.success(Unit) }
    }

    suspend fun refreshFromServer() {
        val token = authRepository.getToken() ?: return
        try {
            val resp = apiService.getMemorialDays("Bearer $token")
            if (resp.success && resp.data != null) {
                resp.data.forEach { m ->
                    val ex = memorialDayDao.getByServerId(m.id)
                    memorialDayDao.upsert(MemorialDay(
                        id = ex?.id ?: 0, userId = m.userId, title = m.title, date = m.date,
                        type = m.type, icon = m.icon, colorHex = m.colorHex,
                        repeatYearly = m.repeatYearly, reminderDays = m.reminderDays,
                        note = m.note, serverId = m.id, syncPending = false
                    ))
                }
            }
        } catch (_: Exception) {}
    }

    private suspend fun enqueue(action: String, day: MemorialDay, localId: Int): Result<MemorialDay> {
        outboxDao.enqueue(OutboxEntry(entityType = "memorial", action = action,
            payload = gson.toJson(day), localId = localId, serverId = day.serverId))
        return Result.success(day)
    }
}
