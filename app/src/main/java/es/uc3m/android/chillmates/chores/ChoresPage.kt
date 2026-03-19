package es.uc3m.android.chillmates.chores

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import es.uc3m.android.chillmates.LargeSectionHeader
import es.uc3m.android.chillmates.R

@Composable
fun ChoresPage(navController: NavController) {
    // TODO: Read data from DB.
    val flatmates = listOf("Me", "Marta", "Lorena", "Everyone")
    val chores = remember {
        mutableStateListOf(
            Chore(1, "Clean the bathroom", "Marta", false),
            Chore(2, "Take out the trash", "Me", false),
            Chore(3, "Wash the dishes", "Lorena", true),
            Chore(4, "Sweep the living room", "Everyone", false)
        )
    }

    var showAddChoreDialog by remember { mutableStateOf(false) }
    var showReconfigureDialog by remember { mutableStateOf(false) }
    var choreToSwap by remember { mutableStateOf<Chore?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        LargeSectionHeader(title = stringResource(R.string.chores_title))

        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = stringResource(R.string.chores_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(chores) { chore ->
                    ChoreCard(
                        chore = chore,
                        onUpdateChore = { updatedChore ->
                            val index = chores.indexOfFirst { it.id == updatedChore.id }
                            if (index != -1) {
                                chores[index] = updatedChore
                            }
                        },
                        onSwapRequest = { choreToSwap = it }
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showReconfigureDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.cd_reconfigure),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_reconfigure_chores))
                    }

                    Button(
                        onClick = { showAddChoreDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.cd_add_chore),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_add_new_chore))
                    }
                }
            }
        }
    }

    if (showAddChoreDialog) {
        AddChoreDialog(
            flatmates = flatmates,
            onDismiss = { showAddChoreDialog = false },
            onAdd = { title, assignee ->
                val newId = (chores.maxOfOrNull { it.id } ?: 0) + 1
                chores.add(Chore(newId, title, assignee, false))
                showAddChoreDialog = false
            }
        )
    }

    choreToSwap?.let { chore ->
        SwapChoreDialog(
            chore = chore,
            flatmates = flatmates,
            onDismiss = { choreToSwap = null },
            onConfirmSwap = { newAssignee ->
                val index = chores.indexOfFirst { it.id == chore.id }
                if (index != -1) {
                    chores[index] = chore.copy(assignee = newAssignee)
                }
                choreToSwap = null
            }
        )
    }

    if (showReconfigureDialog) {
        ReconfigureChoresDialog(
            currentChores = chores,
            flatmates = flatmates,
            onDismiss = { showReconfigureDialog = false },
            onSave = { updatedList ->
                chores.clear()
                chores.addAll(updatedList)
                showReconfigureDialog = false
            }
        )
    }
}

/**
 * Dialog to input details for a new chore assignment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChoreDialog(
    flatmates: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var assignee by remember { mutableStateOf(flatmates.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_add_chore_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.label_chore_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlatmateDropdown(
                    selected = assignee,
                    flatmates = flatmates,
                    onSelected = { assignee = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onAdd(title, assignee) }
            ) { Text(stringResource(R.string.btn_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}

/**
 * Dialog to reassign a specific chore to a different flatmate.
 */
@Composable
fun SwapChoreDialog(
    chore: Chore,
    flatmates: List<String>,
    onDismiss: () -> Unit,
    onConfirmSwap: (String) -> Unit
) {
    var selectedAssignee by remember { mutableStateOf(flatmates.first { it != chore.assignee }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_swap_chore_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.dialog_swap_chore_text, chore.title))
                FlatmateDropdown(
                    selected = selectedAssignee,
                    flatmates = flatmates,
                    onSelected = { selectedAssignee = it }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirmSwap(selectedAssignee) }) {
                Text(stringResource(R.string.btn_swap))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}

/**
 * Screen-overlay to adjust all current chore assignments at once.
 */
@Composable
fun ReconfigureChoresDialog(
    currentChores: List<Chore>,
    flatmates: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<Chore>) -> Unit
) {
    val tempChores = remember { mutableStateListOf(*currentChores.toTypedArray()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.btn_reconfigure_chores)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(tempChores.size) { index ->
                    val chore = tempChores[index]
                    Column {
                        Text(text = chore.title, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        FlatmateDropdown(
                            selected = chore.assignee,
                            flatmates = flatmates,
                            onSelected = { newAssignee ->
                                tempChores[index] = chore.copy(assignee = newAssignee)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(tempChores.toList()) }) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}

/**
 * Custom dropdown menu for selecting a flatmate from a list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlatmateDropdown(
    selected: String,
    flatmates: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            flatmates.forEach { mate ->
                DropdownMenuItem(
                    text = { Text(mate) },
                    onClick = {
                        onSelected(mate)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Card representing an individual chore with status toggles and reminder actions.
 */
@Composable
fun ChoreCard(
    chore: Chore,
    onUpdateChore: (Chore) -> Unit,
    onSwapRequest: (Chore) -> Unit
) {
    // TODO: Read data from DB.
    val isMe = chore.assignee == "Me" || chore.assignee == "Everyone"
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chore.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (chore.isDone) TextDecoration.LineThrough else null
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.label_assigned_to, chore.assignee),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (isMe) {
                Checkbox(
                    checked = chore.isDone,
                    onCheckedChange = { onUpdateChore(chore.copy(isDone = it)) }
                )
            } else {
                if (chore.isDone) {
                    TextButton(
                        onClick = { onUpdateChore(chore.copy(isDone = false)) }
                    ) {
                        Text(
                            text = stringResource(R.string.btn_redo),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { onSwapRequest(chore) }
                        ) {
                            Text(stringResource(R.string.btn_swap))
                        }

                        IconButton(
                            onClick = {
                                val intent = Intent(context, ChoreReminderReceiver::class.java).apply {
                                    // FLAG: Key naming fixed to camelCase
                                    putExtra("choreTitle", chore.title)
                                    putExtra("assignee", chore.assignee)
                                }
                                context.sendBroadcast(intent)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = stringResource(R.string.cd_send_reminder),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

data class Chore(val id: Int, val title: String, val assignee: String, val isDone: Boolean)