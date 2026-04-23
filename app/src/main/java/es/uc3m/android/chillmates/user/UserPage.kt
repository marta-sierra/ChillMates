package es.uc3m.android.chillmates.user

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
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
    val snackbarHostState = remember { SnackbarHostState() }
    var showPasswordDialog by remember { mutableStateOf(false) }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        // Al anular los insets por defecto evitamos el hueco superior de la barra de estado
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        UserRoute(
            modifier = Modifier.padding(paddingValues),
            state = vm.uiState,
            onBackClick = { navController.popBackStack() },
            onChangePhotoClick = {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onLeaveFlatClick = { vm.onLeaveFlat() },
            onShowCreateJoinDialog = { vm.onShowCreateJoinDialog() },
            onUpdateProfile = { vm.onUpdateProfile(it) },
            onPasswordClick = { showPasswordDialog = true },
            onLogoutClick = {
                vm.onLogout()
                navController.navigate("login") { popUpTo(0) }
            }
        )
    }

    // Dialogs
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

    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { newPass ->
                vm.onUpdatePassword(newPass)
                showPasswordDialog = false
            }
        )
    }
}

@Composable
private fun UserRoute(
    modifier: Modifier = Modifier,
    state: UserUiState,
    onBackClick: () -> Unit,
    onChangePhotoClick: () -> Unit,
    onLeaveFlatClick: () -> Unit,
    onShowCreateJoinDialog: () -> Unit,
    onUpdateProfile: (String) -> Unit,
    onPasswordClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Surface(color = MaterialTheme.colorScheme.primaryContainer) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.user_cd_back),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Text(
                    text = state.user?.displayName?.ifEmpty { "User" } ?: "User",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )

                ProfilePhotoSmall(
                    photoUri = state.user?.avatarUrl,
                    onChangePhotoClick = onChangePhotoClick
                )
            }
        }

        // LazyColumn de contenido
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                top = 16.dp,     // <-- AQUÍ SEPARADO DEL HEADER (antes estaba a 0.dp)
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            )
        ) {
            item {
                ProfileSectionCard(
                    displayName = state.user?.displayName ?: "",
                    email = state.user?.email ?: "",
                    isLoading = state.isLoading,
                    onUpdateProfile = onUpdateProfile,
                    onPasswordClick = onPasswordClick
                )
            }

            if (state.flat != null) {
                item {
                    FlatSectionCardClean(
                        flat = state.flat,
                        onLeaveFlatClick = onLeaveFlatClick
                    )
                }
            } else {
                item {
                    Button(
                        onClick = onShowCreateJoinDialog,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AddHome, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create or Join a Flat")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onLogoutClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout Session")
                }
            }
        }
    }
}

@Composable
private fun ProfilePhotoSmall(photoUri: String?, onChangePhotoClick: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            modifier = Modifier.size(48.dp).clickable { menuExpanded = true },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            if (!photoUri.isNullOrEmpty()) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(24.dp))
                }
            }
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Change Photo") },
                onClick = { menuExpanded = false; onChangePhotoClick() }
            )
        }
    }
}

@Composable
private fun ProfileSectionCard(
    displayName: String,
    email: String,
    isLoading: Boolean,
    onUpdateProfile: (String) -> Unit,
    onPasswordClick: () -> Unit
) {
    var nameDraft by remember(displayName) { mutableStateOf(displayName) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "My Profile", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !isLoading
                )
                IconButton(
                    onClick = { onUpdateProfile(nameDraft) },
                    enabled = nameDraft != displayName && !isLoading && nameDraft.isNotBlank()
                ) {
                    Icon(Icons.Default.CheckCircle, "Update", tint = MaterialTheme.colorScheme.primary)
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                enabled = true,
                trailingIcon = { Icon(Icons.Default.Lock, null, Modifier.size(18.dp)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Button(
                onClick = onPasswordClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                Icon(Icons.Default.VpnKey, null)
                Spacer(Modifier.width(8.dp))
                Text("Change Password")
            }
        }
    }
}

@Composable
private fun FlatSectionCardClean(
    flat: es.uc3m.android.chillmates.model.Flat,
    onLeaveFlatClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Current Flat", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = flat.name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = flat.inviteCode, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(flat.inviteCode)) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Button(
                onClick = onLeaveFlatClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ExitToApp, null)
                Spacer(Modifier.width(8.dp))
                Text("Leave Flat")
            }
        }
    }
}

@Composable
fun ChangePasswordDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pass1 by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = pass1, onValueChange = { pass1 = it }, label = { Text("New Password") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pass2, onValueChange = { pass2 = it }, label = { Text("Repeat Password") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(enabled = pass1.isNotEmpty() && pass1 == pass2 && pass1.length >= 6, onClick = { onConfirm(pass1) }) { Text("Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
        title = { Text(if (mode == CreateJoinMode.CREATE) "Create Flat" else "Join Flat") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == CreateJoinMode.CREATE, onClick = { onModeChange(CreateJoinMode.CREATE) }, label = { Text("Create") }, modifier = Modifier.weight(1f))
                    FilterChip(selected = mode == CreateJoinMode.JOIN, onClick = { onModeChange(CreateJoinMode.JOIN) }, label = { Text("Join") }, modifier = Modifier.weight(1f))
                }
                if (mode == CreateJoinMode.CREATE) {
                    OutlinedTextField(value = flatName, onValueChange = onFlatNameChange, label = { Text("Flat Name") }, modifier = Modifier.fillMaxWidth())
                } else {
                    OutlinedTextField(value = inviteCode, onValueChange = onInviteCodeChange, label = { Text("Invite Code") }, modifier = Modifier.fillMaxWidth())
                }
                if (errorMessage != null) Text(text = errorMessage, color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isLoading) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp)) else Text("Confirm")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}