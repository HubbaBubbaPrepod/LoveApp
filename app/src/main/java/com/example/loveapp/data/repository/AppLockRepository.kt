package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.AppLockDeleteRequest
import com.example.loveapp.data.api.models.AppLockSetRequest
import com.example.loveapp.data.api.models.AppLockUpdateRequest
import com.example.loveapp.data.api.models.AppLockVerifyRequest
import com.example.loveapp.data.dao.AppLockSettingDao
import com.example.loveapp.data.entity.AppLockSetting
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import javax.inject.Inject

class AppLockRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val appLockSettingDao: AppLockSettingDao,
    private val authRepository: AuthRepository
) {
    fun observeLockSetting(userId: Int): Flow<AppLockSetting?> =
        appLockSettingDao.observeByUserId(userId)

    suspend fun isLockEnabled(userId: Int): Boolean =
        appLockSettingDao.getByUserId(userId) != null

    suspend fun setPin(userId: Int, pin: String, biometric: Boolean = false): Result<Unit> {
        val pinHash = hashPin(pin)
        appLockSettingDao.upsert(AppLockSetting(userId = userId, pinHash = pinHash, isBiometric = biometric))
        // Sync to server
        return try {
            val token = authRepository.getToken() ?: return Result.success(Unit)
            apiService.setAppLock("Bearer $token", AppLockSetRequest(pin, biometric))
            Result.success(Unit)
        } catch (_: Exception) { Result.success(Unit) }
    }

    suspend fun verifyPin(userId: Int, pin: String): Boolean {
        val setting = appLockSettingDao.getByUserId(userId) ?: return false
        return setting.pinHash == hashPin(pin)
    }

    suspend fun updatePin(userId: Int, currentPin: String, newPin: String, biometric: Boolean? = null): Result<Unit> {
        val setting = appLockSettingDao.getByUserId(userId) ?: return Result.failure(Exception("No lock set"))
        if (setting.pinHash != hashPin(currentPin)) return Result.failure(Exception("Incorrect PIN"))

        val updated = setting.copy(
            pinHash = hashPin(newPin),
            isBiometric = biometric ?: setting.isBiometric,
            updatedAt = System.currentTimeMillis()
        )
        appLockSettingDao.upsert(updated)

        return try {
            val token = authRepository.getToken() ?: return Result.success(Unit)
            apiService.updateAppLock("Bearer $token", AppLockUpdateRequest(currentPin, newPin, biometric))
            Result.success(Unit)
        } catch (_: Exception) { Result.success(Unit) }
    }

    suspend fun removePin(userId: Int, pin: String): Result<Unit> {
        val setting = appLockSettingDao.getByUserId(userId) ?: return Result.success(Unit)
        if (setting.pinHash != hashPin(pin)) return Result.failure(Exception("Incorrect PIN"))

        appLockSettingDao.deleteByUserId(userId)
        return try {
            val token = authRepository.getToken() ?: return Result.success(Unit)
            apiService.removeAppLock("Bearer $token", AppLockDeleteRequest(pin))
            Result.success(Unit)
        } catch (_: Exception) { Result.success(Unit) }
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
