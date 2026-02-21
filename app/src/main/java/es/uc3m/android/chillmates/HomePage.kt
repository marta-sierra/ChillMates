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


@Composable
fun HomePage(navController: NavController) {
    val vm: HomeViewModel = viewModel()

    // events
    HomeRoute(
        state = vm.uiState,
        onAcceptNotice = vm::onAcceptNotice,
        onRejectNotice = vm::onRejectNotice,
        onShowAllNotices = vm::onShowAllNotices,
        onAddToCalendar = vm::onAddToCalendar,
        onViewMoreDetails = vm::onViewMoreDetails,
        onToggleRemindMe = vm::onToggleRemindMe
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
    onToggleRemindMe: (String) -> Unit
) {
    // Screen layout: vertical scroll with two main sections (News Board + Upcoming Events)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            NewsBoardSection(
                latestNotice = state.notices.firstOrNull(), // only show last new
                onAcceptNotice = onAcceptNotice,
                onRejectNotice = onRejectNotice,
                onShowAll = onShowAllNotices
            )
        }

        item {
            UpcomingEventsSection(
                events = state.upcomingEvents,
                onAddToCalendar = onAddToCalendar,
                onViewMoreDetails = onViewMoreDetails,
                onToggleRemindMe = onToggleRemindMe
            )
        }
    }
}

// SECTION: News Board -----
@Composable
private fun NewsBoardSection(
    latestNotice: NoticeUi?,
    onAcceptNotice: (String) -> Unit,
    onRejectNotice: (String) -> Unit,
    onShowAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "News Board",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
        )

        // last new
        if (latestNotice != null) {
            NewsBoardCard(
                notice = latestNotice,
                onAccept = { onAcceptNotice(latestNotice.id) },
                onReject = { onRejectNotice(latestNotice.id) }
            )
        }

        // "Show all" news button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onShowAll) { Text("Show all") }
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

            // event details
            Text(
                text = "“${notice.details}”",
                style = MaterialTheme.typography.bodyMedium
            )

            AttendingLine(members = notice.acceptedBy)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Buttons area
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

// People attending an event
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
    onAddToCalendar: (String) -> Unit,
    onViewMoreDetails: (String) -> Unit,
    onToggleRemindMe: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Upcoming Events  →",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                events.forEachIndexed { index, event ->
                    UpcomingEventRow(
                        event = event,
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

// Individual events
@Composable
private fun UpcomingEventRow(
    event: UpcomingEventUi,
    onAddToCalendar: () -> Unit,
    onViewMoreDetails: () -> Unit,
    onToggleRemindMe: () -> Unit
) {
    // Upcoming event row: date/description + calendar button + overflow menu
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Event description
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

        // Calendar
        IconButton(onClick = onAddToCalendar) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "Add to calendar"
            )
        }

        // More options
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
                    text = { Text("View more details") },
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
}