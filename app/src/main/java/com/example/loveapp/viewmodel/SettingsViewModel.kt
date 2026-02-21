package com.example.loveapp.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.settingsDataStore by preferencesDataStore("settings")

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        private val REMINDERS_ENABLED_KEY = booleanPreferencesKey("reminders_enabled")
    }

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _remindersEnabled = MutableStateFlow(true)
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            context.settingsDataStore.data.map { preferences ->
                preferences[DARK_MODE_KEY] ?: false
            }.collect { _isDarkMode.value = it }

            context.settingsDataStore.data.map { preferences ->
                preferences[NOTIFICATIONS_ENABLED_KEY] ?: true
            }.collect { _notificationsEnabled.value = it }

            context.settingsDataStore.data.map { preferences ->
                preferences[REMINDERS_ENABLED_KEY] ?: true
            }.collect { _remindersEnabled.value = it }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            context.settingsDataStore.edit { preferences ->
                preferences[DARK_MODE_KEY] = enabled
            }
            _isDarkMode.value = enabled
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.settingsDataStore.edit { preferences ->
                preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
            }
            _notificationsEnabled.value = enabled
        }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.settingsDataStore.edit { preferences ->
                preferences[REMINDERS_ENABLED_KEY] = enabled
            }
            _remindersEnabled.value = enabled
        }
    }
}
