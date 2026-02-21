package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.NoteRequest
import com.example.loveapp.data.api.models.NoteResponse
import com.example.loveapp.data.dao.NoteDao
import com.example.loveapp.data.entity.Note
import javax.inject.Inject

class NoteRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val noteDao: NoteDao,
    private val authRepository: AuthRepository
) {
    suspend fun createNote(
        title: String,
        content: String,
        userId: Int,
        isPrivate: Boolean = false,
        tags: String = ""
    ): Result<NoteResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val request = NoteRequest(
            title = title,
            content = content,
            isPrivate = isPrivate,
            tags = tags
        )
        val response = apiService.createNote("Bearer $token", request)
        
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.message ?: "Failed to create note"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getNotes(page: Int = 1): Result<List<NoteResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getNotes("Bearer $token", page)
        
        if (response.success && response.data != null) {
            Result.success(response.data.items)
        } else {
            Result.failure(Exception(response.message ?: "Failed to get notes"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getNoteById(id: Int): Result<NoteResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getNote("Bearer $token", id)
        
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.message ?: "Failed to get note"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateNote(id: Int, note: NoteRequest): Result<NoteResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.updateNote("Bearer $token", id, note)
        
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.message ?: "Failed to update note"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteNote(id: Int): Result<Unit> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.deleteNote("Bearer $token", id)
        
        if (response.success) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(response.message ?: "Failed to delete note"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
