package es.uc3m.android.chillmates.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.chillmates.model.Notice
import es.uc3m.android.chillmates.model.UpcomingEvent
import es.uc3m.android.chillmates.model.USERS_COLLECTION
import es.uc3m.android.chillmates.repository.FlatRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Locale

@Immutable
data class NoticeUi(
    val id: String,
    val title: String,
    val details: String,
    val acceptedBy: List<MemberUi> = emptyList()
)

@Immutable
data class MemberUi(
    val name: String
)

@Immutable
data class UpcomingEventUi(
    val id: String,
    val dateLabel: String,
    val description: String,
    val isRemindMeEnabled: Boolean = false,
    val location: String = "",
    val attending: List<MemberUi> = emptyList()
)

@Immutable
data class HomeUiState(
    val notices: List<NoticeUi> = emptyList(),
    val upcomingEvents: List<UpcomingEventUi> = emptyList(),
    val showAllNotices: Boolean = false,
    val expandedEventId: String? = null,
    val currentUserName: String = "Me"
)

class HomeViewModel(
    private val repository: FlatRepository = FlatRepository()
) : ViewModel() {

    var uiState by mutableStateOf(HomeUiState())
        private set

    sealed class HomeAction {
        data class AddToCalendar(
            val title: String,
            val description: String,
            val beginMillis: Long,
            val endMillis: Long
        ) : HomeAction()

        data class ScheduleReminder(
            val requestCode: Int,
            val title: String,
            val message: String,
            val triggerAtMillis: Long
        ) : HomeAction()

        data class CancelReminder(val requestCode: Int) : HomeAction()
    }

    var action: HomeAction? by mutableStateOf(null)
        private set

    private var currentFlatId: String? = null
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val localReminderStates = mutableMapOf<String, Boolean>()

    init {
        loadUserAndFlatData()
    }

    private fun loadUserAndFlatData() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                val userDoc = db.collection(USERS_COLLECTION).document(userId).get().await()
                val flatId = userDoc.getString("flatId")
                val userName = userDoc.getString("displayName") ?: "Me"

                if (flatId != null) {
                    currentFlatId = flatId

                    repository.observeFlat(flatId).collect { flat ->
                        if (flat != null) {
                            // Map Firebase Notices to UI Notices
                            val mappedNotices = flat.notices.map { dbNotice ->
                                NoticeUi(dbNotice.id, dbNotice.title, dbNotice.details, dbNotice.acceptedBy.map { MemberUi(it) })
                            }

                            // Map Firebase Events to UI Events
                            val mappedEvents = flat.events.map { dbEvent ->
                                val isReminded = localReminderStates[dbEvent.id] ?: false
                                UpcomingEventUi(
                                    id = dbEvent.id,
                                    dateLabel = dbEvent.dateLabel,
                                    description = dbEvent.description,
                                    location = dbEvent.location,
                                    attending = dbEvent.attending.map { MemberUi(it) },
                                    isRemindMeEnabled = isReminded
                                )
                            }.sortedBy { computeNextOccurrenceMillis(it.dateLabel, 60) } // Keep sorted automatically

                            uiState = uiState.copy(
                                notices = mappedNotices,
                                upcomingEvents = mappedEvents,
                                currentUserName = userName
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onActionConsumed() {
        action = null
    }

    fun onAcceptNotice(noticeId: String) {
        val flatId = currentFlatId ?: return
        val userName = uiState.currentUserName

        val updatedDbNotices = uiState.notices.map { n ->
            val already = n.acceptedBy.any { it.name == userName }
            val newAccepted = if (already) n.acceptedBy.map { it.name } else n.acceptedBy.map { it.name } + userName
            Notice(n.id, n.title, n.details, newAccepted)
        }

        viewModelScope.launch { repository.updateNotices(flatId, updatedDbNotices) }
    }

    fun onRejectNotice(noticeId: String) {
        val flatId = currentFlatId ?: return
        val userName = uiState.currentUserName

        val updatedDbNotices = uiState.notices.map { n ->
            val newAccepted = n.acceptedBy.filterNot { it.name == userName }.map { it.name }
            Notice(n.id, n.title, n.details, newAccepted)
        }

        viewModelScope.launch { repository.updateNotices(flatId, updatedDbNotices) }
    }

    fun onShowAllNotices() {
        uiState = uiState.copy(showAllNotices = !uiState.showAllNotices)
    }

    // --- NEW: Manually add a notice to Firebase ---
    fun onAddNotice(title: String, details: String) {
        val flatId = currentFlatId ?: return
        val newNotice = Notice(
            id = "n_${System.currentTimeMillis()}",
            title = title,
            details = details,
            acceptedBy = emptyList()
        )
        val currentDbNotices = uiState.notices.map { Notice(it.id, it.title, it.details, it.acceptedBy.map { m -> m.name }) }
        viewModelScope.launch { repository.updateNotices(flatId, listOf(newNotice) + currentDbNotices) }
    }

    fun onAddToCalendar(eventId: String) {
        val e = uiState.upcomingEvents.firstOrNull { it.id == eventId } ?: return
        val beginMillis = computeNextOccurrenceMillis(e.dateLabel, durationMinutes = 60)
        val endMillis = beginMillis + 60L * 60L * 1000L

        action = HomeAction.AddToCalendar(
            title = e.description,
            description = "${e.dateLabel} • ${e.location}",
            beginMillis = beginMillis,
            endMillis = endMillis
        )
    }

    fun onViewMoreDetails(eventId: String) {
        uiState = uiState.copy(
            expandedEventId = if (uiState.expandedEventId == eventId) null else eventId
        )
    }

    fun onToggleRemindMe(eventId: String) {
        val event = uiState.upcomingEvents.firstOrNull { it.id == eventId } ?: return
        val isNowEnabled = !event.isRemindMeEnabled
        localReminderStates[eventId] = isNowEnabled

        // Optimistic UI Update for the toggle
        uiState = uiState.copy(
            upcomingEvents = uiState.upcomingEvents.map { e ->
                if (e.id == eventId) e.copy(isRemindMeEnabled = isNowEnabled) else e
            }
        )

        val requestCode = eventId.hashCode()

        if (isNowEnabled) {
            val eventStart = computeNextOccurrenceMillis(event.dateLabel, durationMinutes = 60)
            val triggerAtMillis = eventStart - 24L * 60L * 60L * 1000L

            action = HomeAction.ScheduleReminder(
                requestCode = requestCode,
                title = "ChillMates reminder",
                message = "${event.description} • ${event.dateLabel}",
                triggerAtMillis = triggerAtMillis
            )
        } else {
            action = HomeAction.CancelReminder(requestCode = requestCode)
        }
    }

    fun onAddEvent(dateLabel: String, description: String, location: String) {
        val flatId = currentFlatId ?: return
        val cleanDateLabel = dateLabel.trim()
        val cleanDescription = description.trim()
        val cleanLocation = location.trim()

        if (cleanDateLabel.isBlank() || cleanDescription.isBlank()) return

        // Save to Firebase (Event)
        val newDbEvent = UpcomingEvent(
            id = "e_${System.currentTimeMillis()}",
            dateLabel = cleanDateLabel,
            description = cleanDescription,
            location = cleanLocation,
            attending = emptyList()
        )

        // Save to Firebase (Auto-Notice)
        val newDbNotice = Notice(
            id = "n_${System.currentTimeMillis()}",
            title = "New event added",
            details = "$cleanDescription • $cleanDateLabel",
            acceptedBy = emptyList()
        )

        val currentDbEvents = uiState.upcomingEvents.map { UpcomingEvent(it.id, it.dateLabel, it.description, it.location, it.attending.map { m -> m.name }) }
        val currentDbNotices = uiState.notices.map { Notice(it.id, it.title, it.details, it.acceptedBy.map { m -> m.name }) }

        viewModelScope.launch {
            repository.updateEvents(flatId, currentDbEvents + newDbEvent)
            repository.updateNotices(flatId, listOf(newDbNotice) + currentDbNotices)
        }
    }

    private fun computeNextOccurrenceMillis(dateLabel: String, durationMinutes: Int): Long {
        val parts = dateLabel.trim().split(" ")
        val dayPart = parts.getOrNull(0).orEmpty()
        val timePart = parts.getOrNull(1).orEmpty()

        val dayOfWeek = when (dayPart.lowercase(Locale.ROOT).take(3)) {
            "mon" -> Calendar.MONDAY
            "tue" -> Calendar.TUESDAY
            "wed" -> Calendar.WEDNESDAY
            "thu" -> Calendar.THURSDAY
            "fri" -> Calendar.FRIDAY
            "sat" -> Calendar.SATURDAY
            "sun" -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }

        val hour = timePart.substringBefore(":", "12").toIntOrNull() ?: 12
        val minute = timePart.substringAfter(":", "00").toIntOrNull() ?: 0

        val now = Calendar.getInstance()
        val cal = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }

        val currentDow = cal.get(Calendar.DAY_OF_WEEK)
        var delta = (dayOfWeek - currentDow + 7) % 7
        if (delta == 0 && cal.timeInMillis <= now.timeInMillis) delta = 7
        cal.add(Calendar.DAY_OF_YEAR, delta)

        return cal.timeInMillis
    }
}