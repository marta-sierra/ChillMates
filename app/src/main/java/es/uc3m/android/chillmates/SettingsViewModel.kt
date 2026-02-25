package es.uc3m.android.chillmates

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.Immutable

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val soundEnabled: Boolean = true,
    val language: String = "English"
)

class SettingsViewModel : ViewModel() {

    var uiState by mutableStateOf(SettingsUiState())
        private set

    fun onNotificationsToggle(enabled: Boolean) {
        uiState = uiState.copy(notificationsEnabled = enabled)
    }

    fun onDarkModeToggle(enabled: Boolean) {
        uiState = uiState.copy(darkModeEnabled = enabled)
    }

    fun onSoundToggle(enabled: Boolean) {
        uiState = uiState.copy(soundEnabled = enabled)
    }

    fun onLanguageChange(language: String) {
        uiState = uiState.copy(language = language)
    }

    fun onLogout() {
        // TODO: Implement logout logic
    }
}
