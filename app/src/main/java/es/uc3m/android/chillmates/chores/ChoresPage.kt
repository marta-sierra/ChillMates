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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
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
import es.uc3m.android.chillmates.model.ChoreAssignment
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Composable
fun ChoresPage(
    navController: NavController,
    viewModel: ChoresViewModel = viewModel()
) {
    val chores by viewModel.chores.collectAsState()
    val flatmates by viewModel.flatmates.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val context = LocalContext.current
    var showAddChoreDialog by remember { mutableStateOf(false) }
    var showReconfigureDialog by remember { mutableStateOf(false) }
    var choreToSwap by remember { mutableStateOf<Chore?>(null) }
    var reminderTargetChore by remember { mutableStateOf<Chore?>(null) }
    var showReminderDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showReminderDialog = reminderTargetChore != null
        } else {
            reminderTargetChore = null
        }
    }

    var weekOffset by remember { mutableIntStateOf(0) }

    val displayedChores = remember(chores, flatmates, weekOffset) {
        val currentWeekKey = weekKeyForOffset(0)
        val targetWeekKey = weekKeyForOffset(weekOffset)
        val mates = flatmates.filter { it != "Everyone" }
        chores.map { chore ->
            when {
                weekOffset == 0 -> {
                    val assignment = chore.assignments[targetWeekKey]
                        ?: ChoreAssignment(assignee = chore.assignee, isDone = chore.isDone)
                    ChoreWeekUi(
                        chore = chore,
                        assignee = assignment.assignee,
                        isDone = assignment.isDone,
                        mode = ChoreWeekMode.CURRENT,
                        hasRecord = chore.assignments.containsKey(targetWeekKey)
                    )
                }
                weekOffset < 0 -> {
                    val assignment = chore.assignments[targetWeekKey]
                    ChoreWeekUi(
                        chore = chore,
                        assignee = assignment?.assignee ?: "No record",
                        isDone = assignment?.isDone ?: false,
                        mode = ChoreWeekMode.PAST,
                        hasRecord = assignment != null
                    )
                }
                else -> {
                    val baseAssignee = chore.assignee
                    val tentativeAssignee = if (baseAssignee == "Everyone" || mates.isEmpty()) {
                        baseAssignee
                    } else {
                        val currentIndex = mates.indexOf(baseAssignee)
                        if (currentIndex == -1) {
                            baseAssignee
                        } else {
                            val size = mates.size
                            val shiftedIndex = ((currentIndex + weekOffset) % size + size) % size
                            mates[shiftedIndex]
                        }
                    }
                    ChoreWeekUi(
                        chore = chore,
                        assignee = tentativeAssignee,
                        isDone = false,
                        mode = ChoreWeekMode.FUTURE,
                        hasRecord = false
                    )
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

                    items(
                        items = displayedChores,
                        key = { choreUi -> choreUi.chore.id }
                    ) { choreUi ->
                        ChoreCard(
                            chore = choreUi.chore,
                            displayedAssignee = choreUi.assignee,
                            displayedIsDone = choreUi.isDone,
                            mode = choreUi.mode,
                            hasRecord = choreUi.hasRecord,
                            currentUserName = currentUserName,
                            onUpdateChore = { choreId, isDone ->
                                viewModel.updateChoreStatus(choreId, isDone)
                            },
                            onSwapRequest = { choreToSwap = it },
                            onSendReminder = { chore ->
                                reminderTargetChore = chore
                                if (
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    showReminderDialog = true
                                }
                            }
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
            currentWeekKey = weekKeyForOffset(0),
            onDismiss = { showReconfigureDialog = false },
            onSave = { updatedList ->
                viewModel.saveReconfiguredChores(updatedList)
                showReconfigureDialog = false
            }
        )
    }

    if (showReminderDialog) {
        ReminderTimeDialog(
            onDismiss = {
                showReminderDialog = false
                reminderTargetChore = null
            },
            onSelectDelayMinutes = { minutes ->
                reminderTargetChore?.let { chore ->
                    val title = context.getString(R.string.notif_title_reminder)
                    val message = context.getString(R.string.notif_text_format, chore.assignee, chore.title)
                    scheduleChoreReminder(
                        context = context,
                        requestCode = chore.id.hashCode(),
                        title = title,
                        message = message,
                        delayMs = TimeUnit.MINUTES.toMillis(minutes.toLong())
                    )
                }
                showReminderDialog = false
                reminderTargetChore = null
            }
        )
    }
}

@Composable
private fun ReminderTimeDialog(
    onDismiss: () -> Unit,
    onSelectDelayMinutes: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chore_reminder_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.chore_reminder_message))
                TextButton(onClick = { onSelectDelayMinutes(10) }) {
                    Text(stringResource(R.string.chore_reminder_in_10_min))
                }
                TextButton(onClick = { onSelectDelayMinutes(30) }) {
                    Text(stringResource(R.string.chore_reminder_in_30_min))
                }
                TextButton(onClick = { onSelectDelayMinutes(60) }) {
                    Text(stringResource(R.string.chore_reminder_in_1_hour))
                }
                TextButton(onClick = { onSelectDelayMinutes(180) }) {
                    Text(stringResource(R.string.chore_reminder_in_3_hours))
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) } }
    )
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
fun ReconfigureChoresDialog(
    currentChores: List<Chore>,
    flatmates: List<String>,
    currentWeekKey: String,
    onDismiss: () -> Unit,
    onSave: (List<Chore>) -> Unit
) {
    val tempChores = remember(currentChores) {
        mutableStateListOf<Chore>().apply {
            addAll(currentChores.map { chore ->
                val actualAssignee = chore.assignments[currentWeekKey]?.assignee ?: chore.assignee
                chore.copy(assignee = actualAssignee)
            })
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.btn_reconfigure_chores)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                itemsIndexed(tempChores) { index, chore ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = chore.title, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { tempChores.removeAt(index) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete chore",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
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
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Expandir")
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
private fun ChoreCard(
    chore: Chore,
    displayedAssignee: String,
    displayedIsDone: Boolean,
    mode: ChoreWeekMode,
    hasRecord: Boolean,
    currentUserName: String,
    onUpdateChore: (String, Boolean) -> Unit,
    onSwapRequest: (Chore) -> Unit,
    onSendReminder: (Chore) -> Unit
) {
    val isAssignedToMe = displayedAssignee == currentUserName || displayedAssignee == "Me"
    val isEveryone = displayedAssignee == "Everyone"
    val canComplete = mode == ChoreWeekMode.CURRENT && (isAssignedToMe || isEveryone)
    val showActions = mode == ChoreWeekMode.CURRENT

    val cardColors = when (mode) {
        ChoreWeekMode.CURRENT -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ChoreWeekMode.PAST -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ChoreWeekMode.FUTURE -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    }
    val cardBorder = if (mode == ChoreWeekMode.CURRENT) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = cardColors,
        border = cardBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = canComplete && showActions) {
                    // 🚀 La tarjeta manda la orden a Firebase, y Firebase redibuja instantáneamente.
                    onUpdateChore(chore.id, !displayedIsDone)
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chore.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (displayedIsDone && mode == ChoreWeekMode.CURRENT) TextDecoration.LineThrough else null
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.label_assigned_to, displayedAssignee),
                    style = MaterialTheme.typography.bodySmall
                )
                when (mode) {
                    ChoreWeekMode.PAST -> {
                        val statusText = if (hasRecord) {
                            if (displayedIsDone) {
                                stringResource(R.string.chore_status_completed)
                            } else {
                                stringResource(R.string.chore_status_not_completed)
                            }
                        } else {
                            stringResource(R.string.chore_status_no_record)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ChoreWeekMode.FUTURE -> Unit
                    ChoreWeekMode.CURRENT -> Unit
                }
            }

            if (showActions) {
                if (canComplete) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!displayedIsDone) {
                            IconButton(
                                onClick = { onSendReminder(chore) }
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = stringResource(R.string.cd_send_reminder),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Checkbox(
                            checked = displayedIsDone,
                            onCheckedChange = null // 🚀 100% pasivo, igual que ShoppingList
                        )
                    }
                } else {
                    if (displayedIsDone) {
                        TextButton(onClick = { onUpdateChore(chore.id, false) }) {
                            Text(stringResource(R.string.btn_redo), color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        TextButton(onClick = { onSwapRequest(chore) }) {
                            Text(stringResource(R.string.btn_swap))
                        }
                    }
                }
            }
        }
    }
}

private data class ChoreWeekUi(
    val chore: Chore,
    val assignee: String,
    val isDone: Boolean,
    val mode: ChoreWeekMode,
    val hasRecord: Boolean
)

private enum class ChoreWeekMode {
    CURRENT,
    PAST,
    FUTURE
}

private fun weekKeyForOffset(weekOffset: Int): String {
    val calendar = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        minimalDaysInFirstWeek = 4
        add(Calendar.WEEK_OF_YEAR, weekOffset)
    }
    val week = calendar.get(Calendar.WEEK_OF_YEAR)
    val year = calendar.get(Calendar.YEAR)
    return String.format("%04d-W%02d", year, week)
}

private fun scheduleChoreReminder(
    context: Context,
    requestCode: Int,
    title: String,
    message: String,
    delayMs: Long
) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val triggerAtMillis = System.currentTimeMillis() + delayMs
    val intent = Intent(context, ChoreReminderReceiver::class.java).apply {
        putExtra(ChoreReminderReceiver.EXTRA_TITLE, title)
        putExtra(ChoreReminderReceiver.EXTRA_MESSAGE, message)
        putExtra(ChoreReminderReceiver.EXTRA_NOTIFICATION_ID, requestCode)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true
    if (canExact) {
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    } else {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }
}