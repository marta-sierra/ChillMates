package es.uc3m.android.chillmates.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.uc3m.android.chillmates.R

@Composable
fun SettingsPage(navController: NavController) {
    val vm: SettingsViewModel = viewModel()

    SettingsRoute(
        state = vm.uiState,
        onBackClick = { navController.popBackStack() },
        onNotificationsToggle = vm::onNotificationsToggle,
        onDarkModeToggle = vm::onDarkModeToggle,
        onSoundToggle = vm::onSoundToggle,
        onLanguageChange = vm::onLanguageChange,
        onApplyChanges = vm::applyChanges,
        onLogoutClick = vm::onLogout
    )
}

/**
 * Main layout for the Settings screen, organizing items into subsections.
 */
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
            item { SectionHeader(title = stringResource(R.string.settings_section_general)) }
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

            item { SectionHeader(title = stringResource(R.string.settings_section_appearance)) }
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
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onApplyChanges,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = stringResource(R.string.settings_btn_apply))
                }
                
                Spacer(Modifier.height(12.dp))
                
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

/**
 * Common layout for a toggle-based setting item.
 */
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
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * Setting item that triggers a dropdown menu to select the application language.
 */
@Composable
fun LanguageSettingItem(
    currentLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    // Added "System Default" and "Català" to the options list
    val languages = listOf("System Default", "English", "Español", "Català")

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.settings_label_language),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = currentLanguage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
            languages.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language) },
                    onClick = {
                        onLanguageChange(language)
                        expanded = false
                    }
                )
            }
        }
    }
}
