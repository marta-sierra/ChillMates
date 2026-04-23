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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.uc3m.android.chillmates.NavGraph
import es.uc3m.android.chillmates.R
import es.uc3m.android.chillmates.ui.theme.newsBoardButtonBackground
import es.uc3m.android.chillmates.ui.theme.newsBoardCardBackground

@Composable
fun HomePage(navController: NavController) {
    val vm: HomeViewModel = viewModel()
    val context = LocalContext.current
    var pendingReminder by remember { mutableStateOf<HomeViewModel.HomeAction.ScheduleReminder?>(null) }
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle

    val returnedDateLabel = savedStateHandle?.get<String>(NavGraph.RESULT_EVENT_DATE_LABEL)
    val returnedDescription = savedStateHandle?.get<String>(NavGraph.RESULT_EVENT_DESCRIPTION)
    val returnedLocation = savedStateHandle?.get<String>(NavGraph.RESULT_EVENT_LOCATION)

    LaunchedEffect(returnedDateLabel, returnedDescription, returnedLocation) {
        if (returnedDateLabel != null && returnedDescription != null && returnedLocation != null) {
            vm.onAddEvent(
                dateLabel = returnedDateLabel,
                description = returnedDescription,
                location = returnedLocation
            )
            savedStateHandle?.remove<String>(NavGraph.RESULT_EVENT_DATE_LABEL)
            savedStateHandle?.remove<String>(NavGraph.RESULT_EVENT_DESCRIPTION)
            savedStateHandle?.remove<String>(NavGraph.RESULT_EVENT_LOCATION)
        }
    }

    val requestNotificationsPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingReminder?.let { action ->
                scheduleReminder(context, action.requestCode, action.title, action.message, action.triggerAtMillis)
                pendingReminder = null
            }
        } else {
            Toast.makeText(context, "Notifications permission denied", Toast.LENGTH_SHORT).show()
            pendingReminder = null
        }
    }


    fun hasNotificationsPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    LaunchedEffect(vm.action) {
        val action = vm.action ?: return@LaunchedEffect

        when (action) {
            is HomeViewModel.HomeAction.AddToCalendar -> {
                openCalendarInsert(context, action.title, action.description, action.beginMillis, action.endMillis)
            }
            is HomeViewModel.HomeAction.ScheduleReminder -> {
                if (!hasNotificationsPermission()) {
                    pendingReminder = action
                    requestNotificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    scheduleReminder(context, action.requestCode, action.title, action.message, action.triggerAtMillis)
                }
            }
            is HomeViewModel.HomeAction.CancelReminder -> {
                cancelReminder(context, action.requestCode)
            }
            else -> {}
        }
        vm.onActionConsumed()
    }

    HomeRoute(
        state = vm.uiState,
        onAcceptNotice = vm::onAcceptNotice,
        onRejectNotice = vm::onRejectNotice,
        onShowAllNotices = vm::onShowAllNotices,
        onAddNotice = vm::onAddNotice,
        onAddToCalendar = vm::onAddToCalendar,
        onViewMoreDetails = vm::onViewMoreDetails,
        onToggleRemindMe = vm::onToggleRemindMe,
        onAddEvent = {
            navController.navigate(NavGraph.AddEvent.route)
        }
    )
}

@Composable
private fun HomeRoute(
    state: HomeUiState,
    onAcceptNotice: (String) -> Unit,
    onRejectNotice: (String) -> Unit,
    onShowAllNotices: () -> Unit,
    onAddNotice: (String, String) -> Unit,
    onAddToCalendar: (String) -> Unit,
    onViewMoreDetails: (String) -> Unit,
    onToggleRemindMe: (String) -> Unit,
    onAddEvent: () -> Unit
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
                onToggleShowAll = onShowAllNotices,
                onAddNotice = onAddNotice
            )
        }
        item {
            UpcomingEventsSection(
                events = state.upcomingEvents,
                expandedEventId = state.expandedEventId,
                onAddToCalendar = onAddToCalendar,
                onViewMoreDetails = onViewMoreDetails,
                onToggleRemindMe = onToggleRemindMe,
                onAddEvent = onAddEvent
            )
        }
    }
}

