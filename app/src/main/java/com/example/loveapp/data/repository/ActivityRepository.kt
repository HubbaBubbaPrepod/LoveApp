package com.example.loveapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.ActivityRequest
import com.example.loveapp.data.api.models.ActivityResponse
import com.example.loveapp.data.api.models.CustomActivityTypeRequest
import com.example.loveapp.data.api.models.CustomActivityTypeResponse
import com.example.loveapp.data.dao.ActivityLogDao
import com.example.loveapp.data.dao.OutboxDao
import com.example.loveapp.data.entity.ActivityLog
import com.example.loveapp.data.entity.OutboxEntry
import com.example.loveapp.utils.DateUtils
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ActivityRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val activityLogDao: ActivityLogDao,
    private val outboxDao: OutboxDao,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) {
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val gson = Gson()

    // в”Ђв”Ђв”Ђ Live Flows в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
    fun observeAllActivities(): Flow<List<ActivityLog>> = activityLogDao.observeAllActivities()
    fun observeActivitiesByUser(userId: Int): Flow<List<ActivityLog>> =
        activityLogDao.observeActivitiesByUser(userId)

    // в”Ђв”Ђв”Ђ Write: Room-first в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
    suspend fun createActivity(
        title: String,
        activityType: String,
        durationMinutes: Int,
        startTime: String,
        note: String,
        date: String = dateFmt.format(Date())
    ): Result<ActivityLog> {
        val local = ActivityLog(title = title, description = note, category = activityType,
            date = date, userId = 0, syncPending = true)
        val localId = activityLogDao.insertActivityLog(local).toInt()
        val saved = local.copy(id = localId)
        return try {
            val token = authRepository.getToken() ?: return enqueue("create", saved, localId)
            val request = ActivityRequest(title = title, description = note, date = date,
                category = activityType, activityType = activityType,
                durationMinutes = durationMinutes, startTime = startTime, note = note)
            val resp = apiService.createActivity("Bearer $token", request)
            if (resp.success && resp.data != null) {
                val synced = saved.copy(serverId = resp.data.id, syncPending = false)
                activityLogDao.upsert(synced); Result.success(synced)
            } else enqueue("create", saved, localId)
        } catch (e: Exception) { enqueue("create", saved, localId) }
    }

    suspend fun deleteActivity(localId: Int): Result<Unit> {
        val ex = activityLogDao.getActivityById(localId) ?: return Result.success(Unit)
        val sd = ex.copy(deletedAt = System.currentTimeMillis(), syncPending = true)
        activityLogDao.upsert(sd)
        return try {
            val token = authRepository.getToken() ?: return enqueueDelete(sd, localId)
            val sId = sd.serverId ?: return enqueueDelete(sd, localId)
            apiService.deleteActivity("Bearer $token", sId)
            activityLogDao.upsert(sd.copy(syncPending = false)); Result.success(Unit)
        } catch (e: Exception) { enqueueDelete(sd, localId) }
    }

    // в”Ђв”Ђв”Ђ REST reads в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
    suspend fun getActivities(date: String? = null, startDate: String? = null,
                              endDate: String? = null): Result<List<ActivityResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getActivities("Bearer $token", date = date,
            startDate = startDate, endDate = endDate, limit = 500)
        if (response.success && response.data != null) Result.success(response.data.items)
        else Result.failure(Exception(response.message ?: "Failed to get activities"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getPartnerActivities(date: String? = null, startDate: String? = null,
                                     endDate: String? = null): Result<List<ActivityResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getPartnerActivities("Bearer $token", date = date,
            startDate = startDate, endDate = endDate)
        if (response.success && response.data != null) Result.success(response.data.items)
        else Result.failure(Exception(response.message ?: "Failed to get partner activities"))
    } catch (e: Exception) { Result.failure(e) }

    // в”Ђв”Ђв”Ђ Custom activity types (server-only, no local entity) в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
    suspend fun getCustomActivityTypes(): Result<List<CustomActivityTypeResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getCustomActivityTypes("Bearer $token")
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message ?: "Failed to get custom activity types"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun createCustomActivityType(name: String, emoji: String,
                                         colorHex: String): Result<CustomActivityTypeResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.createCustomActivityType("Bearer $token",
            CustomActivityTypeRequest(name = name, emoji = emoji, colorHex = colorHex))
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message ?: "Failed to create custom activity type"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deleteCustomActivityType(id: Int): Result<Unit> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.deleteCustomActivityType("Bearer $token", id)
        if (response.success) Result.success(Unit)
        else Result.failure(Exception(response.message ?: "Failed to delete custom activity type"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun refreshFromServer() {
        val token = authRepository.getToken() ?: return
        val resp = apiService.getActivities("Bearer $token", limit = 500)
        if (resp.success && resp.data != null) {
            resp.data.items.forEach { a ->
                val ex = a.id?.let { activityLogDao.getByServerId(it) }
                activityLogDao.upsert(ActivityLog(id = ex?.id ?: 0,
                    title = a.title ?: "", description = a.description ?: a.note ?: "",
                    category = a.activityType ?: a.category ?: "",
                    date = a.date ?: "", userId = a.userId ?: 0,
                    timestamp = DateUtils.parseIsoTs(a.timestamp),
                    serverId = a.id, syncPending = false))
            }
        }
    }

    private suspend fun compressImage(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val bmp = context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) }
        ByteArrayOutputStream().also { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, out); bmp.recycle()
        }.toByteArray()
    }

    suspend fun uploadImage(uri: Uri): Result<String> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val bytes = compressImage(uri)
        val part = MultipartBody.Part.createFormData("file", "activity_icon.jpg",
            bytes.toRequestBody("image/jpeg".toMediaTypeOrNull()))
        val response = apiService.uploadImage("Bearer $token", part)
        if (response.success && response.data != null) Result.success(response.data.url)
        else Result.failure(Exception(response.message ?: "Upload failed"))
    } catch (e: Exception) { Result.failure(e) }

    private suspend fun enqueue(action: String, log: ActivityLog, localId: Int): Result<ActivityLog> {
        outboxDao.enqueue(OutboxEntry(entityType = "activity", action = action,
            payload = gson.toJson(log), localId = localId, serverId = log.serverId))
        return Result.success(log)
    }
    private suspend fun enqueueDelete(log: ActivityLog, localId: Int): Result<Unit> {
        outboxDao.enqueue(OutboxEntry(entityType = "activity", action = "delete",
            payload = gson.toJson(mapOf("id" to log.serverId)), localId = localId, serverId = log.serverId))
        return Result.success(Unit)
    }
}
