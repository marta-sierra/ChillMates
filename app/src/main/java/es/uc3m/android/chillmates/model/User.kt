package es.uc3m.android.chillmates.model

data class User(
    var id: String? = null,
    val email: String = "",
    val displayName: String = "",
    val flatId: String? = null,
    val flatName: String? = null,
    val avatarUrl: String? = null,
    val joinedDate: Long = System.currentTimeMillis(),
    val notificationEnabled: Boolean = true
) {
    fun hasFlat(): Boolean = !flatId.isNullOrEmpty()
}

const val USERS_COLLECTION = "users"
