package es.uc3m.android.chillmates.user

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import es.uc3m.android.chillmates.R

@Composable
fun UserPage(navController: NavController) {
    val vm: UserViewModel = viewModel()
    val context = LocalContext.current

    // Launcher for selecting a profile picture from the gallery.
    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            vm.onChangePhotoClick(uri)
        }
    }

    UserRoute(
        state = vm.uiState,
        onBackClick = { navController.popBackStack() },
        onEditProfileClick = vm::onEditProfileClick,
        onSaveProfileClick = vm::onSaveProfileClick,
        onDraftNameChange = vm::onDraftNameChange,
        onDraftEmailChange = vm::onDraftEmailChange,
        onFlatNameChange = vm::onFlatNameChange,
        onToggleFlatNameEditing = vm::onToggleFlatNameEditing,
        onChangePasswordClick = vm::onChangePasswordClick,
        onChangePhotoClick = {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onInviteClick = { vm.onInviteClick(context) },
        onLeaveFlatClick = vm::onLeaveFlatClick,
        onCreateNewFlatClick = vm::onCreateNewFlatClick
    )
}

@Composable
private fun UserRoute(
    state: UserUiState,
    onBackClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onSaveProfileClick: () -> Unit,
    onDraftNameChange: (String) -> Unit,
    onDraftEmailChange: (String) -> Unit,
    onFlatNameChange: (String) -> Unit,
    onToggleFlatNameEditing: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onChangePhotoClick: () -> Unit,
    onInviteClick: () -> Unit,
    onLeaveFlatClick: () -> Unit,
    onCreateNewFlatClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        UserHeaderSection(
            title = state.fullName,
            onBackClick = onBackClick,
            photoUri = state.profilePhotoUrl,
            onProfilePhotoClick = onChangePhotoClick
        )

        UserContentSection(
            state = state,
            onEditProfileClick = onEditProfileClick,
            onSaveProfileClick = onSaveProfileClick,
            onDraftNameChange = onDraftNameChange,
            onDraftEmailChange = onDraftEmailChange,
            onChangePasswordClick = onChangePasswordClick,
            onFlatNameChange = onFlatNameChange,
            onToggleFlatNameEditing = onToggleFlatNameEditing,
            onInviteClick = onInviteClick,
            onLeaveFlatClick = onLeaveFlatClick,
            onCreateNewFlatClick = onCreateNewFlatClick
        )
    }
}

/**
 * Top app bar area showing user's name and profile picture.
 */
@Composable
private fun UserHeaderSection(
    title: String,
    photoUri: String?,
    onBackClick: () -> Unit,
    onProfilePhotoClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 14.dp)
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.user_cd_back)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                )

                ProfilePhotoLarge(
                    photoUri = photoUri,
                    onChangePhotoClick = onProfilePhotoClick
                )
            }
        }
    }
}

@Composable
private fun ProfilePhotoLarge(
    photoUri: String?,
    onChangePhotoClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier
                .size(96.dp)
                .clickable { menuExpanded = true },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            if (photoUri != null) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = stringResource(R.string.user_cd_profile_photo),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp))
                }
            }
        }

        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.user_menu_change_photo)) },
                onClick = {
                    menuExpanded = false
                    onChangePhotoClick()
                }
            )
        }
    }
}

@Composable
private fun UserContentSection(
    state: UserUiState,
    onEditProfileClick: () -> Unit,
    onSaveProfileClick: () -> Unit,
    onDraftNameChange: (String) -> Unit,
    onDraftEmailChange: (String) -> Unit,
    onChangePasswordClick: () -> Unit,
    onFlatNameChange: (String) -> Unit,
    onToggleFlatNameEditing: () -> Unit,
    onInviteClick: () -> Unit,
    onLeaveFlatClick: () -> Unit,
    onCreateNewFlatClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            PersonalSectionCard(
                isEditing = state.isProfileEditing,
                fullNameValue = if (state.isProfileEditing) state.draftFullName else state.fullName,
                emailValue = if (state.isProfileEditing) state.draftEmail else state.email,
                onNameChange = onDraftNameChange,
                onEmailChange = onDraftEmailChange,
                onEditProfileClick = onEditProfileClick,
                onSaveProfileClick = onSaveProfileClick,
                onChangePasswordClick = onChangePasswordClick
            )
        }

        item {
            FlatSectionCard(
                flatName = state.flatName,
                isFlatNameEditing = state.isFlatNameEditing,
                members = state.members,
                onFlatNameChange = onFlatNameChange,
                onToggleFlatNameEditing = onToggleFlatNameEditing,
                onInviteClick = onInviteClick,
                onLeaveFlatClick = onLeaveFlatClick
            )
        }

        item {
            Button(
                onClick = onCreateNewFlatClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.user_btn_create_join))
            }
        }
    }
}

/**
 * Card for viewing and editing personal profile information.
 */
@Composable
private fun PersonalSectionCard(
    isEditing: Boolean,
    fullNameValue: String,
    emailValue: String,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onEditProfileClick: () -> Unit,
    onSaveProfileClick: () -> Unit,
    onChangePasswordClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.user_section_profile),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )

            OutlinedTextField(
                value = fullNameValue,
                onValueChange = { if (isEditing) onNameChange(it) },
                label = { Text(stringResource(R.string.user_label_fullname)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = !isEditing
            )

            OutlinedTextField(
                value = emailValue,
                onValueChange = { if (isEditing) onEmailChange(it) },
                label = { Text(stringResource(R.string.user_label_email)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = !isEditing
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = { if (isEditing) onSaveProfileClick() else onEditProfileClick() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(if (isEditing) R.string.user_btn_save else R.string.user_btn_edit_info))
                }

                FilledTonalButton(
                    onClick = onChangePasswordClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.user_btn_password))
                }
            }
        }
    }
}

/**
 * Card displaying flat details and management options for roommates.
 */
@Composable
private fun FlatSectionCard(
    flatName: String,
    isFlatNameEditing: Boolean,
    members: List<FlatMemberUi>,
    onFlatNameChange: (String) -> Unit,
    onToggleFlatNameEditing: () -> Unit,
    onInviteClick: () -> Unit,
    onLeaveFlatClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.user_section_flat),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )

            OutlinedTextField(
                value = flatName,
                onValueChange = { if (isFlatNameEditing) onFlatNameChange(it) },
                label = { Text(stringResource(R.string.user_label_flat_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = !isFlatNameEditing,
                trailingIcon = {
                    IconButton(onClick = onToggleFlatNameEditing) {
                        Icon(
                            imageVector = if (isFlatNameEditing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = null
                        )
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = stringResource(R.string.user_label_roommates),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )

            members.forEach { member ->
                ListItem(
                    headlineContent = { Text(member.name) },
                    leadingContent = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onInviteClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.user_btn_invite))
                }
                OutlinedButton(
                    onClick = onLeaveFlatClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.user_btn_leave))
                }
            }
        }
    }
}