@Composable
private fun NewsBoardSection(
    notices: List<NoticeUi>,
    showAll: Boolean,
    onAcceptNotice: (String) -> Unit,
    onRejectNotice: (String) -> Unit,
    onToggleShowAll: () -> Unit,
    onAddNotice: (String, String) -> Unit
) {
    var showAddNoticeDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.home_section_news_board),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            if (notices.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF59D)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No news right now.\nTap '+' to add something!",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF5D4037)
                        )
                    }
                }
            } else {
                val visibleNotices = if (showAll) notices else listOfNotNull(notices.firstOrNull())
                visibleNotices.forEach { notice ->
                    NewsBoardCard(
                        notice = notice,
                        onAccept = { onAcceptNotice(notice.id) },
                        onReject = { onRejectNotice(notice.id) }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    val label = if (showAll) stringResource(R.string.home_show_less) else stringResource(R.string.home_show_all)
                    TextButton(onClick = onToggleShowAll) { Text(label) }
                }
            }
        }

        SmallFloatingActionButton(
            onClick = { showAddNoticeDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).offset(y = (-40).dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Notice")
        }
    }

    if (showAddNoticeDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newDetails by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddNoticeDialog = false },
            title = { Text("Add Notice") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDetails,
                        onValueChange = { newDetails = it },
                        label = { Text("Details") },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onAddNotice(newTitle, newDetails)
                            showAddNoticeDialog = false
                        }
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoticeDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun NewsBoardCard(notice: NoticeUi, onAccept: () -> Unit, onReject: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = newsBoardCardBackground),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "“${notice.title}”", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(text = "“${notice.details}”", style = MaterialTheme.typography.bodyMedium)
            AttendingLine(members = notice.acceptedBy)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = newsBoardButtonBackground,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(stringResource(R.string.home_accept_label))
                }

                FilledTonalButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = newsBoardButtonBackground,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(stringResource(R.string.home_reject_label))
                }
            }
        }
    }
}

@Composable
private fun AttendingLine(members: List<MemberUi>) {
    val names = remember(members) { members.joinToString(" • ") { it.name } }
    val displayNames = if (names.isBlank()) stringResource(R.string.home_attending_empty) else names
    Text(
        text = stringResource(R.string.home_attending_prefix, displayNames),
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun UpcomingEventsSection(
    events: List<UpcomingEventUi>,
    expandedEventId: String?,
    onAddToCalendar: (String) -> Unit,
    onViewMoreDetails: (String) -> Unit,
    onToggleRemindMe: (String) -> Unit,
    onAddEvent: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.home_section_upcoming_events),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onAddEvent) { Text(stringResource(R.string.home_btn_add_event)) }
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
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
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
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

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = event.dateLabel, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(text = event.description, style = MaterialTheme.typography.bodyMedium)
            }

            IconButton(onClick = onAddToCalendar) {
                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.home_cd_calendar))
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = stringResource(R.string.home_cd_more))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(if (expanded) R.string.home_hide_details else R.string.home_view_details)) },
                        onClick = { menuExpanded = false; onViewMoreDetails() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(if (event.isRemindMeEnabled) R.string.home_stop_notifying else R.string.home_remind_me)) },
                        onClick = { menuExpanded = false; onToggleRemindMe() }
                    )
                }
            }
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.home_location_prefix, event.location), style = MaterialTheme.typography.bodySmall)
            val attendingNames = remember(event.attending) { event.attending.joinToString(" • ") { it.name } }
            val displayNames = if (attendingNames.isBlank()) stringResource(R.string.home_attending_empty) else attendingNames
            Text(text = stringResource(R.string.home_attending_prefix, displayNames), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun openCalendarInsert(context: Context, title: String, description: String, beginMillis: Long, endMillis: Long) {
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, title)
        putExtra(CalendarContract.Events.DESCRIPTION, description)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        putExtra(CalendarContract.Events.CUSTOM_APP_PACKAGE, context.packageName)
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

    if (canExact) alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    else alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
}

private fun cancelReminder(context: Context, requestCode: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, HomeReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    alarmManager.cancel(pendingIntent)
    pendingIntent.cancel()
}
