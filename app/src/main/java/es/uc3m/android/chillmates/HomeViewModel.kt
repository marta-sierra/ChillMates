package es.uc3m.android.chillmates

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

// person
@Immutable
data class MemberUi(
    val id: String,
    val name: String
)

// new (in "News Board")
@Immutable
data class NoticeUi(
    val id: String,
    val title: String,
    val details: String,
    val acceptedBy: List<MemberUi>
)

// event (in "Upcoming Events")
@Immutable
data class UpcomingEventUi(
    val id: String,
    val dateLabel: String,
    val description: String,
    val isRemindMeEnabled: Boolean // "Remind me"/"Stop Notifying"
)

// Home screen
@Immutable
data class HomeUiState(
    val notices: List<NoticeUi> = emptyList(),
    val upcomingEvents: List<UpcomingEventUi> = emptyList()
)

class HomeViewModel : ViewModel() {

    // Screen state consumed by the Home UI
    var uiState by mutableStateOf(
        HomeUiState(
            // news Board
            notices = listOf(
                NoticeUi(
                    id = "n1",
                    title = "TACO TUESDAY",
                    details = "Tonight at 9 PM",
                    acceptedBy = listOf(
                        MemberUi("m1", "Marta"),
                        MemberUi("m2", "Lorena"),
                        MemberUi("m3", "Me")
                    )
                ),
                NoticeUi(
                    id = "n2",
                    title = "Sofa delivery at 4 PM",
                    details = "Please be home to receive it",
                    acceptedBy = listOf(
                        MemberUi("m1", "Marta"),
                        MemberUi("m3", "Me")
                    )
                )
            ),

            upcomingEvents = listOf(
                UpcomingEventUi("e1", "February 2nd", "Dinner with friends", isRemindMeEnabled = false),
                UpcomingEventUi("e2", "February 5th", "Reparation of the bathroom door", isRemindMeEnabled = true),
                UpcomingEventUi("e3", "February 12th", "Flat Meeting", isRemindMeEnabled = false),
                UpcomingEventUi("e4", "March 1st", "Expenses Settlement Day", isRemindMeEnabled = false)
            )
        )
    )
        private set

    // Actions that represent user interactions on the Home screen
    fun onAcceptNotice(noticeId: String) {
        // “Count with me!” button
    }

    fun onRejectNotice(noticeId: String) {
        // “Busy, sorry :(” button
    }
    fun onShowAllNotices() {
        // “Show all” news
    }

    fun onAddToCalendar(eventId: String) {
        // Add the event to personal calendar
    }
    fun onViewMoreDetails(eventId: String) {
        // “View more details” dropdown option
    }

    fun onToggleRemindMe(eventId: String) {
        // “Remind me”/“Stop notifying” dropdown option
        uiState = uiState.copy(
            upcomingEvents = uiState.upcomingEvents.map { event ->
                // Change status only of the clicked event
                if (event.id == eventId) event.copy(isRemindMeEnabled = !event.isRemindMeEnabled) else event
            }
        )
    }
}