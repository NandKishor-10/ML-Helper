package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

data class UserPreferences(
    val tabSize: Int = 2,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val wrapLines: Boolean = true,
    val fontSizeSp: Float = 14f
)

class PreferencesRepository(private val dataStore: DataStore<Preferences>) {

    private object PreferencesKeys {
        val TAB_SIZE = intPreferencesKey("tab_size")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val WRAP_LINES = booleanPreferencesKey("wrap_lines")
        val FONT_SIZE = floatPreferencesKey("font_size")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val tabSize = preferences[PreferencesKeys.TAB_SIZE] ?: 2
            val themeModeName = preferences[PreferencesKeys.THEME_MODE] ?: AppThemeMode.SYSTEM.name
            val themeMode = try {
                AppThemeMode.valueOf(themeModeName)
            } catch (_: Exception) {
                AppThemeMode.SYSTEM
            }
            val wrapLines = preferences[PreferencesKeys.WRAP_LINES] ?: true
            val fontSize = preferences[PreferencesKeys.FONT_SIZE] ?: 14f

            UserPreferences(
                tabSize = tabSize,
                themeMode = themeMode,
                wrapLines = wrapLines,
                fontSizeSp = fontSize
            )
        }

    suspend fun updateTabSize(tabSize: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TAB_SIZE] = tabSize
        }
    }

    suspend fun updateThemeMode(themeMode: AppThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun updateWrapLines(wrapLines: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.WRAP_LINES] = wrapLines
        }
    }

    suspend fun updateFontSize(fontSize: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_SIZE] = fontSize
        }
    }
}
