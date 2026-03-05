package es.uc3m.android.chillmates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.uc3m.android.chillmates.ui.theme.newsBoardButtonBackground
import es.uc3m.android.chillmates.ui.theme.newsBoardCardBackground

// home actions (intents + alarms + notifications permission)
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun HomePage(navController: NavController) {
    val vm: HomeViewModel = viewModel()

    val context = LocalContext.current

    // add event flow
    val addEventLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data ?: return@rememberLauncherForActivityResult
            val dateLabel = data.getStringExtra(AddEventActivity.EXTRA_DATE_LABEL).orEmpty()
            val description = data.getStringExtra(AddEventActivity.EXTRA_DESCRIPTION).orEmpty()
            val location = data.getStringExtra(AddEventActivity.EXTRA_LOCATION).orEmpty()

            // forward result to ViewModel (state + ordering + news board)
            vm.onAddEvent(dateLabel = dateLabel, description = description, location = location)
        }
    }

    // keep a pending reminder action while we ask for notifications permission
    var pendingReminder by remember { mutableStateOf<HomeViewModel.HomeAction.ScheduleReminder?>(null) }

    // permission request launcher
    val requestNotificationsPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // If user granted permission, schedule the reminder we had pending
            val action = pendingReminder
            if (action != null) {
                scheduleReminder(
                    context = context,
                    requestCode = action.requestCode,
                    title = action.title,
                    message = action.message,
                    triggerAtMillis = action.triggerAtMillis
                )
                pendingReminder = null
            }
        } else {
            Toast.makeText(context, "Notifications permission denied", Toast.LENGTH_SHORT).show()
            pendingReminder = null
        }
    }

    // returns true if we have notifications permission (or it is not needed)
    fun hasNotificationsPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // ViewModel one-shot actions
    LaunchedEffect(vm.action) {
        val action = vm.action ?: return@LaunchedEffect

        when (action) {
            is HomeViewModel.HomeAction.AddToCalendar -> {
                openCalendarInsert(
                    context = context,
                    title = action.title,
                    description = action.description,
                    beginMillis = action.beginMillis,
                    endMillis = action.endMillis
                )
            }

            is HomeViewModel.HomeAction.ScheduleReminder -> {
                if (!hasNotificationsPermission()) {
                    pendingReminder = action
                    requestNotificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    scheduleReminder(
                        context = context,
                        requestCode = action.requestCode,
                        title = action.title,
                        message = action.message,
                        triggerAtMillis = action.triggerAtMillis
                    )
                }
            }

            is HomeViewModel.HomeAction.CancelReminder -> {
                cancelReminder(
                    context = context,
                    requestCode = action.requestCode
                )
            }

            else -> {
                // (kept as-is)
            }
        }

        vm.onActionConsumed()
    }

    HomeRoute(
        state = vm.uiState,
        onAcceptNotice = vm::onAcceptNotice,
        onRejectNotice = vm::onRejectNotice,
        onShowAllNotices = vm::onShowAllNotices,
        onAddToCalendar = vm::onAddToCalendar,
        onViewMoreDetails = vm::onViewMoreDetails,
        onToggleRemindMe = vm::onToggleRemindMe,
        onAddEvent = {
            // add event flow
            val intent = Intent(context, AddEventActivity::class.java)
            addEventLauncher.launch(intent)
        }
    )
}

@Composable
private fun HomeRoute(
    state: HomeUiState,
    onAcceptNotice: (String) -> Unit,
    onRejectNotice: (String) -> Unit,
    onShowAllNotices: () -> Unit,
    onAddToCalendar: (String) -> Unit,
    onViewMoreDetails: (String) -> Unit,
    onToggleRemindMe: (String) -> Unit,
    onAddEvent: () -> Unit // add event action from UI
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            NewsBoardSection(
                notices = state.notices,
                showAll = state.showAllNotices,
                onAcceptNotice = onAcceptNotice,
                onRejectNotice = onRejectNotice,
                onToggleShowAll = onShowAllNotices
            )
        }

        item {
            UpcomingEventsSection(
                events = state.upcomingEvents,
                expandedEventId = state.expandedEventId,
                onAddToCalendar = onAddToCalendar,
                onViewMoreDetails = onViewMoreDetails,
                onToggleRemindMe = onToggleRemindMe,
                onAddEvent = onAddEvent // pass down button action
            )
        }
    }
}

// SECTION: News Board -----
@Composable
private fun NewsBoardSection(
    notices: List<NoticeUi>,
    showAll: Boolean,
    onAcceptNotice: (String) -> Unit,
    onRejectNotice: (String) -> Unit,
    onToggleShowAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "News Board",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
        )

        // news board block ---
        // show either last notice or the full list in-place
        val visibleNotices = if (showAll) notices else listOfNotNull(notices.firstOrNull())

        visibleNotices.forEach { notice ->
            NewsBoardCard(
                notice = notice,
                onAccept = { onAcceptNotice(notice.id) },
                onReject = { onRejectNotice(notice.id) }
            )
        }

        // toggle button changes label based on current mode
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            val label = if (showAll) "Show less" else "Show all"
            TextButton(onClick = onToggleShowAll) { Text(label) }
        }
    }
}

