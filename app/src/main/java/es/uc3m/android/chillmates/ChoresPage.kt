package es.uc3m.android.chillmates

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ChoresPage(navController: NavController) {
    // List of flatmates to use in dropdowns
    val flatmates = listOf("Me", "Marta", "Lorena", "Everyone")

    // Mutable state list for the chores
    val chores = remember {
        mutableStateListOf(
            Chore(1, "Clean the bathroom", "Marta", false),
            Chore(2, "Take out the trash", "Me", false),
            Chore(3, "Wash the dishes", "Lorena", true),
            Chore(4, "Sweep the living room", "Everyone", false)
        )
    }

    // States to control the visibility of dialogs
    var showAddChoreDialog by remember { mutableStateOf(false) }
    var showReconfigureDialog by remember { mutableStateOf(false) }

    // State to hold the chore currently being swapped (null means dialog is closed)
    var choreToSwap by remember { mutableStateOf<Chore?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        LargeSectionHeader(title = "Chores")

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
                        text = "To make living together easier.",
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
                        onSwapRequest = { choreToSwap = it } // Open swap dialog for this chore
                    )
                }
            }

            // Bottom action buttons
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
                            contentDescription = "Reconfigure",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Reconfigure Chores")
                    }

                    Button(
                        onClick = { showAddChoreDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Chore",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Add new chore")
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. Add Chore Dialog
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

    // 2. Swap Chore Dialog
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

    // 3. Reconfigure Chores Dialog
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

// --- SUB-COMPONENTS & DIALOGS ---

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
        title = { Text("Add New Chore") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Chore Title") },
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
                onClick = {
                    if (title.isNotBlank()) onAdd(title, assignee)
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

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
        title = { Text("Swap Chore") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Who will take over '${chore.title}'?")
                FlatmateDropdown(
                    selected = selectedAssignee,
                    flatmates = flatmates,
                    onSelected = { selectedAssignee = it }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirmSwap(selectedAssignee) }) { Text("Swap") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ReconfigureChoresDialog(
    currentChores: List<Chore>,
    flatmates: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<Chore>) -> Unit
) {
    // Create a temporary working copy of the chores for the dialog
    val tempChores = remember { mutableStateListOf(*currentChores.toTypedArray()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reconfigure Chores") },
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
            Button(onClick = { onSave(tempChores.toList()) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

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


@Composable
fun ChoreCard(
    chore: Chore,
    onUpdateChore: (Chore) -> Unit,
    onSwapRequest: (Chore) -> Unit
) {
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
                    text = "Assigned to: ${chore.assignee}",
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
                        Text("Redo", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { onSwapRequest(chore) } // Triggers the Swap Dialog
                        ) {
                            Text("Swap")
                        }

                        IconButton(
                            onClick = {
                                val intent = Intent(context, ChoreReminderReceiver::class.java).apply {
                                    putExtra("CHORE_TITLE", chore.title)
                                    putExtra("ASSIGNEE", chore.assignee)
                                }
                                context.sendBroadcast(intent)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Send Reminder",
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