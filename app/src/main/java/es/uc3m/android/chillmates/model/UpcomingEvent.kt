package es.uc3m.android.chillmates.model

// Subcollection name inside a Flat document
const val EVENTS_SUBCOLLECTION = "events"

data class UpcomingEvent(
    val id: String = "",
    val dateLabel: String = "",
    val description: String = "",
    val location: String = "",
    val attending: List<String> = emptyList()
)