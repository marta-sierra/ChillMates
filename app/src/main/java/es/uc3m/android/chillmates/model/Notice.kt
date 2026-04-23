package es.uc3m.android.chillmates.model

// Subcollection name inside a Flat document
const val NOTICES_SUBCOLLECTION = "notices"

data class Notice(
    val id: String = "",
    val title: String = "",
    val details: String = "",
    val createdBy: String = "", // O las variables extra que tú tuvieras...
    val acceptedBy: List<String> = emptyList(), // <-- FUNDAMENTAL PARA LA LISTA DE NOMBRES
    val isPoll: Boolean = false                 // <-- FUNDAMENTAL PARA MOSTRAR LOS BOTONES
)