@Composable
private fun NewsBoardCard(
    notice: NoticeUi,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val softYellow = newsBoardCardBackground
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = softYellow),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "“${notice.title}”",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Text(
                text = "“${notice.details}”",
                style = MaterialTheme.typography.bodyMedium
            )

            AttendingLine(members = notice.acceptedBy)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val buttonBg = newsBoardButtonBackground
                val buttonText = MaterialTheme.colorScheme.onSurface

                FilledTonalButton(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = buttonBg,
                        contentColor = buttonText
                    )
                ) {
                    Text("Count with me!")
                }

                FilledTonalButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = buttonBg,
                        contentColor = buttonText
                    )
                ) {
                    Text("Busy, sorry :(")
                }
            }
        }
    }
}

@Composable
private fun AttendingLine(members: List<MemberUi>) {
    val names = remember(members) { members.joinToString(" • ") { it.name } }

    Text(
        text = if (names.isBlank()) "Attending: —" else "Attending: $names",
        style = MaterialTheme.typography.bodySmall
    )
}

// SECTION: Upcoming Events -----
@Composable
private fun UpcomingEventsSection(
    events: List<UpcomingEventUi>,
    expandedEventId: String?,
    onAddToCalendar: (String) -> Unit,
    onViewMoreDetails: (String) -> Unit,
    onToggleRemindMe: (String) -> Unit,
    onAddEvent: () -> Unit // add event button callback
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // upcoming events header - title + "Add event" button row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Upcoming Events  →",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f)
            )

            TextButton(onClick = onAddEvent) {
                Text("Add event")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                events.forEachIndexed { index, event ->
                    UpcomingEventRow(
                        event = event,
                        expanded = (expandedEventId == event.id),
                        onAddToCalendar = { onAddToCalendar(event.id) },
                        onViewMoreDetails = { onViewMoreDetails(event.id) },
                        onToggleRemindMe = { onToggleRemindMe(event.id) }
                    )

                    if (index != events.lastIndex) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingEventRow(
    event: UpcomingEventUi,
    expanded: Boolean,
    onAddToCalendar: () -> Unit,
    onViewMoreDetails: () -> Unit,
    onToggleRemindMe: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.dateLabel,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            IconButton(onClick = onAddToCalendar) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Add to calendar"
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (expanded) "Hide details" else "View more details") },
                        onClick = {
                            menuExpanded = false
                            onViewMoreDetails()
                        }
                    )

                    val remindLabel = if (event.isRemindMeEnabled) "Stop notifying" else "Remind me"
                    DropdownMenuItem(
                        text = { Text(remindLabel) },
                        onClick = {
                            menuExpanded = false
                            onToggleRemindMe()
                        }
                    )
                }
            }
        }

        // upcoming events block ---
        // expanded content shown inside the same card
        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Location: ${event.location}",
                style = MaterialTheme.typography.bodySmall
            )
            val attendingNames = remember(event.attending) { event.attending.joinToString(" • ") { it.name } }
            Text(
                text = if (attendingNames.isBlank()) "Attending: —" else "Attending: $attendingNames",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// upcoming events block ---
// insert an event in Calendar with time + return-to-app
private fun openCalendarInsert(
    context: Context,
    title: String,
    description: String,
    beginMillis: Long,
    endMillis: Long
) {
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, title)
        putExtra(CalendarContract.Events.DESCRIPTION, description)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)

        // hint Calendar to return to this app after saving
        putExtra(CalendarContract.Events.CUSTOM_APP_PACKAGE, context.packageName)
    }

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No Calendar app found", Toast.LENGTH_LONG).show()
    }
}

private fun scheduleReminder(
    context: Context,
    requestCode: Int,
    title: String,
    message: String,
    triggerAtMillis: Long
) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val intent = Intent(context, HomeReminderReceiver::class.java).apply {
        putExtra(HomeReminderReceiver.EXTRA_TITLE, title)
        putExtra(HomeReminderReceiver.EXTRA_MESSAGE, message)
        putExtra(HomeReminderReceiver.EXTRA_NOTIFICATION_ID, requestCode)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    try {
        // Android 12+ can block exact alarms. If blocked, fall back to inexact.
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    } catch (e: SecurityException) {
        // Fallback in case the device/OS policy throws here
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }
}

// cancel an AlarmManager reminder
private fun cancelReminder(context: Context, requestCode: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, HomeReminderReceiver::class.java)

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    alarmManager.cancel(pendingIntent)
    pendingIntent.cancel()
}