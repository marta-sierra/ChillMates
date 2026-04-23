package es.uc3m.android.chillmates.model

// Subcollection name inside a Flat document
const val REPAIRS_SUBCOLLECTION = "repairs"

data class Repair(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val reportedBy: String = "",
    val landlordMessage: String = "",
    val photoUris: List<String> = emptyList(),
    val status: String = "PENDING"
)
