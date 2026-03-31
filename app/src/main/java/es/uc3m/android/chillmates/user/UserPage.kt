package es.uc3m.android.chillmates.user

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import es.uc3m.android.chillmates.R

@Composable
fun UserPage(navController: NavController) {
    val vm: UserViewModel = viewModel()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

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

    LaunchedEffect(vm.uiState.successMessage) {
        vm.uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            vm.clearSuccessMessage()
        }
    }

    LaunchedEffect(vm.uiState.errorMessage) {
        vm.uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            vm.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            UserRoute(
                state = vm.uiState,
                onBackClick = { navController.popBackStack() },
                onChangePhotoClick = {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onInviteClick = { vm.onInviteClick(context) },
                onLeaveFlatClick = { vm.onLeaveFlat() },
                onCreateNewFlatClick = { vm.onShowCreateJoinDialog() },
                onShowCreateJoinDialog = { vm.onShowCreateJoinDialog() },
                onDismissCreateJoinDialog = { vm.onDismissCreateJoinDialog() },
                onCreateJoinModeChange = { vm.onCreateJoinModeChange(it) },
                onDialogFlatNameChange = { vm.onDialogFlatNameChange(it) },
                onDialogInviteCodeChange = { vm.onDialogInviteCodeChange(it) },
                onConfirmCreateJoin = { vm.onConfirmCreateJoin() },
                onUpdateProfile = { vm.onUpdateProfile(it) }
            )
        }
    }

    if (vm.uiState.showCreateJoinDialog) {
        CreateJoinFlatDialog(
            mode = vm.uiState.createJoinMode,
            flatName = vm.uiState.dialogFlatName,
            inviteCode = vm.uiState.dialogInviteCode,
            isLoading = vm.uiState.isLoading,
            errorMessage = vm.uiState.errorMessage,
            onModeChange = { vm.onCreateJoinModeChange(it) },
            onFlatNameChange = { vm.onDialogFlatNameChange(it) },
            onInviteCodeChange = { vm.onDialogInviteCodeChange(it) },
            onConfirm = { vm.onConfirmCreateJoin() },
            onDismiss = { vm.onDismissCreateJoinDialog() }
        )
    }
}

@Composable
private fun UserRoute(
    state: UserUiState,
    onBackClick: () -> Unit,
    onChangePhotoClick: () -> Unit,
    onInviteClick: () -> Unit,
    onLeaveFlatClick: () -> Unit,
    onCreateNewFlatClick: () -> Unit,
    onShowCreateJoinDialog: () -> Unit,
    onDismissCreateJoinDialog: () -> Unit,
    onCreateJoinModeChange: (CreateJoinMode) -> Unit,
    onDialogFlatNameChange: (String) -> Unit,
    onDialogInviteCodeChange: (String) -> Unit,
    onConfirmCreateJoin: () -> Unit,
    onUpdateProfile: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        UserHeaderSection(
            title = state.user?.displayName ?: "",
            photoUri = state.user?.avatarUrl,
            onBackClick = onBackClick,
            onProfilePhotoClick = onChangePhotoClick
        )

        UserContentSection(
            state = state,
            onInviteClick = onInviteClick,
            onLeaveFlatClick = onLeaveFlatClick,
            onCreateNewFlatClick = onCreateNewFlatClick,
            onUpdateProfile = onUpdateProfile
        )
    }
}

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
                    text = title.ifEmpty { "User" },
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
    onInviteClick: () -> Unit,
    onLeaveFlatClick: () -> Unit,
    onCreateNewFlatClick: () -> Unit,
    onUpdateProfile: (String) -> Unit
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
            ProfileSectionCard(
                displayName = state.user?.displayName ?: "",
                email = state.user?.email ?: "",
                isLoading = state.isLoading,
                onUpdateProfile = onUpdateProfile
            )
        }

        if (state.flat != null) {
            item {
                FlatSectionCard(
                    flat = state.flat,
                    members = state.members,
                    onInviteClick = onInviteClick,
                    onLeaveFlatClick = onLeaveFlatClick
                )
            }
        }

        item {
            Button(
                onClick = onCreateNewFlatClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (state.flat != null) {
                        stringResource(R.string.user_btn_create_join)
                    } else {
                        stringResource(R.string.flat_setup_title)
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileSectionCard(
    displayName: String,
    email: String,
    isLoading: Boolean,
    onUpdateProfile: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var draftName by remember(displayName) { mutableStateOf(displayName) }

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
                value = if (isEditing) draftName else displayName,
                onValueChange = { if (isEditing) draftName = it },
                label = { Text(stringResource(R.string.user_label_fullname)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = !isEditing,
                enabled = !isLoading
            )

            OutlinedTextField(
                value = email,
                onValueChange = { },
                label = { Text(stringResource(R.string.user_label_email)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = true,
                enabled = false
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        if (isEditing) {
                            onUpdateProfile(draftName)
                        }
                        isEditing = !isEditing
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(if (isEditing) R.string.user_btn_save else R.string.user_btn_edit_info))
                }

                FilledTonalButton(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = false
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.user_btn_password))
                }
            }
        }
    }
}

@Composable
private fun FlatSectionCard(
    flat: es.uc3m.android.chillmates.model.Flat,
    members: List<FlatMemberUi>,
    onInviteClick: () -> Unit,
    onLeaveFlatClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var showCopiedSnackbar by remember { mutableStateOf(false) }

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

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = flat.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.flat_setup_invite_code),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = flat.inviteCode,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(flat.inviteCode))
                        showCopiedSnackbar = true
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy code")
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = stringResource(R.string.user_label_roommates),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )

            if (members.isEmpty()) {
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
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

@Composable
private fun CreateJoinFlatDialog(
    mode: CreateJoinMode,
    flatName: String,
    inviteCode: String,
    isLoading: Boolean,
    errorMessage: String?,
    onModeChange: (CreateJoinMode) -> Unit,
    onFlatNameChange: (String) -> Unit,
    onInviteCodeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (mode == CreateJoinMode.CREATE) {
                    stringResource(R.string.flat_setup_create)
                } else {
                    stringResource(R.string.flat_setup_join)
                }
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = mode == CreateJoinMode.CREATE,
                        onClick = { onModeChange(CreateJoinMode.CREATE) },
                        label = { Text(stringResource(R.string.flat_setup_create)) },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = mode == CreateJoinMode.JOIN,
                        onClick = { onModeChange(CreateJoinMode.JOIN) },
                        label = { Text(stringResource(R.string.flat_setup_join)) },
                        leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                when (mode) {
                    CreateJoinMode.CREATE -> {
                        OutlinedTextField(
                            value = flatName,
                            onValueChange = onFlatNameChange,
                            label = { Text(stringResource(R.string.flat_setup_flat_name_label)) },
                            placeholder = { Text(stringResource(R.string.flat_setup_flat_name_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isLoading
                        )
                    }
                    CreateJoinMode.JOIN -> {
                        OutlinedTextField(
                            value = inviteCode,
                            onValueChange = onInviteCodeChange,
                            label = { Text(stringResource(R.string.flat_setup_invite_code_label)) },
                            placeholder = { Text(stringResource(R.string.flat_setup_invite_code_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isLoading
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.flat_setup_continue))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
private fun Box(modifier: Modifier, contentAlignment: Alignment, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier,
        contentAlignment = contentAlignment
    ) {
        content()
    }
}
