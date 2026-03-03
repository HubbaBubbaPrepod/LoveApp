package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.RelationshipRequest
import com.example.loveapp.data.api.models.RelationshipResponse
import com.example.loveapp.data.dao.OutboxDao
import com.example.loveapp.data.dao.RelationshipInfoDao
import com.example.loveapp.data.entity.OutboxEntry
import com.example.loveapp.data.entity.RelationshipInfo
import com.example.loveapp.utils.DateUtils
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RelationshipRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val relationshipDao: RelationshipInfoDao,
    private val outboxDao: OutboxDao,
    private val authRepository: AuthRepository
) {
    private val gson = Gson()

    fun observeRelationship(userId: Int): Flow<RelationshipInfo?> = relationshipDao.observeRelationshipByUserId(userId)

    /** Server uses PUT as upsert (no separate POST /relationship). */
    suspend fun createRelationship(relationshipStartDate: String, firstKissDate: String? = null,
                                   anniversaryDate: String? = null, myBirthday: String? = null,
                                   partnerBirthday: String? = null): Result<RelationshipResponse> =
        updateRelationship(relationshipStartDate, firstKissDate, anniversaryDate, myBirthday, partnerBirthday)

    suspend fun getRelationship(): Result<RelationshipResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getRelationship("Bearer $token")
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message ?: "No relationship data found"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun updateRelationship(relationshipStartDate: String, firstKissDate: String? = null,
                                   anniversaryDate: String? = null, myBirthday: String? = null,
                                   partnerBirthday: String? = null): Result<RelationshipResponse> {
        val request = RelationshipRequest(relationshipStartDate = relationshipStartDate,
            firstKissDate = firstKissDate, anniversaryDate = anniversaryDate,
            myBirthday = myBirthday, partnerBirthday = partnerBirthday)
        return try {
            val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
            val response = apiService.updateRelationship("Bearer $token", request)
            if (response.success && response.data != null) {
                val d = response.data
                val ex = relationshipDao.getAllRelationshipInfo().firstOrNull()
                relationshipDao.upsert(RelationshipInfo(
                    id = ex?.id ?: 0, relationshipStartDate = 0L,
                    userId1 = d.userId1 ?: 0, userId2 = d.userId2 ?: 0,
                    serverId = d.id, syncPending = false))
                Result.success(d)
            } else {
                outboxDao.enqueue(OutboxEntry(entityType = "relationship", action = "update",
                    payload = gson.toJson(request), localId = 0, serverId = null))
                Result.failure(Exception(response.message ?: "Failed to update relationship"))
            }
        } catch (e: Exception) {
            outboxDao.enqueue(OutboxEntry(entityType = "relationship", action = "update",
                payload = gson.toJson(request), localId = 0, serverId = null))
            Result.failure(e)
        }
    }

    suspend fun refreshFromServer() {
        val token = authRepository.getToken() ?: return
        try {
            val resp = apiService.getRelationship("Bearer $token")
            if (resp.success && resp.data != null) {
                val d = resp.data
                val ex = relationshipDao.getAllRelationshipInfo().firstOrNull()
                relationshipDao.upsert(RelationshipInfo(
                    id = ex?.id ?: 0,
                    relationshipStartDate = DateUtils.parseIsoTs(d.relationshipStartDate),
                    firstKissDate = d.firstKissDate?.let { DateUtils.parseIsoTs(it) },
                    anniversaryDate = d.anniversaryDate?.let { DateUtils.parseIsoTs(it) },
                    userId1 = d.userId1,
                    userId2 = d.userId2,
                    nickname1 = d.nickname1,
                    nickname2 = d.nickname2,
                    serverId = d.id,
                    syncPending = false
                ))
            }
        } catch (_: Exception) {}
    }
}
