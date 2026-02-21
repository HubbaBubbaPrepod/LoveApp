package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.AuthResponse
import com.example.loveapp.data.api.models.LoginRequest
import com.example.loveapp.data.api.models.SignupRequest
import com.example.loveapp.data.dao.UserDao
import com.example.loveapp.data.entity.User
import com.example.loveapp.utils.TokenManager
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val userDao: UserDao,
    private val tokenManager: TokenManager
) {

    suspend fun signup(
        username: String,
        email: String,
        password: String,
        displayName: String,
        gender: String
    ): Result<AuthResponse> = try {
        val request = SignupRequest(
            username = username,
            email = email,
            password = password,
            displayName = displayName,
            gender = gender
        )
        val response = apiService.signup(request)
        
        if (response.success && response.data != null) {
            response.data.token?.let { token ->
                tokenManager.saveToken(
                    token = token,
                    userId = response.data.id.toString(),
                    username = response.data.username,
                    email = response.data.email,
                    displayName = response.data.displayName
                )
            }
            
            // Save to local database
            val user = User(
                username = response.data.username,
                email = response.data.email,
                password = password,
                displayName = response.data.displayName,
                gender = response.data.gender,
                isLoggedIn = true
            )
            userDao.insertUser(user)
            
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.message ?: "Signup failed"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun login(email: String, password: String): Result<AuthResponse> = try {
        val request = LoginRequest(email = email, password = password)
        val response = apiService.login(request)
        
        if (response.success && response.data != null) {
            response.data.token?.let { token ->
                tokenManager.saveToken(
                    token = token,
                    userId = response.data.id.toString(),
                    username = response.data.username,
                    email = response.data.email,
                    displayName = response.data.displayName
                )
            }
            
            // Save to local database
            val user = User(
                username = response.data.username,
                email = response.data.email,
                password = password,
                displayName = response.data.displayName,
                gender = response.data.gender,
                isLoggedIn = true
            )
            userDao.insertUser(user)
            
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.message ?: "Login failed"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun logout(): Result<Unit> = try {
        tokenManager.clearAll()
        val currentUser = userDao.getAllUsers().firstOrNull()
        currentUser?.let {
            userDao.updateUser(it.copy(isLoggedIn = false))
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getProfile(): Result<AuthResponse> = try {
        val token = tokenManager.getToken() ?: return Result.failure(Exception("No token available"))
        val response = apiService.getProfile("Bearer $token")
        
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.message ?: "Failed to get profile"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getToken(): String? = tokenManager.getToken()
}
