package com.amrts.regexsmali

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val advancedMode: Boolean = false,
    val includeBranches: Boolean = false,
    val excludeDebugInfo: Boolean = false,
    val isDarkTheme: Boolean? = null
)

class UserPreferencesRepository(private val context: Context) {

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            advancedMode = prefs[KEY_ADVANCED_MODE] ?: false,
            includeBranches = prefs[KEY_INCLUDE_BRANCHES] ?: false,
            excludeDebugInfo = prefs[KEY_EXCLUDE_DEBUG_INFO] ?: false,
            isDarkTheme = prefs[KEY_IS_DARK_THEME]
        )
    }

    suspend fun updateAdvancedMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ADVANCED_MODE] = enabled }
    }

    suspend fun updateIncludeBranches(enabled: Boolean) {
        context.dataStore.edit { it[KEY_INCLUDE_BRANCHES] = enabled }
    }

    suspend fun updateExcludeDebugInfo(enabled: Boolean) {
        context.dataStore.edit { it[KEY_EXCLUDE_DEBUG_INFO] = enabled }
    }

    suspend fun updateIsDarkTheme(isDark: Boolean) {
        context.dataStore.edit { it[KEY_IS_DARK_THEME] = isDark }
    }

    private companion object {
        val KEY_ADVANCED_MODE = booleanPreferencesKey("advanced_mode")
        val KEY_INCLUDE_BRANCHES = booleanPreferencesKey("include_branches")
        val KEY_EXCLUDE_DEBUG_INFO = booleanPreferencesKey("exclude_debug_info")
        val KEY_IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
    }
}
