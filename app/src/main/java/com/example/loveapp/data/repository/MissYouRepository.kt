package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.MissYouRequest
import com.example.loveapp.data.api.models.MissYouTodayResponse
import com.example.loveapp.data.dao.MissYouEventDao
import com.example.loveapp.data.dao.OutboxDao
import com.example.loveapp.data.entity.MissYouEvent
import com.example.loveapp.data.entity.OutboxEntry
import com.example.loveapp.utils.DateUtils
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MissYouRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val missYouEventDao: MissYouEventDao,
    private val outboxDao: OutboxDao,
    private val authRepository: AuthRepository
) {
    private val gson = Gson()

    fun observeEvents(coupleKey: String): Flow<List<MissYouEvent>> =
        missYouEventDao.observeEvents(coupleKey)

    suspend fun sendMissYou(emoji: String = "❤️", message: String = ""): Result<MissYouEvent> {
        val local = MissYouEvent(senderId = 0, receiverId = 0, emoji = emoji, message = message, syncPending = true)
        val localId = missYouEventDao.upsert(local).toInt()
        val saved = local.copy(id = localId)
        return try {
            val token = authRepository.getToken() ?: return enqueue(saved, localId)
            val resp = apiService.sendMissYou("Bearer $token", MissYouRequest(emoji, message))
            if (resp.success && resp.data != null) {
                val synced = saved.copy(
                    serverId = resp.data.id,
                    senderId = resp.data.senderId,
                    receiverId = resp.data.receiverId,
                    coupleKey = resp.data.coupleKey,
                    syncPending = false
                )
                missYouEventDao.upsert(synced)
                Result.success(synced)
            } else enqueue(saved, localId)
        } catch (e: Exception) { enqueue(saved, localId) }
    }

    suspend fun getTodayStats(): Result<MissYouTodayResponse> {
        return try {
            val token = authRepository.getToken() ?: return Result.failure(Exception("Not auth"))
            val resp = apiService.getMissYouToday("Bearer $token")
            if (resp.success && resp.data != null) Result.success(resp.data)
            else Result.success(MissYouTodayResponse())
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun refreshFromServer() {
        val token = authRepository.getToken() ?: return
        try {
            val resp = apiService.getMissYouHistory("Bearer $token", 1, 100)
            if (resp.success && resp.data != null) {
                resp.data.items.forEach { m ->
                    val ex = missYouEventDao.getByServerId(m.id)
                    missYouEventDao.upsert(MissYouEvent(
                        id = ex?.id ?: 0,
                        senderId = m.senderId,
                        receiverId = m.receiverId,
                        coupleKey = m.coupleKey,
                        emoji = m.emoji,
                        message = m.message,
                        timestamp = DateUtils.parseIsoTs(m.createdAt),
                        serverId = m.id,
                        syncPending = false
                    ))
                }
            }
        } catch (_: Exception) {}
    }

    private suspend fun enqueue(event: MissYouEvent, localId: Int): Result<MissYouEvent> {
        outboxDao.enqueue(OutboxEntry(entityType = "miss_you", action = "create",
            payload = gson.toJson(event), localId = localId, serverId = event.serverId))
        return Result.success(event)
    }
}
