package es.uc3m.android.chillmates.chores

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.uc3m.android.chillmates.LargeSectionHeader
import es.uc3m.android.chillmates.PageContentWrapper
import es.uc3m.android.chillmates.R
import es.uc3m.android.chillmates.model.Chore

// Constants for broadcasts
private const val CHORE_REMINDER_ACTION = "es.uc3m.android.chillmates.CHORE_REMINDER"

@Composable
fun ChoresPage(
    navController: NavController,
    viewModel: ChoresViewModel = viewModel()
) {
    val chores by viewModel.chores.collectAsState()
    val flatmates by viewModel.flatmates.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()

    var showAddChoreDialog by remember { mutableStateOf(false) }
    var showReconfigureDialog by remember { mutableStateOf(false) }
    var choreToSwap by remember { mutableStateOf<Chore?>(null) }

    var weekOffset by remember { mutableIntStateOf(0) }

    val displayedChores = remember(chores, flatmates, weekOffset) {
        if (weekOffset == 0) {
            chores
        } else {
            val mates = flatmates.filter { it != "Everyone" }
            if (mates.isEmpty()) {
                chores
            } else {
                chores.map { chore ->
                    if (chore.assignee == "Everyone") {
                        chore.copy(isDone = false)
                    } else {
                        val currentIndex = mates.indexOf(chore.assignee)
                        val newAssignee = if (currentIndex != -1) {
                            val size = mates.size
                            val shiftedIndex = (((currentIndex + weekOffset) % size) + size) % size
                            mates[shiftedIndex]
                        } else {
                            chore.assignee
                        }
                        chore.copy(assignee = newAssignee, isDone = false)
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LargeSectionHeader(title = stringResource(R.string.chores_title))

        PageContentWrapper {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { weekOffset-- }) {
                                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Week")
                            }

                            Text(
                                text = when {
                                    weekOffset == 0 -> "Current Week"
                                    weekOffset == 1 -> "Next Week"
                                    weekOffset == -1 -> "Last Week"
                                    weekOffset > 1 -> "In $weekOffset weeks"
                                    else -> "${-weekOffset} weeks ago"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (weekOffset == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            IconButton(onClick = { weekOffset++ }) {
                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Week")
                            }
                        }
                    }

                    // FIX: Using key = { it.id } prevents Compose from crashing when elements are modified in large lists
                    items(
                        items = displayedChores,
                        key = { chore -> chore.id }
                    ) { chore ->
                        ChoreCard(
                            chore = chore,
                            currentUserName = currentUserName,
                            isPreview = weekOffset != 0,
                            onUpdateChore = { updatedChore ->
                                viewModel.updateChoreStatus(updatedChore.id, updatedChore.isDone)
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
    }

    if (showAddChoreDialog) {
        AddChoreDialog(
            flatmates = flatmates,
            onDismiss = { showAddChoreDialog = false },
            onAdd = { title, assignee ->
                viewModel.addChore(title, assignee)
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
                viewModel.swapChore(chore.id, newAssignee)
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
                viewModel.saveReconfiguredChores(updatedList)
                showReconfigureDialog = false
            }
        )
    }
}

@Composable
fun AddChoreDialog(flatmates: List<String>, onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var assignee by remember { mutableStateOf(flatmates.firstOrNull() ?: "Me") }
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
                FlatmateDropdown(selected = assignee, flatmates = flatmates, onSelected = { assignee = it })
            }
        },
        confirmButton = { Button(onClick = { if (title.isNotBlank()) onAdd(title, assignee) }) { Text(stringResource(R.string.btn_add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) } }
    )
}

@Composable
fun SwapChoreDialog(chore: Chore, flatmates: List<String>, onDismiss: () -> Unit, onConfirmSwap: (String) -> Unit) {
    var selectedAssignee by remember { mutableStateOf(flatmates.firstOrNull { it != chore.assignee } ?: chore.assignee) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_swap_chore_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.dialog_swap_chore_text, chore.title))
                FlatmateDropdown(selected = selectedAssignee, flatmates = flatmates, onSelected = { selectedAssignee = it })
            }
        },
        confirmButton = { Button(onClick = { onConfirmSwap(selectedAssignee) }) { Text(stringResource(R.string.btn_swap)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) } }
    )
}

@Composable
fun ReconfigureChoresDialog(currentChores: List<Chore>, flatmates: List<String>, onDismiss: () -> Unit, onSave: (List<Chore>) -> Unit) {
    // FIX: Removing 'currentChores' from the 'remember' key prevents the list from completely
    // resetting and crashing if a background update arrives from Firebase while editing.
    val tempChores = remember { mutableStateListOf<Chore>().apply { addAll(currentChores) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.btn_reconfigure_chores)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // FIX: Use key to uniquely identify dialog items during fast recomposition
                itemsIndexed(
                    items = tempChores,
                    key = { _, chore -> chore.id }
                ) { index, chore ->
                    Column {
                        Text(text = chore.title, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        FlatmateDropdown(
                            selected = chore.assignee,
                            flatmates = flatmates,
                            onSelected = { newAssignee -> tempChores[index] = chore.copy(assignee = newAssignee) }
                        )
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(tempChores.toList()) }) { Text(stringResource(R.string.btn_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) } }
    )
}

@Composable
fun FlatmateDropdown(selected: String, flatmates: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Transparent)
                .clickable { expanded = true }
        )

        DropdownMenu(
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
    currentUserName: String,
    isPreview: Boolean,
    onUpdateChore: (Chore) -> Unit,
    onSwapRequest: (Chore) -> Unit
) {
    val isMe = (chore.assignee == currentUserName || chore.assignee == "Everyone" || chore.assignee == "Me") && !isPreview
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
                    textDecoration = if (chore.isDone && !isPreview) TextDecoration.LineThrough else null
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.label_assigned_to, chore.assignee),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (!isPreview) {
                if (isMe) {
                    Checkbox(
                        checked = chore.isDone,
                        onCheckedChange = { onUpdateChore(chore.copy(isDone = it)) }
                    )
                } else {
                    if (chore.isDone) {
                        TextButton(onClick = { onUpdateChore(chore.copy(isDone = false)) }) {
                            Text(stringResource(R.string.btn_redo), color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onSwapRequest(chore) }) {
                                Text(stringResource(R.string.btn_swap))
                            }
                            IconButton(
                                onClick = {
                                    sendChoreReminderBroadcast(context, chore)
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = stringResource(R.string.cd_send_reminder), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            } else {
                // If it's a preview, show a disabled checkbox just to indicate it's a pending chore
                Checkbox(checked = false, onCheckedChange = null, enabled = false)
            }
        }
    }
}

private fun sendChoreReminderBroadcast(context: Context, chore: Chore) {
    val intent = Intent(CHORE_REMINDER_ACTION).apply {
        setPackage(context.packageName)
        putExtra("choreTitle", chore.title)
        putExtra("assignee", chore.assignee)
    }

    context.sendBroadcast(intent)
}

private fun scheduleChoreReminder(context: Context, chore: Chore, delayMs: Long) {
    val intent = Intent(CHORE_REMINDER_ACTION).apply {
        setPackage(context.packageName)
        putExtra("choreTitle", chore.title)
        putExtra("assignee", chore.assignee)
    }

    val requestCode = System.currentTimeMillis().toInt()
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val triggerAtMillis = System.currentTimeMillis() + delayMs

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    } else {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }
}