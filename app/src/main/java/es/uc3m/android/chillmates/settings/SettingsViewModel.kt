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
    private var _uiState by mutableStateOf(SettingsUiState())
    
    // Temporary state to hold changes before applying
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
    }

    fun applyChanges() {
        _uiState = uiState
        
        // Apply Language
        val localeCode = when (_uiState.language) {
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
        AppCompatDelegate.setApplicationLocales(appLocale)
        
        // Note: Dark mode logic usually relies on the theme observing a state.
        // If ChillMatesTheme uses isSystemInDarkTheme(), you might need to 
        // set AppCompatDelegate.setDefaultNightMode based on _uiState.darkModeEnabled
        val nightMode = if (_uiState.darkModeEnabled) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    // TODO: Implement logout logic
    fun onLogout() {
    }
}
