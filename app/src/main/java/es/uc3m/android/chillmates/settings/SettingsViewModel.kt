package es.uc3m.android.chillmates.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val soundEnabled: Boolean = true,
    val language: String = "System Default"
)

class SettingsViewModel(
    private val dataStoreHelper: SettingsDataStoreHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataStoreHelper.notificationsEnabled.collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
            }
        }

        viewModelScope.launch {
            dataStoreHelper.darkModeEnabled.collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(darkModeEnabled = enabled)
            }
        }

        viewModelScope.launch {
            dataStoreHelper.soundEnabled.collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(soundEnabled = enabled)
            }
        }

        viewModelScope.launch {
            dataStoreHelper.language.collectLatest { language ->
                _uiState.value = _uiState.value.copy(language = language)
            }
        }
    }

    fun onNotificationsToggle(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
    }

    fun onDarkModeToggle(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(darkModeEnabled = enabled)
    }

    fun onSoundToggle(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(soundEnabled = enabled)
    }

    fun onLanguageChange(language: String) {
        _uiState.value = _uiState.value.copy(language = language)
    }

    fun applyChanges() {
        val currentState = _uiState.value

        viewModelScope.launch {
            dataStoreHelper.saveNotificationsEnabled(currentState.notificationsEnabled)
            dataStoreHelper.saveDarkModeEnabled(currentState.darkModeEnabled)
            dataStoreHelper.saveSoundEnabled(currentState.soundEnabled)
            dataStoreHelper.saveLanguage(currentState.language)
        }

        val localeCode = when (currentState.language) {
            "Español" -> "es"
            "Català" -> "ca"
            "English" -> "en"
            else -> ""
        }

        val appLocale = if (localeCode.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(localeCode)
        }
        AppCompatDelegate.setApplicationLocales(appLocale)

        val nightMode = if (currentState.darkModeEnabled) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}