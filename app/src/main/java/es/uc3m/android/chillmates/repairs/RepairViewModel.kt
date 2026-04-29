package es.uc3m.android.chillmates.repairs

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import es.uc3m.android.chillmates.model.Repair
import es.uc3m.android.chillmates.model.USERS_COLLECTION
import es.uc3m.android.chillmates.repository.FlatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import android.util.Log

data class RepairUi(
    val id: String,
    val title: String,
    val description: String,
    val reportedBy: String,
    val landlordMessage: String,
    val photoUris: List<String>,
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

    var isAiReviewing by mutableStateOf(false)
        private set

    private var currentFlatId: String? = null

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance() // Instancia de Firebase Storage

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

                launch {
                    repository.observeRepairs(flatId).collect { repairsList ->
                        uiState = uiState.copy(
                            repairs = repairsList.map { it.toUi() }
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reviewMessageWithAI(rawDescription: String, onResult: (String) -> Unit) {
        val cleaned = rawDescription.trim()
        if (cleaned.isBlank()) return

        viewModelScope.launch {
            isAiReviewing = true
            try {
                val generativeModel = GenerativeModel(
                    modelName = "gemini-2.5-flash",
                    apiKey = "AQ.Ab8RN6IT4ieMw3oYtlTZEt0wWnSdQBDptqAl0KpDlvH7uqfikA"
                )

                val prompt = """
                    You are a polite and professional assistant helping a tenant write a maintenance request to their landlord.
                    The tenant's name is ${uiState.currentUserName}.
                    
                    Please rewrite the following issue description into a clear, formal, and polite email body.
                    Fix any spelling or grammar mistakes.
                    Do NOT add any conversational filler, just return the finalized email text.
                    
                    Here is the issue description:
                    $cleaned
                """.trimIndent()

                var responseText: String? = null
                var success = false
                var retries = 0

                // 🚀 SISTEMA DE REINTENTOS: Intentará conectar hasta 3 veces si Google está saturado
                while (!success && retries < 3) {
                    try {
                        val response = withContext(Dispatchers.IO) {
                            generativeModel.generateContent(prompt)
                        }
                        responseText = response.text
                        success = true
                    } catch (e: Exception) {
                        retries++
                        if (retries >= 3) {
                            throw e // Si falla 3 veces, nos rendimos y vamos al Catch principal
                        }
                        // Esperamos 1.5 segundos antes del siguiente intento
                        kotlinx.coroutines.delay(1500)
                    }
                }

                responseText?.let { improvedText ->
                    onResult(improvedText.trim())
                }
            } catch (e: Exception) {
                // Si incluso después de 3 reintentos falla, usamos el texto de emergencia
                android.util.Log.e("GeminiAI", "Falló tras 3 intentos: ${e.message}")
                val fallbackText = """
                    Hello,

                    I am ${uiState.currentUserName} from the flat.

                    We would like to report the following maintenance issue:
                    - ${cleaned.replaceFirstChar { it.uppercase() }}

                    Could you please let us know how to proceed to get this resolved?

                    Best regards,
                    ${uiState.currentUserName}
                """.trimIndent()
                onResult(fallbackText)
            } finally {
                isAiReviewing = false
            }
        }
    }

    // 🚀 MODIFICADO: Sube las fotos a Firebase Storage primero
    fun createSentRepair(title: String, rawDescription: String, finalMessage: String, photoUris: List<String>) {
        val flatId = currentFlatId ?: return
        val description = rawDescription.trim()
        if (description.isBlank()) return

        val repairTitle = title.trim().ifBlank { "Repair request" }

        viewModelScope.launch {
            val uploadedUrls = mutableListOf<String>()

            // 1. Subir cada imagen a Firebase Storage
            for (localUriStr in photoUris) {
                try {
                    val uri = Uri.parse(localUriStr)
                    val fileName = UUID.randomUUID().toString() + ".jpg"
                    val ref = storage.reference.child("flats/$flatId/repairs/$fileName")

                    // Sube el archivo
                    ref.putFile(uri).await()

                    // Obtiene la URL pública HTTP
                    val downloadUrl = ref.downloadUrl.await().toString()
                    uploadedUrls.add(downloadUrl)
                } catch (e: Exception) {
                    e.printStackTrace() // Si falla una foto, simplemente la saltamos
                }
            }

            // 2. Crear el objeto con las URLs públicas
            val newRepair = Repair(
                id = System.currentTimeMillis().toString(),
                title = repairTitle,
                description = description,
                reportedBy = uiState.currentUserName,
                landlordMessage = finalMessage.trim().ifBlank { description },
                photoUris = uploadedUrls, // Usamos las URLs de Firebase!
                status = "SENT"
            )

            repository.addRepair(flatId, newRepair)
        }
    }

    fun updateRepairStatus(repairId: String, newStatus: String) {
        val flatId = currentFlatId ?: return
        viewModelScope.launch {
            repository.updateRepairStatus(flatId, repairId, newStatus)
        }
    }

    private fun Repair.toUi(): RepairUi =
        RepairUi(id = id, title = title, description = description, reportedBy = reportedBy, landlordMessage = landlordMessage, photoUris = photoUris, status = status)
}