package es.uc3m.android.chillmates.model

// Subcollection name inside a Flat document
const val CHORES_SUBCOLLECTION = "chores"

data class Chore(
    val id: String = "",
    val title: String = "",
    val assignee: String = "",
    val isDone: Boolean = false
)