package es.uc3m.android.chillmates.settings

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import es.uc3m.android.chillmates.R
import es.uc3m.android.chillmates.viewmodel.AuthViewModel

@Composable
fun SettingsPage(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val vm: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as Application
                val helper = SettingsDataStoreHelper(application)
                SettingsViewModel(helper)
            }
        }
    )

    val state by vm.uiState.collectAsState()

    SettingsRoute(
        state = state,
        onBackClick = { navController.popBackStack() },
        onNotificationsToggle = vm::onNotificationsToggle,
        onDarkModeToggle = vm::onDarkModeToggle,
        onSoundToggle = vm::onSoundToggle,
        onLanguageChange = vm::onLanguageChange,
        onApplyChanges = vm::applyChanges,
        onLogoutClick = { authViewModel.logout() }
    )
}

@Composable
private fun SettingsRoute(
    state: SettingsUiState,
    onBackClick: () -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    onSoundToggle: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onApplyChanges: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.settings_cd_back),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                SectionHeader(title = stringResource(R.string.settings_section_general))
            }

            item {
                SettingsCard {
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_label_notifications),
                        checked = state.notificationsEnabled,
                        onCheckedChange = onNotificationsToggle
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_label_sound),
                        checked = state.soundEnabled,
                        onCheckedChange = onSoundToggle
                    )
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.settings_section_appearance))
            }

            item {
                SettingsCard {
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_label_dark_mode),
                        checked = state.darkModeEnabled,
                        onCheckedChange = onDarkModeToggle
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    LanguageSettingItem(
                        currentLanguage = state.language,
                        onLanguageChange = onLanguageChange
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onApplyChanges,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = stringResource(R.string.settings_btn_apply))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = stringResource(R.string.settings_btn_logout))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun LanguageSettingItem(
    currentLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    val languages = listOf("System Default", "English", "Español", "Català")
    val expanded = remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded.value = true }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.settings_label_language),
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = currentLanguage,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }

    DropdownMenu(
        expanded = expanded.value,
        onDismissRequest = { expanded.value = false }
    ) {
        languages.forEach { language ->
            DropdownMenuItem(
                text = { Text(language) },
                onClick = {
                    onLanguageChange(language)
                    expanded.value = false
                }
            )
        }
    }
}