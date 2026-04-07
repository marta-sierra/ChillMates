package es.uc3m.android.chillmates.shoppinglist

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.chillmates.model.Supermarket
import es.uc3m.android.chillmates.model.USERS_COLLECTION
import es.uc3m.android.chillmates.repository.SupermarketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Immutable
data class ShoppingItemUi(
    val id: String,
    val name: String,
    val quantity: String,
    val neededBy: List<String>,
    val supermarkets: List<String>,
    val isBought: Boolean = false
)

data class ShoppingUiState(
    val items: List<ShoppingItemUi> = emptyList(),
    val selectedSupermarkets: Set<String> = emptySet(),
    val availableSupermarkets: List<Supermarket> = emptyList(),
    val availableFlatmates: List<String> = emptyList(),
    val flatId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ShoppingViewModel(application: Application) : AndroidViewModel(application) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val supermarketRepository = SupermarketRepository()

    private val _uiState = MutableStateFlow(ShoppingUiState())
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    init {
        loadFlatData()
    }

    private fun loadFlatData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@launch
                }

                // Obtenemos el flatId del usuario
                val userDoc = firestore.collection(USERS_COLLECTION).document(userId).get().await()
                val flatId = userDoc.getString("flatId")

                if (flatId != null) {
                    _uiState.value = _uiState.value.copy(flatId = flatId)
                    observeSupermarkets(flatId)
                    loadFlatMembers(flatId)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "No flat associated with user")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun observeSupermarkets(flatId: String) {
        viewModelScope.launch {
            // Escuchamos cambios en tiempo real desde el repositorio
            supermarketRepository.observeSupermarkets(flatId).collect { supermarkets ->
                _uiState.value = _uiState.value.copy(
                    availableSupermarkets = supermarkets,
                    isLoading = false
                )
            }
        }
    }

    private fun loadFlatMembers(flatId: String) {
        viewModelScope.launch {
            try {
                val flatDoc = firestore.collection("flats").document(flatId).get().await()
                val memberIds = flatDoc.get("memberIds") as? List<String> ?: emptyList()

                // Resolvemos los nombres de los compañeros de piso
                val memberNames = memberIds.mapNotNull { userId ->
                    try {
                        val userDoc = firestore.collection(USERS_COLLECTION).document(userId).get().await()
                        userDoc.getString("displayName") ?: "User"
                    } catch (e: Exception) {
                        null
                    }
                }

                _uiState.value = _uiState.value.copy(availableFlatmates = memberNames)
            } catch (e: Exception) {
                // Silently fail for member loading
            }
        }
    }

    // --- Acciones de UI ---

    fun onFilterSupermarkets(selected: Set<String>) {
        _uiState.value = _uiState.value.copy(selectedSupermarkets = selected)
    }

    fun onToggleSupermarketSelection(supermarketId: String) {
        val currentSelection = _uiState.value.selectedSupermarkets
        val newSelection = if (supermarketId in currentSelection) {
            currentSelection - supermarketId
        } else {
            currentSelection + supermarketId
        }
        _uiState.value = _uiState.value.copy(selectedSupermarkets = newSelection)
    }

    fun clearSupermarketSelection() {
        _uiState.value = _uiState.value.copy(selectedSupermarkets = emptySet())
    }

    fun onDeleteItem(id: String) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.filterNot { it.id == id }
        )
    }

    fun onToggleBought(itemId: String) {
        val updatedItems = _uiState.value.items.map { item ->
            if (item.id == itemId) {
                item.copy(isBought = !item.isBought)
            } else {
                item
            }
        }
        _uiState.value = _uiState.value.copy(items = updatedItems)
    }

    fun addItem(item: ShoppingItemUi) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items + item
        )
    }

    fun addSupermarket(supermarket: Supermarket, onResult: (Result<Supermarket>) -> Unit) {
        val flatId = _uiState.value.flatId ?: return
        viewModelScope.launch {
            val result = supermarketRepository.addSupermarket(flatId, supermarket)
            onResult(result)
        }
    }

    fun deleteSupermarket(id: String, onResult: (Result<Unit>) -> Unit) {
        val flatId = _uiState.value.flatId ?: return
        viewModelScope.launch {
            val result = supermarketRepository.deleteSupermarket(flatId, id)
            onResult(result)
        }
    }
}