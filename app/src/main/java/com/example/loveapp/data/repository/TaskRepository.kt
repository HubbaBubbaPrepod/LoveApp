package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.CoupleTaskRequest
import com.example.loveapp.data.api.models.CoupleTaskResponse
import com.example.loveapp.data.dao.CoupleTaskDao
import com.example.loveapp.data.dao.OutboxDao
import com.example.loveapp.data.entity.CoupleTask
import com.example.loveapp.data.entity.OutboxEntry
import com.example.loveapp.utils.DateUtils
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val coupleTaskDao: CoupleTaskDao,
    private val outboxDao: OutboxDao,
    private val authRepository: AuthRepository
) {
    private val gson = Gson()

    fun observeByDate(coupleKey: String, date: String): Flow<List<CoupleTask>> =
        coupleTaskDao.observeByDate(coupleKey, date)

    fun observeTotalPoints(coupleKey: String, date: String): Flow<Int> =
        coupleTaskDao.observeTotalPoints(coupleKey, date)

    suspend fun createTask(title: String, description: String = "", category: String = "custom",
                           icon: String = "💕", points: Int = 10, dueDate: String? = null): Result<CoupleTask> {
        val local = CoupleTask(userId = 0, title = title, description = description,
            category = category, icon = icon, points = points, dueDate = dueDate, syncPending = true)
        val localId = coupleTaskDao.upsert(local).toInt()
        val saved = local.copy(id = localId)
        return try {
            val token = authRepository.getToken() ?: return enqueue("create", saved, localId)
            val resp = apiService.createTask("Bearer $token",
                CoupleTaskRequest(title, description, category, icon, points, dueDate))
            if (resp.success && resp.data != null) {
                val synced = saved.copy(serverId = resp.data.id, coupleKey = resp.data.coupleKey,
                    userId = resp.data.userId, syncPending = false)
                coupleTaskDao.upsert(synced); Result.success(synced)
            } else enqueue("create", saved, localId)
        } catch (e: Exception) { enqueue("create", saved, localId) }
    }

    suspend fun completeTask(localId: Int): Result<CoupleTask> {
        val ex = coupleTaskDao.getById(localId) ?: return Result.failure(Exception("Not found"))
        val updated = ex.copy(isCompleted = true, completedAt = System.currentTimeMillis(), syncPending = true)
        coupleTaskDao.upsert(updated)
        return try {
            val token = authRepository.getToken() ?: return enqueue("update", updated, localId)
            val sId = updated.serverId ?: return enqueue("update", updated, localId)
            val resp = apiService.completeTask("Bearer $token", sId)
            if (resp.success) { coupleTaskDao.upsert(updated.copy(syncPending = false)); Result.success(updated) }
            else enqueue("update", updated, localId)
        } catch (e: Exception) { enqueue("update", updated, localId) }
    }

    suspend fun deleteTask(localId: Int): Result<Unit> {
        val ex = coupleTaskDao.getById(localId) ?: return Result.success(Unit)
        val sd = ex.copy(deletedAt = System.currentTimeMillis(), syncPending = true)
        coupleTaskDao.upsert(sd)
        return try {
            val token = authRepository.getToken() ?: return Result.success(Unit)
            val sId = sd.serverId ?: return Result.success(Unit)
            apiService.deleteTask("Bearer $token", sId)
            Result.success(Unit)
        } catch (e: Exception) { Result.success(Unit) }
    }

    suspend fun refreshFromServer(date: String? = null) {
        val token = authRepository.getToken() ?: return
        try {
            val resp = apiService.getTasks("Bearer $token", date)
            if (resp.success && resp.data != null) {
                resp.data.items.forEach { t ->
                    val ex = coupleTaskDao.getByServerId(t.id)
                    coupleTaskDao.upsert(CoupleTask(
                        id = ex?.id ?: 0, coupleKey = t.coupleKey, userId = t.userId,
                        title = t.title, description = t.description, category = t.category,
                        icon = t.icon, points = t.points, isCompleted = t.isCompleted,
                        completedBy = t.completedBy,
                        completedAt = t.completedAt?.let { DateUtils.parseIsoTs(it) },
                        dueDate = t.dueDate, isSystem = t.isSystem,
                        serverId = t.id, syncPending = false
                    ))
                }
            }
        } catch (_: Exception) {}
    }

    private suspend fun enqueue(action: String, task: CoupleTask, localId: Int): Result<CoupleTask> {
        outboxDao.enqueue(OutboxEntry(entityType = "couple_task", action = action,
            payload = gson.toJson(task), localId = localId, serverId = task.serverId))
        return Result.success(task)
    }
}
