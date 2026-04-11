package es.uc3m.android.chillmates.repairs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.chillmates.model.Repair
import es.uc3m.android.chillmates.model.USERS_COLLECTION
import es.uc3m.android.chillmates.repository.FlatRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class RepairUi(
    val id: String,
    val title: String,
    val description: String,
    val reportedBy: String,
    val landlordMessage: String,
    val photoUri: String?,
    val status: String
)

data class RepairUiState(
    val repairs: List<RepairUi> = emptyList(),
    val currentUserName: String = "Me"
)

class RepairViewModel(
    private val repository: FlatRepository = FlatRepository()
) : ViewModel() {

    var uiState by mutableStateOf(RepairUiState())
        private set

    private var currentFlatId: String? = null

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    init {
        loadUserAndFlatData()
    }

    private fun loadUserAndFlatData() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch

            try {
                val userDoc = db.collection(USERS_COLLECTION).document(userId).get().await()
                val flatId = userDoc.getString("flatId") ?: return@launch
                val userName = userDoc.getString("displayName") ?: "Me"

                currentFlatId = flatId
                uiState = uiState.copy(currentUserName = userName)

                repository.observeFlat(flatId).collect { flat ->
                    if (flat != null) {
                        uiState = uiState.copy(
                            repairs = flat.repairs.map {
                                RepairUi(
                                    id = it.id,
                                    title = it.title,
                                    description = it.description,
                                    reportedBy = it.reportedBy,
                                    landlordMessage = it.landlordMessage,
                                    photoUri = it.photoUri,
                                    status = it.status
                                )
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun generateReviewedMessage(rawDescription: String): String {
        val cleaned = rawDescription.trim()
        if (cleaned.isBlank()) return ""
        return """
            Hello,

            I am ${uiState.currentUserName} from the flat.

            We would like to report the following issue:
            $cleaned

            Could you please let us know how to proceed?

            Best regards,
            ${uiState.currentUserName}
        """.trimIndent()
    }

    fun createAndMarkSent(
        rawDescription: String,
        finalMessage: String,
        photoUri: String?
    ): RepairUi? {
        val flatId = currentFlatId ?: return null

        val description = rawDescription.trim()
        if (description.isBlank()) return null

        val title = description
            .lineSequence()
            .firstOrNull()
            ?.take(40)
            ?.ifBlank { "Repair request" }
            ?: "Repair request"

        val newRepair = Repair(
            id = System.currentTimeMillis().toString(),
            title = title,
            description = description,
            reportedBy = uiState.currentUserName,
            landlordMessage = finalMessage.trim().ifBlank { description },
            photoUri = photoUri,
            status = "SENT"
        )

        val updatedRepairs = uiState.repairs.map { it.toDb() } + newRepair
        uiState = uiState.copy(repairs = updatedRepairs.map { it.toUi() })

        viewModelScope.launch {
            repository.updateRepairs(flatId, updatedRepairs)
        }

        return newRepair.toUi()
    }

    fun updateRepairStatus(repairId: String, newStatus: String) {
        val flatId = currentFlatId ?: return

        val updatedRepairs = uiState.repairs.map {
            if (it.id == repairId) it.copy(status = newStatus) else it
        }

        uiState = uiState.copy(repairs = updatedRepairs)

        viewModelScope.launch {
            repository.updateRepairs(flatId, updatedRepairs.map { it.toDb() })
        }
    }

    private fun RepairUi.toDb(): Repair =
        Repair(
            id = id,
            title = title,
            description = description,
            reportedBy = reportedBy,
            landlordMessage = landlordMessage,
            photoUri = photoUri,
            status = status
        )

    private fun Repair.toUi(): RepairUi =
        RepairUi(
            id = id,
            title = title,
            description = description,
            reportedBy = reportedBy,
            landlordMessage = landlordMessage,
            photoUri = photoUri,
            status = status
        )
}