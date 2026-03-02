package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.CyclePatchRequest
import com.example.loveapp.data.api.models.CycleRequest
import com.example.loveapp.data.api.models.CycleResponse
import com.example.loveapp.data.dao.MenstrualCycleDao
import com.example.loveapp.data.dao.OutboxDao
import com.example.loveapp.data.entity.MenstrualCycleEntry
import com.example.loveapp.data.entity.OutboxEntry
import com.example.loveapp.utils.DateUtils
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CycleRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val cycleDao: MenstrualCycleDao,
    private val outboxDao: OutboxDao,
    private val authRepository: AuthRepository
) {
    private val gson = Gson()

    fun observeAllCycles(): Flow<List<MenstrualCycleEntry>> = cycleDao.observeAllCycleEntries()
    fun observeCyclesByUser(userId: Int): Flow<List<MenstrualCycleEntry>> = cycleDao.observeCycleEntriesByUser(userId)

    suspend fun createCycle(cycleStartDate: String, cycleDuration: Int = 28, periodDuration: Int = 5,
                            symptoms: Map<String, List<String>> = emptyMap(), mood: Map<String, String> = emptyMap(),
                            notes: String = ""): Result<MenstrualCycleEntry> {
        val startTs = DateUtils.dateStringToTimestamp(cycleStartDate)
        val local = MenstrualCycleEntry(userId = 0, cycleStartDate = startTs,
            cycleDuration = cycleDuration, periodDuration = periodDuration,
            symptoms = gson.toJson(symptoms), mood = gson.toJson(mood), notes = notes, syncPending = true)
        val localId = cycleDao.insertCycleEntry(local).toInt()
        val saved = local.copy(id = localId)
        return try {
            val token = authRepository.getToken() ?: return enqueue("create", saved, localId)
            val request = CycleRequest(cycleStartDate = cycleStartDate, cycleDuration = cycleDuration,
                periodDuration = periodDuration, symptoms = symptoms, mood = mood, notes = notes)
            val resp = apiService.createCycle("Bearer $token", request)
            if (resp.success && resp.data != null) { val s = saved.copy(serverId = resp.data.id, syncPending = false); cycleDao.upsert(s); Result.success(s) }
            else enqueue("create", saved, localId)
        } catch (e: Exception) { enqueue("create", saved, localId) }
    }

    suspend fun updateCycle(localId: Int, cycle: CycleRequest): Result<MenstrualCycleEntry> {
        val ex = cycleDao.getCycleEntryById(localId) ?: return Result.failure(Exception("Not found"))
        val updated = ex.copy(cycleDuration = cycle.cycleDuration ?: ex.cycleDuration,
            periodDuration = cycle.periodDuration ?: ex.periodDuration, notes = cycle.notes ?: ex.notes, syncPending = true)
        cycleDao.upsert(updated)
        return try {
            val token = authRepository.getToken() ?: return enqueue("update", updated, localId)
            val sId = updated.serverId ?: return enqueue("update", updated, localId)
            val resp = apiService.updateCycle("Bearer $token", sId, cycle)
            if (resp.success) { cycleDao.upsert(updated.copy(syncPending = false)); Result.success(updated.copy(syncPending = false)) }
            else enqueue("update", updated, localId)
        } catch (e: Exception) { enqueue("update", updated, localId) }
    }

    suspend fun patchCycleDay(cycleId: Int, date: String, symptomsDay: List<String>? = null, moodDay: String? = null): Result<CycleResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.patchCycle(token = "Bearer $token", id = cycleId,
            request = CyclePatchRequest(date = date, symptomsDay = symptomsDay, moodDay = moodDay))
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message ?: "Failed to patch cycle"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deleteCycle(localId: Int): Result<Unit> {
        val ex = cycleDao.getCycleEntryById(localId) ?: return Result.success(Unit)
        val sd = ex.copy(deletedAt = System.currentTimeMillis(), syncPending = true); cycleDao.upsert(sd)
        return try {
            val token = authRepository.getToken() ?: return enqueueDelete(sd, localId)
            val sId = sd.serverId ?: return enqueueDelete(sd, localId)
            apiService.deleteCycle("Bearer $token", sId)
            cycleDao.upsert(sd.copy(syncPending = false)); Result.success(Unit)
        } catch (e: Exception) { enqueueDelete(sd, localId) }
    }

    suspend fun getCycles(limit: Int = 100): Result<List<CycleResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getCycles(token = "Bearer $token", limit = limit)
        if (response.success && response.data != null) Result.success(response.data.items)
        else Result.failure(Exception(response.message ?: "Failed to get cycles"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getLatestCycle(): Result<CycleResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getLatestCycle("Bearer $token")
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message ?: "No cycle data found"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getPartnerCycles(limit: Int = 100): Result<List<CycleResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getPartnerCycles(token = "Bearer $token", limit = limit)
        if (response.success && response.data != null) Result.success(response.data.items)
        else Result.success(emptyList())
    } catch (e: Exception) { Result.success(emptyList()) }

    private suspend fun enqueue(action: String, e: MenstrualCycleEntry, localId: Int): Result<MenstrualCycleEntry> {
        outboxDao.enqueue(OutboxEntry(entityType = "cycle", action = action, payload = gson.toJson(e), localId = localId, serverId = e.serverId))
        return Result.success(e)
    }
    private suspend fun enqueueDelete(e: MenstrualCycleEntry, localId: Int): Result<Unit> {
        outboxDao.enqueue(OutboxEntry(entityType = "cycle", action = "delete", payload = gson.toJson(mapOf("id" to e.serverId)), localId = localId, serverId = e.serverId))
        return Result.success(Unit)
    }
}
