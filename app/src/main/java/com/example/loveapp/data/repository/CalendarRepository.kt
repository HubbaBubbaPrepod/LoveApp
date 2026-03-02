package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.CalendarEventRequest
import com.example.loveapp.data.api.models.CalendarEventResponse
import com.example.loveapp.data.api.models.CustomCalendarRequest
import com.example.loveapp.data.api.models.CustomCalendarResponse
import com.example.loveapp.data.dao.CustomCalendarDao
import com.example.loveapp.data.dao.OutboxDao
import com.example.loveapp.data.entity.CustomCalendar
import com.example.loveapp.data.entity.OutboxEntry
import com.example.loveapp.utils.DateUtils
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CalendarRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val calendarDao: CustomCalendarDao,
    private val outboxDao: OutboxDao,
    private val authRepository: AuthRepository
) {
    private val gson = Gson()

    fun observeAllCalendars(): Flow<List<CustomCalendar>> = calendarDao.observeAllCalendars()
    fun observeCalendarsByUser(userId: Int): Flow<List<CustomCalendar>> = calendarDao.observeCalendarsByUser(userId)

    suspend fun createCalendar(name: String, type: String, color: String, description: String = ""): Result<CustomCalendar> {
        val local = CustomCalendar(name = name, description = description, type = type,
            colorHex = color, userId = 0, syncPending = true)
        val localId = calendarDao.insertCalendar(local).toInt()
        val saved = local.copy(id = localId)
        return try {
            val token = authRepository.getToken() ?: return enqueueCalendar("create", saved, localId)
            val resp = apiService.createCalendar("Bearer $token", CustomCalendarRequest(name = name, description = description, type = type, colorHex = color))
            if (resp.success && resp.data != null) {
                val s = saved.copy(serverId = resp.data.id, syncPending = false); calendarDao.upsert(s); Result.success(s)
            } else enqueueCalendar("create", saved, localId)
        } catch (e: Exception) { enqueueCalendar("create", saved, localId) }
    }

    suspend fun getCalendars(type: String? = null): Result<List<CustomCalendarResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getCalendars("Bearer $token", type, 1, 100)
        if (response.success && response.data != null) Result.success(response.data.items)
        else Result.failure(Exception(response.message ?: "Failed to get calendars"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getPartnerCalendars(): Result<List<CustomCalendarResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getPartnerCalendars("Bearer $token")
        if (response.success && response.data != null) Result.success(response.data.items)
        else Result.success(emptyList())
    } catch (e: Exception) { Result.success(emptyList()) }

    suspend fun getCalendar(id: Int): Result<CustomCalendarResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getCalendar("Bearer $token", id)
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message ?: "Failed to get calendar"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun updateCalendar(localId: Int, calendar: CustomCalendarRequest): Result<CustomCalendar> {
        val ex = calendarDao.getCalendarById(localId) ?: return Result.failure(Exception("Not found"))
        val updated = ex.copy(name = calendar.name ?: ex.name, description = calendar.description ?: ex.description,
            colorHex = calendar.colorHex ?: ex.colorHex, syncPending = true)
        calendarDao.upsert(updated)
        return try {
            val token = authRepository.getToken() ?: return enqueueCalendar("update", updated, localId)
            val sId = updated.serverId ?: return enqueueCalendar("update", updated, localId)
            val resp = apiService.updateCalendar("Bearer $token", sId, calendar)
            if (resp.success) { calendarDao.upsert(updated.copy(syncPending = false)); Result.success(updated.copy(syncPending = false)) }
            else enqueueCalendar("update", updated, localId)
        } catch (e: Exception) { enqueueCalendar("update", updated, localId) }
    }

    suspend fun deleteCalendar(localId: Int): Result<Unit> {
        val ex = calendarDao.getCalendarById(localId) ?: return Result.success(Unit)
        val sd = ex.copy(deletedAt = System.currentTimeMillis(), syncPending = true); calendarDao.upsert(sd)
        return try {
            val token = authRepository.getToken() ?: return enqueueCalendarDelete(sd, localId)
            val sId = sd.serverId ?: return enqueueCalendarDelete(sd, localId)
            apiService.deleteCalendar("Bearer $token", sId)
            calendarDao.upsert(sd.copy(syncPending = false)); Result.success(Unit)
        } catch (e: Exception) { enqueueCalendarDelete(sd, localId) }
    }

    suspend fun getCalendarEvents(calendarId: Int): Result<List<CalendarEventResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getCalendarEvents("Bearer $token", calendarId)
        if (response.success && response.data != null) Result.success(response.data.items)
        else Result.failure(Exception(response.message ?: "Failed to get events"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun markDay(calendarId: Int, date: String, title: String = ""): Result<CalendarEventResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.createCalendarEvent("Bearer $token", calendarId,
            CalendarEventRequest(eventDate = date, title = title))
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message ?: "Failed to mark day"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun unmarkDay(eventId: Int): Result<Unit> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.deleteCalendarEvent("Bearer $token", eventId)
        if (response.success) Result.success(Unit)
        else Result.failure(Exception(response.message ?: "Failed to unmark day"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun refreshFromServer() {
        val token = authRepository.getToken() ?: return
        val resp = apiService.getCalendars("Bearer $token", null, 1, 100)
        if (resp.success && resp.data != null) {
            resp.data.items.forEach { c ->
                val ex = c.id?.let { calendarDao.getByServerId(it) }
                calendarDao.upsert(CustomCalendar(id = ex?.id ?: 0, name = c.name ?: "",
                    description = c.description ?: "", type = c.type ?: "custom",
                    colorHex = c.colorHex ?: "#FF6B9D", userId = c.userId ?: 0,
                    createdAt = DateUtils.parseIsoTs(c.createdAt),
                    serverId = c.id, syncPending = false))
            }
        }
    }

    private suspend fun enqueueCalendar(action: String, c: CustomCalendar, localId: Int): Result<CustomCalendar> {
        outboxDao.enqueue(OutboxEntry(entityType = "calendar", action = action, payload = gson.toJson(c), localId = localId, serverId = c.serverId))
        return Result.success(c)
    }
    private suspend fun enqueueCalendarDelete(c: CustomCalendar, localId: Int): Result<Unit> {
        outboxDao.enqueue(OutboxEntry(entityType = "calendar", action = "delete", payload = gson.toJson(mapOf("id" to c.serverId)), localId = localId, serverId = c.serverId))
        return Result.success(Unit)
    }
}
