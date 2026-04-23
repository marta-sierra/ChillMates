package es.uc3m.android.chillmates.model

// Subcollection name inside a Flat document
const val NOTICES_SUBCOLLECTION = "notices"

data class Notice(
    val id: String = "",
    val title: String = "",
    val details: String = "",
    val acceptedBy: List<String> = emptyList()
)