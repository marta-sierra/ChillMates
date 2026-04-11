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
    val latitude: Double,
    val longitude: Double,
    val address: String
)

data class ShoppingUiState(
    val items: List<ShoppingItemUi> = emptyList(),
    val selectedSupermarket: String? = null,
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
        loadSupermarkets()
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

                        uiState = uiState.copy(
                            items = flat.shoppingItems.map { it.toUi() },
                            availableFlatmates = memberNames
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadSupermarkets() {
        viewModelScope.launch {
            val result = repository.getSupermarkets()
            if (result.isSuccess) {
                val supermarkets = result.getOrNull().orEmpty().map { it.toUi() }
                uiState = uiState.copy(availableSupermarkets = supermarkets)
            }
        }
    }

    fun onFilterSupermarket(name: String?) {
        uiState = uiState.copy(selectedSupermarket = name)
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
            latitude = latitude,
            longitude = longitude,
            address = address
        )
}