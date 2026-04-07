package es.uc3m.android.chillmates.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Flat(
    @DocumentId var id: String? = null,
    val name: String = "",
    val inviteCode: String = "",
    val memberIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

const val FLATS_COLLECTION = "flats"
