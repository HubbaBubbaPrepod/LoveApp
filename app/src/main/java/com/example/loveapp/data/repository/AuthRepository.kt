package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.AuthResponse
import com.example.loveapp.data.api.models.LinkPartnerRequest
import com.example.loveapp.data.api.models.LinkPartnerResponse
import com.example.loveapp.data.api.models.LoginRequest
import com.example.loveapp.data.api.models.SignupRequest
import com.example.loveapp.data.dao.UserDao
import com.example.loveapp.data.entity.User
import com.example.loveapp.notifications.FcmTokenManager
import com.example.loveapp.utils.TokenManager
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val userDao: UserDao,
    private val tokenManager: TokenManager,
    private val fcmTokenManager: FcmTokenManager
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
            
            // Save to local database (cache after successful server signup)
            // NOTE: password field intentionally left blank — never store passwords locally
            val user = User(
                username = response.data.username,
                email = response.data.email,
                password = "",
                displayName = response.data.displayName,
                gender = response.data.gender ?: "",
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
            
            // Save to local database (cache after successful server login)
            // NOTE: password field intentionally left blank — never store passwords locally
            val user = User(
                username = response.data.username,
                email = response.data.email,
                password = "",
                displayName = response.data.displayName,
                gender = response.data.gender ?: "",
                isLoggedIn = true
            )
            userDao.insertUser(user)

            // Register FCM token with server (fire-and-forget — failure is non-critical)
            fcmTokenManager.refreshAndRegister()

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

    suspend fun getUserId(): Int? = tokenManager.getUserId()?.toIntOrNull()

    /** Returns the gender stored in the local user cache ("female" / "male" / "") */
    suspend fun getGender(): String = userDao.getAllUsers()
        .firstOrNull { it.isLoggedIn }?.gender
        ?: userDao.getAllUsers().firstOrNull()?.gender
        ?: ""

    /** Generates a 6-char pairing code valid for 30 minutes */
    suspend fun generatePairingCode(): Result<String> = try {
        val token = tokenManager.getToken() ?: return Result.failure(Exception("Not logged in"))
        val response = apiService.generatePairingCode("Bearer $token")
        if (response.success && response.data != null) {
            Result.success(response.data.code)
        } else {
            Result.failure(Exception(response.message ?: "Failed to generate code"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Links the current user with a partner using their pairing code */
    suspend fun linkPartner(code: String): Result<LinkPartnerResponse> = try {
        val token = tokenManager.getToken() ?: return Result.failure(Exception("Not logged in"))
        val response = apiService.linkPartner("Bearer $token", LinkPartnerRequest(code))
        if (response.success && response.data != null) {
            tokenManager.savePartnerId(response.data.partnerId.toString())
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.message ?: "Failed to link partner"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
