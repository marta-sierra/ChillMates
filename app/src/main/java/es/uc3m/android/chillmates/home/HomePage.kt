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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowForward
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.chillmates.NavGraph
import es.uc3m.android.chillmates.R
import es.uc3m.android.chillmates.model.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

@Composable
fun HomePage(navController: NavController, viewModel: HomeViewModel = viewModel()) {
    val uiState = viewModel.uiState
    val context = LocalContext.current
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    var showAddNoticeDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val returnedEventDateLabel = savedStateHandle?.get<String>(NavGraph.RESULT_EVENT_DATE_LABEL)
    val returnedEventDescription = savedStateHandle?.get<String>(NavGraph.RESULT_EVENT_DESCRIPTION)
    val returnedEventLocation = savedStateHandle?.get<String>(NavGraph.RESULT_EVENT_LOCATION)

    LaunchedEffect(returnedEventDateLabel, returnedEventDescription, returnedEventLocation) {
        if (
            !returnedEventDateLabel.isNullOrBlank() &&
            !returnedEventDescription.isNullOrBlank()
        ) {
            viewModel.addEvent(
                dateLabel = returnedEventDateLabel,
                description = returnedEventDescription,
                location = returnedEventLocation.orEmpty()
            )
            savedStateHandle?.remove<String>(NavGraph.RESULT_EVENT_DATE_LABEL)
            savedStateHandle?.remove<String>(NavGraph.RESULT_EVENT_DESCRIPTION)
            savedStateHandle?.remove<String>(NavGraph.RESULT_EVENT_LOCATION)
        }
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    if (uiState.errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "${stringResource(R.string.home_error_prefix)}${uiState.errorMessage}", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
        item {
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val db = FirebaseFirestore.getInstance()
                            val auth = FirebaseAuth.getInstance()
                            val currentUserId = auth.currentUser?.uid ?: return@launch

                            val flatId = "flat_mock_massive_001"
                            val uAlice = "user_mock_alice"
                            val uBob = "user_mock_bob"
                            val uCharlie = "user_mock_charlie"
                            val uDiana = "user_mock_diana"

                            val nMe = "Me"
                            val nAlice = "Alice"
                            val nBob = "Bob"
                            val nCharlie = "Charlie"
                            val nDiana = "Diana"

                            db.collection(USERS_COLLECTION).document(currentUserId).update(
                                mapOf("flatId" to flatId, "flatName" to "ChillMates Villa")
                            ).await()

                            db.collection(USERS_COLLECTION).document(uAlice).set(mapOf("displayName" to nAlice, "flatId" to flatId, "flatName" to "ChillMates Villa", "email" to "alice@mock.com")).await()
                            db.collection(USERS_COLLECTION).document(uBob).set(mapOf("displayName" to nBob, "flatId" to flatId, "flatName" to "ChillMates Villa", "email" to "bob@mock.com")).await()
                            db.collection(USERS_COLLECTION).document(uCharlie).set(mapOf("displayName" to nCharlie, "flatId" to flatId, "flatName" to "ChillMates Villa", "email" to "charlie@mock.com")).await()
                            db.collection(USERS_COLLECTION).document(uDiana).set(mapOf("displayName" to nDiana, "flatId" to flatId, "flatName" to "ChillMates Villa", "email" to "diana@mock.com")).await()

                            val flat = Flat(
                                id = flatId,
                                name = "ChillMates Villa",
                                inviteCode = "CHILL55",
                                memberIds = listOf(currentUserId, uAlice, uBob, uCharlie, uDiana),
                                supermarketIds = emptyList(),
                                lastRotationTimestamp = System.currentTimeMillis()
                            )
                            db.collection(FLATS_COLLECTION).document(flatId).set(flat).await()

                            val supermarkets = listOf(
                                Supermarket(id = "sm_1", name = "Mercadona", officialName = "Mercadona S.A.", placeId = "p_1", latitude = 40.416, longitude = -3.703, address = "Calle 1"),
                                Supermarket(id = "sm_2", name = "Carrefour", officialName = "Carrefour Express", placeId = "p_2", latitude = 40.418, longitude = -3.705, address = "Calle 2"),
                                Supermarket(id = "sm_3", name = "Lidl", officialName = "Lidl Supermercados", placeId = "p_3", latitude = 40.420, longitude = -3.710, address = "Calle 3"),
                                Supermarket(id = "sm_4", name = "Aldi", officialName = "Aldi", placeId = "p_4", latitude = 40.410, longitude = -3.700, address = "Calle 4"),
                                Supermarket(id = "sm_5", name = "Dia", officialName = "Supermercados Dia", placeId = "p_5", latitude = 40.400, longitude = -3.720, address = "Calle 5")
                            )
                            val smIds = supermarkets.map { it.id!! }
                            supermarkets.forEach { db.collection("supermarkets").document(it.id!!).set(it).await() }
                            db.collection(FLATS_COLLECTION).document(flatId).update("supermarketIds", smIds).await()

                            val cal = Calendar.getInstance().apply {
                                firstDayOfWeek = Calendar.MONDAY
                                minimalDaysInFirstWeek = 4
                            }
                            val currentWeek = String.format("%04d-W%02d", cal.get(Calendar.YEAR), cal.get(Calendar.WEEK_OF_YEAR))
                            cal.add(Calendar.WEEK_OF_YEAR, -1)
                            val past1 = String.format("%04d-W%02d", cal.get(Calendar.YEAR), cal.get(Calendar.WEEK_OF_YEAR))
                            cal.add(Calendar.WEEK_OF_YEAR, -1)
                            val past2 = String.format("%04d-W%02d", cal.get(Calendar.YEAR), cal.get(Calendar.WEEK_OF_YEAR))
                            cal.add(Calendar.WEEK_OF_YEAR, -1)
                            val past3 = String.format("%04d-W%02d", cal.get(Calendar.YEAR), cal.get(Calendar.WEEK_OF_YEAR))

                            val flatRef = db.collection(FLATS_COLLECTION).document(flatId)

                            val c1 = Chore(id = "c_1", title = "Clean Kitchen", assignee = "Everyone", done = false, assignments = mapOf(
                                currentWeek to ChoreAssignment(assignee = "Everyone", done = false),
                                past1 to ChoreAssignment(assignee = "Everyone", done = true),
                                past2 to ChoreAssignment(assignee = "Everyone", done = false),
                                past3 to ChoreAssignment(assignee = "Everyone", done = true)
                            ))
                            val c2 = Chore(id = "c_2", title = "Take out Trash", assignee = nAlice, done = true, assignments = mapOf(
                                currentWeek to ChoreAssignment(assignee = nAlice, done = true),
                                past1 to ChoreAssignment(assignee = nBob, done = true),
                                past2 to ChoreAssignment(assignee = nCharlie, done = false),
                                past3 to ChoreAssignment(assignee = nDiana, done = true)
                            ))
                            val c3 = Chore(id = "c_3", title = "Vacuum Living Room", assignee = nMe, done = false, assignments = mapOf(
                                currentWeek to ChoreAssignment(assignee = nMe, done = false),
                                past1 to ChoreAssignment(assignee = nDiana, done = true),
                                past2 to ChoreAssignment(assignee = nBob, done = true)
                            ))
                            val c4 = Chore(id = "c_4", title = "Clean Bathrooms", assignee = nCharlie, done = false, assignments = mapOf(
                                currentWeek to ChoreAssignment(assignee = nCharlie, done = false),
                                past1 to ChoreAssignment(assignee = nMe, done = true)
                            ))
                            val c5 = Chore(id = "c_5", title = "Mop the floors", assignee = nDiana, done = true, assignments = mapOf(
                                currentWeek to ChoreAssignment(assignee = nDiana, done = true),
                                past1 to ChoreAssignment(assignee = nAlice, done = false)
                            ))

                            val chores = listOf(c1, c2, c3, c4, c5)
                            chores.forEach { flatRef.collection(CHORES_SUBCOLLECTION).document(it.id).set(it).await() }

                            val r1 = Repair(id = "r_1", title = "Broken Shower", description = "Water leaking", reportedBy = nAlice, status = "Pending")
                            val r2 = Repair(id = "r_2", title = "Oven light", description = "Bulb needs replacement", reportedBy = nMe, status = "In Progress")
                            val r3 = Repair(id = "r_3", title = "Heater not working", description = "Cold air only", reportedBy = nCharlie, status = "Resolved")
                            val r4 = Repair(id = "r_4", title = "Loose door handle", description = "Front door handle is falling off", reportedBy = nDiana, status = "Pending")
                            listOf(r1, r2, r3, r4).forEach { flatRef.collection(REPAIRS_SUBCOLLECTION).document(it.id).set(it).await() }

                            val e1 = Expense(id = "e_1", title = "Internet Bill", amount = 45.00, paidBy = nMe, splitBetween = listOf(nMe, nAlice, nBob, nCharlie, nDiana))
                            val e2 = Expense(id = "e_2", title = "Groceries", amount = 120.50, paidBy = nBob, splitBetween = listOf(nMe, nAlice, nBob, nCharlie, nDiana))
                            val e3 = Expense(id = "e_3", title = "Cleaning Supplies", amount = 15.00, paidBy = nDiana, splitBetween = listOf(nMe, nDiana))
                            val e4 = Expense(id = "e_4", title = "Electricity", amount = 85.20, paidBy = nCharlie, splitBetween = listOf(nMe, nAlice, nBob, nCharlie, nDiana))
                            listOf(e1, e2, e3, e4).forEach { flatRef.collection(EXPENSES_SUBCOLLECTION).document(it.id).set(it).await() }

                            val s1 = ShoppingItem(id = "s_1", name = "Milk", quantity = "4 cartons", neededBy = listOf("Everyone"), supermarkets = listOf("Mercadona", "Lidl"), bought = false)
                            val s2 = ShoppingItem(id = "s_2", name = "Eggs", quantity = "2 dozen", neededBy = listOf(nMe, nAlice), supermarkets = listOf("Carrefour"), bought = false)
                            val s3 = ShoppingItem(id = "s_3", name = "Coffee", quantity = "2 bags", neededBy = listOf(nBob), supermarkets = listOf("Dia"), bought = true)
                            val s4 = ShoppingItem(id = "s_4", name = "Toilet Paper", quantity = "12 rolls", neededBy = listOf("Everyone"), supermarkets = emptyList(), bought = false)
                            val s5 = ShoppingItem(id = "s_5", name = "Chicken Breasts", quantity = "1 kg", neededBy = listOf(nCharlie, nDiana), supermarkets = listOf("Aldi", "Mercadona"), bought = true)
                            val s6 = ShoppingItem(id = "s_6", name = "Olive Oil", quantity = "3 liters", neededBy = listOf("Everyone"), supermarkets = listOf("Mercadona"), bought = false)
                            listOf(s1, s2, s3, s4, s5, s6).forEach { flatRef.collection(SHOPPING_SUBCOLLECTION).document(it.id).set(it).await() }

                            val n1 = Notice(id = "n_1", title = "Landlord Visit", details = "Checking boiler tomorrow at 10 AM.", poll = false, acceptedBy = emptyList(), declinedBy = emptyList())
                            val n2 = Notice(id = "n_2", title = "Pizza Night?", details = "Should we order pizza this Friday?", poll = true, acceptedBy = listOf(nMe, nAlice, nDiana), declinedBy = listOf(nBob))
                            val n3 = Notice(id = "n_3", title = "Quiet Hours", details = "I have an exam on Thursday, please be quiet on Wed night.", poll = false, acceptedBy = emptyList(), declinedBy = emptyList())
                            listOf(n1, n2, n3).forEach { flatRef.collection(NOTICES_SUBCOLLECTION).document(it.id).set(it).await() }

                            val ev1 = UpcomingEvent(id = "ev_1", dateLabel = "15 May 22:00", description = "House Party", location = "ChillMates Villa", attending = listOf(nMe, nAlice, nBob, nCharlie, nDiana))
                            val ev2 = UpcomingEvent(id = "ev_2", dateLabel = "20 May 14:00", description = "Deep Cleaning Day", location = "Whole House", attending = listOf(nMe, nDiana))
                            val ev3 = UpcomingEvent(id = "ev_3", dateLabel = "25 May 20:30", description = "Flat Dinner", location = "Dining Room", attending = listOf(nAlice, nBob, nCharlie))
                            listOf(ev1, ev2, ev3).forEach { flatRef.collection(EVENTS_SUBCOLLECTION).document(it.id).set(it).await() }

                            Toast.makeText(context, "Mock Data Generated!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Generate Mock Data", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
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
            UpcomingEventsSection(
                events = uiState.upcomingEvents,
                context = context,
                viewModel = viewModel,
                onAddEventClick = { navController.navigate(NavGraph.AddEvent.route) }
            )
        }
    }

    if (showAddNoticeDialog) {
        AddNoticeDialog(
            onDismiss = { showAddNoticeDialog = false },
            onConfirm = { title, details, poll ->
                viewModel.addNotice(title, details, poll)
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
    var currentIndex by remember { mutableIntStateOf(0) }

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
                text = stringResource(R.string.home_notice_empty),
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (notices.size > 1) {
                        IconButton(
                            onClick = { currentIndex = (currentIndex - 1 + notices.size) % notices.size },
                            enabled = notices.size > 1
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = stringResource(R.string.home_cd_previous),
                                tint = if (notices.size > 1) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    NoticeCard(
                        notice = notices[currentIndex],
                        onAcceptClick = onAcceptClick,
                        onDeclineClick = onDeclineClick,
                        modifier = Modifier.width(280.dp)
                    )

                    if (notices.size > 1) {
                        IconButton(
                            onClick = { currentIndex = (currentIndex + 1) % notices.size }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = stringResource(R.string.home_cd_next),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }
            }

            if (notices.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(notices.size) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(4.dp),
                                color = if (index == currentIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            ) {}
                        }
                    }
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
                    text = stringResource(R.string.home_add_notice),
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
    onDeclineClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(180.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        if (notice.poll) {
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

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${stringResource(R.string.home_poll_going)}${if (notice.acceptedBy.isEmpty()) "-" else notice.acceptedBy.joinToString { it.name }}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${stringResource(R.string.home_poll_not_going_prefix)}${if (notice.declinedBy.isEmpty()) "-" else notice.declinedBy.joinToString { it.name }}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                        Text(stringResource(R.string.home_poll_im_in), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { onDeclineClick(notice.id) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(stringResource(R.string.home_poll_not_going_btn), maxLines = 1, overflow = TextOverflow.Ellipsis)
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

@Composable
fun AddNoticeDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, details: String, poll: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var poll by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_new_notice_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.home_notice_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text(stringResource(R.string.home_notice_details_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = poll, onCheckedChange = { poll = it })
                    Text(stringResource(R.string.home_notice_poll_label))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank() && details.isNotBlank()) {
                    onConfirm(title, details, poll)
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
fun UpcomingEventsSection(
    events: List<UpcomingEventUi>,
    context: Context,
    viewModel: HomeViewModel,
    onAddEventClick: () -> Unit
) {
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
                text = stringResource(R.string.home_events_empty),
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
                onClick = onAddEventClick,
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
            Toast.makeText(context, context.getString(R.string.home_permission_denied), Toast.LENGTH_SHORT).show()
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
                        contentDescription = stringResource(R.string.home_cd_toggle_reminder),
                        tint = if (event.isRemindMeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.home_cd_expand_details),
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
                        text = "${stringResource(R.string.home_location_prefix)}${event.location}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (event.attending.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${stringResource(R.string.home_attending_prefix)}${event.attending.joinToString { it.name }}",
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
                        text = stringResource(R.string.home_export_calendar),
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
        scheduleReminder(context, event.id.hashCode(), "${context.getString(R.string.home_notification_prefix)}${event.description}", "Starts at ${event.dateLabel}", triggerAtMillis)
        viewModel.toggleReminder(event.id)
        Toast.makeText(context, context.getString(R.string.home_toast_reminder_set), Toast.LENGTH_SHORT).show()
    }
}

private fun exportEventToCalendar(context: Context, event: UpcomingEventUi) {
    val startMillis = extractEventStartMillis(event.dateLabel)
    val endMillis = startMillis + 60 * 60 * 1000

    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, event.description)
        putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
    }

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.home_no_calendar_app), Toast.LENGTH_LONG).show()
    }
}

private fun extractEventStartMillis(dateLabel: String): Long {
    val timeRegex = Regex("""(\d{1,2})[:.]?(\d{2})?""")
    val match = timeRegex.find(dateLabel)

    val hour = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 9
    val minute = match?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0

    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        set(Calendar.MINUTE, minute.coerceIn(0, 59))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
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