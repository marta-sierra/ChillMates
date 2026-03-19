package es.uc3m.android.chillmates.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import es.uc3m.android.chillmates.R
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
    val expandedEventId: String? = null
)

class HomeViewModel : ViewModel() {
    // TODO: Read from DB
    var uiState by mutableStateOf(
        HomeUiState(
            notices = listOf(
                NoticeUi(
                    id = "n1",
                    title = "Dinner on Friday?",
                    details = "Let's have dinner together. Who's in?",
                    acceptedBy = listOf(MemberUi("Marta"), MemberUi("Fran"))
                ),
                NoticeUi(
                    id = "n2",
                    title = "Cleaning schedule",
                    details = "Remember to follow the schedule this week!",
                    acceptedBy = listOf(MemberUi("Lorena"))
                )
            ),
            upcomingEvents = listOf(
                UpcomingEventUi(
                    id = "e1",
                    dateLabel = "Fri 20:00",
                    description = "Dinner with roommates",
                    isRemindMeEnabled = false,
                    location = "Home",
                    attending = listOf(MemberUi("Marta"), MemberUi("Fran"), MemberUi("Lorena"))
                ),
                UpcomingEventUi(
                    id = "e2",
                    dateLabel = "Sat 11:00",
                    description = "Grocery shopping",
                    isRemindMeEnabled = false,
                    location = "Supermarket",
                    attending = listOf(MemberUi("Marta"), MemberUi("Fran"))
                )
            )
        )
    )
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

    fun onActionConsumed() {
        action = null
    }

    fun onAcceptNotice(noticeId: String) {
        uiState = uiState.copy(
            notices = uiState.notices.map { n ->
                if (n.id == noticeId) {
                    val already = n.acceptedBy.any { it.name == "ME" }
                    if (already) n else n.copy(acceptedBy = n.acceptedBy + MemberUi("ME"))
                } else n
            }
        )
    }

    fun onRejectNotice(noticeId: String) {
        uiState = uiState.copy(
            notices = uiState.notices.map { n ->
                if (n.id == noticeId) {
                    n.copy(acceptedBy = n.acceptedBy.filterNot { it.name == "ME" })
                } else n
            }
        )
    }

    /**
     * Toggles the visibility of all notices in the News Board.
     */
    fun onShowAllNotices() {
        uiState = uiState.copy(showAllNotices = !uiState.showAllNotices)
    }

    /**
     * Prepares data for calendar insertion based on the selected event.
     */
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

    /**
     * Handles the expansion/collapse state of an event row.
     */
    fun onViewMoreDetails(eventId: String) {
        uiState = uiState.copy(
            expandedEventId = if (uiState.expandedEventId == eventId) null else eventId
        )
    }

    /**
     * Toggles reminders and emits actions to schedule or cancel system alarms.
     */
    fun onToggleRemindMe(eventId: String) {
        uiState = uiState.copy(
            upcomingEvents = uiState.upcomingEvents.map { e ->
                if (e.id == eventId) e.copy(isRemindMeEnabled = !e.isRemindMeEnabled) else e
            }
        )

        val updated = uiState.upcomingEvents.firstOrNull { it.id == eventId } ?: return
        val requestCode = eventId.hashCode()

        if (updated.isRemindMeEnabled) {
            val eventStart = computeNextOccurrenceMillis(updated.dateLabel, durationMinutes = 60)
            val triggerAtMillis = eventStart - 24L * 60L * 60L * 1000L

            action = HomeAction.ScheduleReminder(
                requestCode = requestCode,
                title = "ChillMates reminder",
                message = "${updated.description} • ${updated.dateLabel}",
                triggerAtMillis = triggerAtMillis
            )
        } else {
            action = HomeAction.CancelReminder(requestCode = requestCode)
        }
    }

    /**
     * Creates a new event, sorts the list by date, and adds a notice to the News Board.
     */
    fun onAddEvent(dateLabel: String, description: String, location: String) {
        val cleanDateLabel = dateLabel.trim()
        val cleanDescription = description.trim()
        val cleanLocation = location.trim()

        if (cleanDateLabel.isBlank() || cleanDescription.isBlank()) return

        val newEvent = UpcomingEventUi(
            id = "e_${System.currentTimeMillis()}",
            dateLabel = cleanDateLabel,
            description = cleanDescription,
            isRemindMeEnabled = false,
            location = cleanLocation,
            attending = emptyList()
        )

        val sortedEvents = (uiState.upcomingEvents + newEvent)
            .sortedBy { e -> computeNextOccurrenceMillis(e.dateLabel, durationMinutes = 60) }

        val newNotice = NoticeUi(
            id = "n_${System.currentTimeMillis()}",
            title = "New event added", // Internal notification title
            details = "${newEvent.description} • ${newEvent.dateLabel}",
            acceptedBy = emptyList()
        )

        uiState = uiState.copy(
            upcomingEvents = sortedEvents,
            notices = listOf(newNotice) + uiState.notices
        )
    }

    /**
     * Parses custom date labels to find the next chronological occurrence.
     */
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