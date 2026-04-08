package es.uc3m.android.chillmates.model

// Constants for Firestore collections
const val FLATS_COLLECTION = "flats"

// Chore model uses String for IDs (best practice for Firebase)
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
// Flat model updated to match FlatRepository requirements
data class Flat(
    var id: String? = null, // Must be 'var' so we can assign the Firestore document ID
    val name: String = "",
    val inviteCode: String = "",
    val memberIds: List<String> = emptyList(),
    val chores: List<Chore> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    var lastRotationTimestamp: Long = 0L
)