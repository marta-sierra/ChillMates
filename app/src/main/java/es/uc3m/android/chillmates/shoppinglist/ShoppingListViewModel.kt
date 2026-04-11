package es.uc3m.android.chillmates.shoppinglist

import androidx.compose.runtime.*
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.chillmates.model.Expense
import es.uc3m.android.chillmates.model.ShoppingItem
import es.uc3m.android.chillmates.model.Supermarket
import es.uc3m.android.chillmates.model.USERS_COLLECTION
import es.uc3m.android.chillmates.repository.FlatRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Immutable
data class ShoppingItemUi(
    val id: String,
    val name: String,
    val quantity: String,
    val neededBy: List<String>,
    val supermarkets: List<String>,
    val bought: Boolean = false
)

@Immutable
data class SupermarketUi(
    val id: String,
    val name: String,
    val officialName: String,
    val placeId: String,
    val latitude: Double,
    val longitude: Double,
    val address: String
)

data class ShoppingUiState(
    val items: List<ShoppingItemUi> = emptyList(),
    val selectedSupermarket: String? = null,
    val selectedSupermarkets: Set<String> = emptySet(),
    val availableSupermarkets: List<SupermarketUi> = emptyList(),
    val availableFlatmates: List<String> = emptyList(),
    val currentUserName: String = "Me"
)

class ShoppingViewModel(
    private val repository: FlatRepository = FlatRepository()
) : ViewModel() {

    var uiState by mutableStateOf(ShoppingUiState())
        private set

    private var currentFlatId: String? = null
    private var currentExpenses: List<Expense> = emptyList()

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
                        currentExpenses = flat.expenses

                        val membersResult = repository.getMemberDetails(flat.memberIds)
                        val memberNames = if (membersResult.isSuccess) {
                            membersResult.getOrNull()?.map { it.second } ?: emptyList()
                        } else {
                            emptyList()
                        }

                        val supermarketsResult = repository.getSupermarketsForFlat(flatId)
                        val supermarkets = if (supermarketsResult.isSuccess) {
                            supermarketsResult.getOrNull().orEmpty().map { it.toUi() }
                        } else {
                            emptyList()
                        }
                        val selectedFilter = uiState.selectedSupermarket
                        val selectedFilters = uiState.selectedSupermarkets.ifEmpty {
                            selectedFilter?.let { setOf(it) } ?: emptySet()
                        }
                        val validFilters = selectedFilters.filter { selected ->
                            supermarkets.any { it.name == selected }
                        }.toSet()

                        uiState = uiState.copy(
                            items = flat.shoppingItems.map { it.toUi() },
                            availableFlatmates = memberNames,
                            availableSupermarkets = supermarkets,
                            selectedSupermarket = validFilters.firstOrNull(),
                            selectedSupermarkets = validFilters
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onFilterSupermarket(name: String?) {
        uiState = uiState.copy(
            selectedSupermarket = name,
            selectedSupermarkets = if (name == null) emptySet() else setOf(name)
        )
    }

    fun onFilterSupermarkets(names: Set<String>) {
        uiState = uiState.copy(
            selectedSupermarkets = names,
            selectedSupermarket = names.firstOrNull()
        )
    }

    fun onDeleteItem(id: String) {
        val flatId = currentFlatId ?: return
        val updatedItems = uiState.items.filterNot { it.id == id }

        uiState = uiState.copy(items = updatedItems)

        viewModelScope.launch {
            repository.updateShoppingItems(flatId, updatedItems.map { it.toDb() })
        }
    }

    fun setItemBought(itemId: String, bought: Boolean) {
        val flatId = currentFlatId ?: return

        val updatedItems = uiState.items.map { item ->
            if (item.id == itemId) item.copy(bought = bought) else item
        }

        uiState = uiState.copy(items = updatedItems)

        viewModelScope.launch {
            repository.updateShoppingItems(flatId, updatedItems.map { it.toDb() })
        }
    }

    fun addItem(item: ShoppingItemUi) {
        val flatId = currentFlatId ?: return
        val updatedItems = uiState.items + item

        uiState = uiState.copy(items = updatedItems)

        viewModelScope.launch {
            repository.updateShoppingItems(flatId, updatedItems.map { it.toDb() })
        }
    }

    fun addSupermarket(
        alias: String,
        officialName: String,
        placeId: String,
        address: String,
        latitude: Double,
        longitude: Double,
        onResult: (Result<SupermarketUi>) -> Unit
    ) {
        val flatId = currentFlatId
        if (flatId == null) {
            onResult(Result.failure(Exception("No flat selected")))
            return
        }

        viewModelScope.launch {
            val result = repository.addSupermarketForFlat(
                flatId = flatId,
                supermarket = Supermarket(
                    name = alias,
                    officialName = officialName,
                    placeId = placeId,
                    address = address,
                    latitude = latitude,
                    longitude = longitude
                )
            )

            if (result.isSuccess) {
                val created = result.getOrThrow().toUi()
                uiState = uiState.copy(
                    availableSupermarkets = (uiState.availableSupermarkets + created).distinctBy { it.id }
                )
                onResult(Result.success(created))
            } else {
                onResult(Result.failure(result.exceptionOrNull() ?: Exception("Failed to add supermarket")))
            }
        }
    }

    fun addExpenseFromShopping(
        itemId: String,
        title: String,
        amount: Double,
        paidBy: String,
        splitBetween: List<String>
    ) {
        val flatId = currentFlatId ?: return

        val updatedItems = uiState.items.map { item ->
            if (item.id == itemId) item.copy(bought = true) else item
        }

        val newExpense = Expense(
            id = System.currentTimeMillis().toString(),
            title = title,
            amount = amount,
            paidBy = paidBy,
            splitBetween = splitBetween,
            ticketUri = null
        )

        val updatedExpenses = currentExpenses + newExpense

        uiState = uiState.copy(items = updatedItems)
        currentExpenses = updatedExpenses

        viewModelScope.launch {
            repository.updateShoppingItems(flatId, updatedItems.map { it.toDb() })
            repository.updateExpenses(flatId, updatedExpenses)
        }
    }

    private fun ShoppingItem.toUi(): ShoppingItemUi =
        ShoppingItemUi(
            id = id,
            name = name,
            quantity = quantity,
            neededBy = neededBy,
            supermarkets = supermarkets,
            bought = bought
        )

    private fun ShoppingItemUi.toDb(): ShoppingItem =
        ShoppingItem(
            id = id,
            name = name,
            quantity = quantity,
            neededBy = neededBy,
            supermarkets = supermarkets,
            bought = bought
        )

    private fun Supermarket.toUi(): SupermarketUi =
        SupermarketUi(
            id = id ?: "",
            name = name,
            officialName = officialName,
            placeId = placeId,
            latitude = latitude,
            longitude = longitude,
            address = address
        )
}