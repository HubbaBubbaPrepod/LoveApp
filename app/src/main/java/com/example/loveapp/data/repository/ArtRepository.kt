package com.example.loveapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.ArtCanvasRequest
import com.example.loveapp.data.api.models.ArtCanvasResponse
import com.example.loveapp.data.api.models.ArtCanvasUpdateRequest
import com.example.loveapp.data.api.models.CanvasStrokesSaveRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class ArtRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val authRepository: AuthRepository
) {
    private val _canvasesFlow = MutableStateFlow<List<ArtCanvasResponse>>(emptyList())
    val canvasesFlow: StateFlow<List<ArtCanvasResponse>> = _canvasesFlow.asStateFlow()

    private suspend fun token() = "Bearer ${authRepository.getToken() ?: error("No token")}"

    suspend fun refreshFromServer() {
        getCanvases().onSuccess { _canvasesFlow.value = it }
    }

    suspend fun getCanvases(): Result<List<ArtCanvasResponse>> = try {
        val r = apiService.getCanvases(token())
        if (r.success && r.data != null) Result.success(r.data)
        else Result.failure(Exception(r.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun createCanvas(title: String = "Без названия"): Result<ArtCanvasResponse> = try {
        val r = apiService.createCanvas(token(), ArtCanvasRequest(title))
        if (r.success && r.data != null) {
            _canvasesFlow.value = listOf(r.data) + _canvasesFlow.value
            Result.success(r.data)
        } else Result.failure(Exception(r.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun updateCanvas(id: Int, title: String): Result<ArtCanvasResponse> = try {
        val r = apiService.updateCanvas(token(), id, ArtCanvasUpdateRequest(title))
        if (r.success && r.data != null) {
            _canvasesFlow.value = _canvasesFlow.value.map { if (it.id == id) r.data else it }
            Result.success(r.data)
        } else Result.failure(Exception(r.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deleteCanvas(id: Int): Result<Unit> = try {
        apiService.deleteCanvas(token(), id)
        _canvasesFlow.value = _canvasesFlow.value.filter { it.id != id }
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun uploadThumbnail(canvasId: Int, bitmap: Bitmap): Result<String> = try {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 85, baos)
        val bytes = baos.toByteArray()
        val rb = bytes.toRequestBody("image/png".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "canvas_${canvasId}.png", rb)
        val r = apiService.uploadCanvasThumbnail(token(), canvasId, part)
        if (r.success && r.data != null) {
            _canvasesFlow.value = _canvasesFlow.value.map {
                if (it.id == canvasId) it.copy(thumbnailUrl = r.data.thumbnailUrl) else it
            }
            Result.success(r.data.thumbnailUrl)
        } else Result.failure(Exception(r.message ?: "Upload failed"))
    } catch (e: Exception) { Result.failure(e) }

    /** Fetch persisted stroke JSON for this canvas (empty array string on failure). */
    suspend fun getStrokes(canvasId: Int): Result<String> = try {
        val r = apiService.getCanvasStrokes(token(), canvasId)
        if (r.success && r.data != null) Result.success(r.data.strokes)
        else Result.success("[]")
    } catch (e: Exception) { Result.success("[]") }

    /** Persist stroke JSON for this canvas. Silent failure – thumbnail still saves. */
    suspend fun saveStrokes(canvasId: Int, strokesJson: String): Result<Unit> = try {
        apiService.saveCanvasStrokes(token(), canvasId, CanvasStrokesSaveRequest(strokesJson))
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}
