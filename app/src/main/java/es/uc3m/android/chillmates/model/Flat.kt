package es.uc3m.android.chillmates.model

data class Flat(
    var id: String? = null,
    val name: String = "",
    val inviteCode: String = "",
    val memberIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

const val FLATS_COLLECTION = "flats"
