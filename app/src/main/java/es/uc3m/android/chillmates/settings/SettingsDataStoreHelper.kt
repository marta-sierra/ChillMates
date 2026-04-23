package es.uc3m.android.chillmates.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private object SettingsPreferencesKeys {
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
    val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
}

class SettingsDataStoreHelper(private val context: Context) {

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SettingsPreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
    }

    val darkModeEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SettingsPreferencesKeys.DARK_MODE_ENABLED] ?: false
    }

    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SettingsPreferencesKeys.SOUND_ENABLED] ?: true
    }

    suspend fun saveNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SettingsPreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun saveDarkModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SettingsPreferencesKeys.DARK_MODE_ENABLED] = enabled
        }
    }

    suspend fun saveSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SettingsPreferencesKeys.SOUND_ENABLED] = enabled
        }
    }
}