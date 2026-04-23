package es.uc3m.android.chillmates.home

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CalendarContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.uc3m.android.chillmates.NavGraph
import es.uc3m.android.chillmates.R

@Composable
fun HomePage(navController: NavController, viewModel: HomeViewModel = viewModel()) {
    val uiState = viewModel.uiState
    val context = LocalContext.current
    var showAddNoticeDialog by remember { mutableStateOf(false) }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    if (uiState.errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Error: ${uiState.errorMessage}", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            NoticeBoardSection(
                notices = uiState.notices,
                onAcceptClick = { noticeId -> viewModel.acceptPoll(noticeId) },
                onDeclineClick = { noticeId -> viewModel.declinePoll(noticeId) },
                onAddNoticeClick = { showAddNoticeDialog = true }
            )
        }
        item {
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        item {
            UpcomingEventsSection(events = uiState.upcomingEvents, context = context, viewModel = viewModel)
        }
    }

    // Dialogo para añadir nuevo Anuncio
    if (showAddNoticeDialog) {
        AddNoticeDialog(
            onDismiss = { showAddNoticeDialog = false },
            onConfirm = { title, details, isPoll ->
                viewModel.addNotice(title, details, isPoll)
                showAddNoticeDialog = false
            }
        )
    }
}

@Composable
fun NoticeBoardSection(
    notices: List<NoticeUi>,
    onAcceptClick: (String) -> Unit,
    onDeclineClick: (String) -> Unit,
    onAddNoticeClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.home_notice_board),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (notices.isEmpty()) {
            Text(
                text = "No notices yet.",
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(notices) { notice ->
                    NoticeCard(
                        notice = notice,
                        onAcceptClick = onAcceptClick,
                        onDeclineClick = onDeclineClick
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextButton(
                onClick = onAddNoticeClick,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Add Notice",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun NoticeCard(
    notice: NoticeUi,
    onAcceptClick: (String) -> Unit,
    onDeclineClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(180.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        if (notice.isPoll) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = notice.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notice.details,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )

                    if (notice.acceptedBy.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Going: ${notice.acceptedBy.joinToString { it.name }}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onAcceptClick(notice.id) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("I'm in!", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { onDeclineClick(notice.id) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Not going", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = notice.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = notice.details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// NUEVO: Dialogo para rellenar los datos del Anuncio
@Composable
fun AddNoticeDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, details: String, isPoll: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var isPoll by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Notice") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Details") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = isPoll, onCheckedChange = { isPoll = it })
                    Text("Is this a poll? (RSVP)")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank() && details.isNotBlank()) {
                    onConfirm(title, details, isPoll)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun UpcomingEventsSection(events: List<UpcomingEventUi>, context: Context, viewModel: HomeViewModel) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val dateLabel = result.data?.getStringExtra(AddEventActivity.EXTRA_DATE_LABEL) ?: ""
            val description = result.data?.getStringExtra(AddEventActivity.EXTRA_DESCRIPTION) ?: ""
            val location = result.data?.getStringExtra(AddEventActivity.EXTRA_LOCATION) ?: ""
            if (dateLabel.isNotEmpty() && description.isNotEmpty()) {
                viewModel.addEvent(dateLabel, description, location)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.home_upcoming_events),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (events.isEmpty()) {
            Text(
                text = "No events scheduled.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                events.forEach { event ->
                    EventCard(event = event, context = context, viewModel = viewModel)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextButton(
                onClick = {
                    val intent = Intent(context, AddEventActivity::class.java)
                    launcher.launch(intent)
                },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.home_add_event),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun EventCard(event: UpcomingEventUi, context: Context, viewModel: HomeViewModel) {
    var isExpanded by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            toggleEventReminder(event, context, viewModel)
        } else {
            Toast.makeText(context, "Permission required for alarms.", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val parts = event.dateLabel.split(" ", limit = 2)
                val day = parts.getOrNull(0) ?: ""
                val time = parts.getOrNull(1) ?: ""

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(50.dp)
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = event.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        when (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
                            PackageManager.PERMISSION_GRANTED -> toggleEventReminder(event, context, viewModel)
                            else -> permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        toggleEventReminder(event, context, viewModel)
                    }
                }) {
                    Icon(
                        imageVector = if (event.isRemindMeEnabled) Icons.Default.Notifications else Icons.Default.NotificationsNone,
                        contentDescription = "Toggle reminder",
                        tint = if (event.isRemindMeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Expand details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                if (event.location.isNotEmpty()) {
                    Text(
                        text = "Location: ${event.location}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (event.attending.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Attending: ${event.attending.joinToString { it.name }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { exportEventToCalendar(context, event) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Export to Calendar",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun toggleEventReminder(event: UpcomingEventUi, context: Context, viewModel: HomeViewModel) {
    if (event.isRemindMeEnabled) {
        cancelReminder(context, event.id.hashCode())
        viewModel.toggleReminder(event.id)
        Toast.makeText(context, context.getString(R.string.home_toast_reminder_canceled), Toast.LENGTH_SHORT).show()
    } else {
        val triggerAtMillis = System.currentTimeMillis() + 10_000
        scheduleReminder(context, event.id.hashCode(), "Upcoming Event: ${event.description}", "Starts at ${event.dateLabel}", triggerAtMillis)
        viewModel.toggleReminder(event.id)
        Toast.makeText(context, context.getString(R.string.home_toast_reminder_set), Toast.LENGTH_SHORT).show()
    }
}

private fun exportEventToCalendar(context: Context, event: UpcomingEventUi) {
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, event.description)
        putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.home_no_calendar_app), Toast.LENGTH_LONG).show()
    }
}

private fun scheduleReminder(context: Context, requestCode: Int, title: String, message: String, triggerAtMillis: Long) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, HomeReminderReceiver::class.java).apply {
        putExtra(HomeReminderReceiver.EXTRA_TITLE, title)
        putExtra(HomeReminderReceiver.EXTRA_MESSAGE, message)
        putExtra(HomeReminderReceiver.EXTRA_NOTIFICATION_ID, requestCode)
    }

    val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
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

private fun cancelReminder(context: Context, requestCode: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, HomeReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    alarmManager.cancel(pendingIntent)
}