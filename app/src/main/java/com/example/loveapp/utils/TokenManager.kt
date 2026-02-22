package com.example.loveapp.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val EMAIL_KEY = stringPreferencesKey("email")
        private val DISPLAY_NAME_KEY = stringPreferencesKey("display_name")
        private val PARTNER_ID_KEY = stringPreferencesKey("partner_id")
        
        private var instance: TokenManager? = null
        
        fun getInstance(context: Context): TokenManager {
            return instance ?: synchronized(this) {
                TokenManager(context).also { instance = it }
            }
        }
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    val userIdFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]
    }

    val userInfoFlow: Flow<UserInfo?> = context.dataStore.data.map { preferences ->
        val token = preferences[TOKEN_KEY]
        if (token != null) {
            UserInfo(
                userId = preferences[USER_ID_KEY] ?: "",
                username = preferences[USERNAME_KEY] ?: "",
                email = preferences[EMAIL_KEY] ?: "",
                displayName = preferences[DISPLAY_NAME_KEY] ?: "",
                partnerId = preferences[PARTNER_ID_KEY]
            )
        } else {
            null
        }
    }

    suspend fun saveToken(
        token: String,
        userId: String,
        username: String,
        email: String,
        displayName: String,
        partnerId: String? = null
    ) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USER_ID_KEY] = userId
            preferences[USERNAME_KEY] = username
            preferences[EMAIL_KEY] = email
            preferences[DISPLAY_NAME_KEY] = displayName
            if (partnerId != null) {
                preferences[PARTNER_ID_KEY] = partnerId
            }
        }
    }

    suspend fun getToken(): String? = tokenFlow.first()

    suspend fun getUserId(): String? = userIdFlow.first()

    suspend fun getPartnerId(): String? = context.dataStore.data.first()[PARTNER_ID_KEY]

    suspend fun savePartnerId(partnerId: String) {
        context.dataStore.edit { preferences ->
            preferences[PARTNER_ID_KEY] = partnerId
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

data class UserInfo(
    val userId: String,
    val username: String,
    val email: String,
    val displayName: String,
    val partnerId: String? = null
)
