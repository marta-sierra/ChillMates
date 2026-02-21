package es.uc3m.android.chillmates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun UserPage(navController: NavController) {
    val vm: UserViewModel = viewModel()

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
        onChangePhotoClick = vm::onChangePhotoClick,
        onInviteClick = vm::onInviteClick,
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

@Composable
private fun UserHeaderSection(
    title: String,
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
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
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
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                ProfilePhotoLarge(
                    onChangePhotoClick = onProfilePhotoClick
                )
            }
        }
    }
}

@Composable
private fun ProfilePhotoLarge(
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
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile photo",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Change photo") },
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
            CreateNewFlatButton(
                onClick = onCreateNewFlatClick
            )
        }
    }
}

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
                text = "My profile",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )

            OutlinedTextField(
                value = fullNameValue,
                onValueChange = { if (isEditing) onNameChange(it) },
                label = { Text("Full name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = !isEditing
            )

            OutlinedTextField(
                value = emailValue,
                onValueChange = { if (isEditing) onEmailChange(it) },
                label = { Text("Email") },
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
                        contentDescription = "Edit or save"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEditing) "Save changes" else "Edit my info")
                }

                FilledTonalButton(
                    onClick = onChangePasswordClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Password")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Password")
                }
            }
        }
    }
}

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
                text = "My flat",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )

            OutlinedTextField(
                value = flatName,
                onValueChange = { if (isFlatNameEditing) onFlatNameChange(it) },
                label = { Text("Flat name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = !isFlatNameEditing,
                trailingIcon = {
                    IconButton(onClick = onToggleFlatNameEditing) {
                        Icon(
                            imageVector = if (isFlatNameEditing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = "Edit flat name"
                        )
                    }
                }
            )

            Text(
                text = "Roommates",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                members.forEachIndexed { index, m ->
                    ListItem(
                        headlineContent = { Text(m.name) }
                    )
                    if (index != members.lastIndex) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onInviteClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Invite roommate")
                }

                Button(
                    onClick = onLeaveFlatClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Leave flat", color = MaterialTheme.colorScheme.onError)
                }
            }
        }
    }
}

@Composable
private fun CreateNewFlatButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Create / join a new flat")
    }
}