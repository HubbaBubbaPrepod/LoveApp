package com.example.loveapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.MomentRequest
import com.example.loveapp.data.api.models.MomentResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class MomentsRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) {
    suspend fun shareMoment(request: MomentRequest): Result<MomentResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.shareMoment("Bearer $token", request)
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getMoments(page: Int = 1, limit: Int = 20): Result<List<MomentResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.getMoments("Bearer $token", page, limit)
        if (resp.success && resp.data != null) Result.success(resp.data.items)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deleteMoment(id: Int): Result<Unit> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.deleteMoment("Bearer $token", id)
        if (resp.success) Result.success(Unit)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    /** Upload image and return server URL. */
    suspend fun uploadImage(uri: Uri): Result<String> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val bytes = withContext(Dispatchers.IO) { compressImage(uri) }
        val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "moment_${System.currentTimeMillis()}.jpg", requestBody)
        val response = apiService.uploadImage("Bearer $token", part)
        if (response.success && response.data != null) Result.success(response.data.url)
        else Result.failure(Exception(response.message ?: "Upload failed"))
    } catch (e: Exception) { Result.failure(e) }

    private fun compressImage(uri: Uri, maxDim: Int = 1920, quality: Int = 85): ByteArray {
        var bitmap: Bitmap? = null
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            val scale = maxOf(1, maxOf(opts.outWidth, opts.outHeight) / maxDim)
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = scale }
            bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: throw Exception("Cannot decode image")
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.toByteArray()
            }
        } finally {
            bitmap?.recycle()
        }
    }
}
