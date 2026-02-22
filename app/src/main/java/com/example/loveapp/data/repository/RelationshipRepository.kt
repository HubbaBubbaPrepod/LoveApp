package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.RelationshipRequest
import com.example.loveapp.data.api.models.RelationshipResponse
import com.example.loveapp.data.dao.RelationshipInfoDao
import javax.inject.Inject

class RelationshipRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val relationshipDao: RelationshipInfoDao,
    private val authRepository: AuthRepository
) {
    suspend fun createRelationship(
        relationshipStartDate: String,
        firstKissDate: String? = null,
        anniversaryDate: String? = null
    ): Result<RelationshipResponse> = try {
        // Server has no POST /relationship; PUT does upsert (create or update)
        updateRelationship(relationshipStartDate, firstKissDate, anniversaryDate)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getRelationship(): Result<RelationshipResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getRelationship("Bearer $token")
        
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.message ?: "No relationship data found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateRelationship(
        relationshipStartDate: String,
        firstKissDate: String? = null,
        anniversaryDate: String? = null
    ): Result<RelationshipResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val request = RelationshipRequest(
            relationshipStartDate = relationshipStartDate,
            firstKissDate = firstKissDate,
            anniversaryDate = anniversaryDate
        )
        val response = apiService.updateRelationship("Bearer $token", request)
        
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.message ?: "Failed to update relationship"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
