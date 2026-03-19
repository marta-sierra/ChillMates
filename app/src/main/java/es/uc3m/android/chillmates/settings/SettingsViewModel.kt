package es.uc3m.android.chillmates.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val soundEnabled: Boolean = true,
    val language: String = "System Default"
)

class SettingsViewModel : ViewModel() {

    //UI state holding user preferences, initialized with default values.

    var uiState by mutableStateOf(SettingsUiState())
        private set

    //Updates the global notification preference.
    fun onNotificationsToggle(enabled: Boolean) {
        uiState = uiState.copy(notificationsEnabled = enabled)
    }

    // Toggles the application's appearance between Light and Dark mode.
    fun onDarkModeToggle(enabled: Boolean) {
        uiState = uiState.copy(darkModeEnabled = enabled)
    }

    // Enables or disables in-app sound effects.
    fun onSoundToggle(enabled: Boolean) {
        uiState = uiState.copy(soundEnabled = enabled)
    }

    //Updates the selected application language.
    fun onLanguageChange(language: String) {
        uiState = uiState.copy(language = language)

        // Map the UI string to Android locale codes
        val localeCode = when (language) {
            "Español" -> "es"
            "Català" -> "ca"
            "English" -> "en"
            else -> "" // Empty string resets to System Default
        }

        val appLocale: LocaleListCompat = if (localeCode.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(localeCode)
        }

        // Apply the locale change globally
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    // TODO: Implement logout logic
    fun onLogout() {
    }
}