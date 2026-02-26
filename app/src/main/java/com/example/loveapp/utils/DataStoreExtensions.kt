package com.example.loveapp.utils

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * Singleton DataStore delegates exposed as Context extension properties.
 *
 * Declaring them here (once, non-private) ensures only ONE DataStore
 * instance exists per file name throughout the whole process — which is
 * the requirement imposed by the DataStore library.
 */

/** Stores UI / UX settings: dark mode, notification toggles, etc. */
val Context.settingsDataStore by preferencesDataStore(name = "settings")
