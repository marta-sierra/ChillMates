package es.uc3m.android.chillmates.model

import com.google.firebase.firestore.Exclude
import com.google.android.gms.maps.model.LatLng

const val FLATS_COLLECTION = "flats"

data class Chore(
    val id: String = "",
    val title: String = "",
    val assignee: String = "",
    val isDone: Boolean = false
)

data class Expense(
    val id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val paidBy: String = "",
    val ticketUri: String? = null,
    val splitBetween: List<String> = emptyList()
)

data class ShoppingItem(
    val id: String = "",
    val name: String = "",
    val quantity: String = "",
    val neededBy: List<String> = emptyList(),
    val supermarkets: List<String> = emptyList(),
    val bought: Boolean = false
)

data class Supermarket(
    @get:Exclude var id: String? = null,
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = ""
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
    override fun toString(): String = name
}

data class Notice(
    val id: String = "",
    val title: String = "",
    val details: String = "",
    val acceptedBy: List<String> = emptyList()
)

data class UpcomingEvent(
    val id: String = "",
    val dateLabel: String = "",
    val description: String = "",
    val location: String = "",
    val attending: List<String> = emptyList()
)

data class Repair(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val reportedBy: String = "",
    val landlordMessage: String = "",
    val photoUri: String? = null,
    val status: String = "PENDING"
)

data class Flat(
    var id: String? = null,
    val name: String = "",
    val inviteCode: String = "",
    val memberIds: List<String> = emptyList(),
    val chores: List<Chore> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val shoppingItems: List<ShoppingItem> = emptyList(),
    val repairs: List<Repair> = emptyList(),
    val notices: List<Notice> = emptyList(),
    val events: List<UpcomingEvent> = emptyList(),
    var lastRotationTimestamp: Long = 0L
)

