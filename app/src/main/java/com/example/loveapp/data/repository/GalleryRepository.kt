package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.GalleryPhotoRequest
import com.example.loveapp.data.dao.GalleryPhotoDao
import com.example.loveapp.data.dao.OutboxDao
import com.example.loveapp.data.entity.GalleryPhoto
import com.example.loveapp.data.entity.OutboxEntry
import com.example.loveapp.utils.DateUtils
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GalleryRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val galleryPhotoDao: GalleryPhotoDao,
    private val outboxDao: OutboxDao,
    private val authRepository: AuthRepository
) {
    private val gson = Gson()

    fun observePhotos(coupleKey: String): Flow<List<GalleryPhoto>> =
        galleryPhotoDao.observePhotos(coupleKey)

    fun observeCount(coupleKey: String): Flow<Int> =
        galleryPhotoDao.observeCount(coupleKey)

    suspend fun addPhoto(imageUrl: String, thumbnailUrl: String? = null, caption: String = "", width: Int = 0, height: Int = 0): Result<GalleryPhoto> {
        val local = GalleryPhoto(userId = 0, imageUrl = imageUrl, thumbnailUrl = thumbnailUrl, caption = caption, width = width, height = height, syncPending = true)
        val localId = galleryPhotoDao.upsert(local).toInt()
        val saved = local.copy(id = localId)
        return try {
            val token = authRepository.getToken() ?: return enqueue("create", saved, localId)
            val resp = apiService.addGalleryPhoto("Bearer $token", GalleryPhotoRequest(imageUrl, thumbnailUrl, caption, width, height))
            if (resp.success && resp.data != null) {
                val synced = saved.copy(
                    serverId = resp.data.id,
                    userId = resp.data.userId,
                    coupleKey = resp.data.coupleKey,
                    syncPending = false
                )
                galleryPhotoDao.upsert(synced)
                Result.success(synced)
            } else enqueue("create", saved, localId)
        } catch (e: Exception) { enqueue("create", saved, localId) }
    }

    suspend fun deletePhoto(localId: Int): Result<Unit> {
        val photo = galleryPhotoDao.getById(localId)
        galleryPhotoDao.softDelete(localId)
        return try {
            val token = authRepository.getToken() ?: return Result.success(Unit)
            val serverId = photo?.serverId ?: return Result.success(Unit)
            apiService.deleteGalleryPhoto("Bearer $token", serverId)
            Result.success(Unit)
        } catch (_: Exception) { Result.success(Unit) }
    }

    suspend fun refreshFromServer() {
        val token = authRepository.getToken() ?: return
        try {
            val resp = apiService.getGalleryPhotos("Bearer $token", 1, 200)
            if (resp.success && resp.data != null) {
                resp.data.items.forEach { p ->
                    val ex = galleryPhotoDao.getByServerId(p.id)
                    galleryPhotoDao.upsert(GalleryPhoto(
                        id = ex?.id ?: 0,
                        coupleKey = p.coupleKey,
                        userId = p.userId,
                        imageUrl = p.imageUrl,
                        thumbnailUrl = p.thumbnailUrl,
                        caption = p.caption,
                        width = p.width,
                        height = p.height,
                        timestamp = DateUtils.parseIsoTs(p.createdAt),
                        serverId = p.id,
                        syncPending = false
                    ))
                }
            }
        } catch (_: Exception) {}
    }

    private suspend fun enqueue(action: String, photo: GalleryPhoto, localId: Int): Result<GalleryPhoto> {
        outboxDao.enqueue(OutboxEntry(entityType = "gallery_photo", action = action,
            payload = gson.toJson(photo), localId = localId, serverId = photo.serverId))
        return Result.success(photo)
    }
}
