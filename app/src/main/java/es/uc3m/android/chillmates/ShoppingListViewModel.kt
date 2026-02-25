package es.uc3m.android.chillmates

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.Immutable

@Immutable
data class ShoppingItemUi(
    val id: String,
    val name: String,
    val quantity: String,
    val neededBy: List<String>, // empty = All
    val supermarkets: List<String>, // empty = Any
    val isBought: Boolean = false
)

data class ShoppingUiState(
    val items: List<ShoppingItemUi> = emptyList(),
    val selectedSupermarket: String? = null,
    val availableSupermarkets: List<String> = emptyList()
)

class ShoppingViewModel : ViewModel() {
    var uiState by mutableStateOf(
        ShoppingUiState(
            availableSupermarkets = listOf("Lidl", "Mercadona", "Carrefour"),
            items = listOf(
                ShoppingItemUi(
                    id = "1",
                    name = "Milk",
                    quantity = "2L",
                    neededBy = listOf("Marta"),
                    supermarkets = listOf("Lidl"),
                    isBought = false
                ),
                ShoppingItemUi(
                    id = "2",
                    name = "Eggs",
                    quantity = "12",
                    neededBy = emptyList(), // All
                    supermarkets = emptyList(), // Any
                    isBought = true
                )
            )
        )
    )
        private set


    fun onFilterSupermarket(name: String?) {
        uiState = uiState.copy(selectedSupermarket = name)
    }

    fun onDeleteItem(id: String) {
        uiState = uiState.copy(
            items = uiState.items.filterNot { it.id == id }
        )
    }

    fun onToggleBought(itemId: String) {
        val updatedItems = uiState.items.map { item ->
            if (item.id == itemId) {
                item.copy(isBought = !item.isBought)
            } else {
                item
            }
        }
        uiState = uiState.copy(items = updatedItems)
    }

    fun addItem(item: ShoppingItemUi) {
        uiState = uiState.copy(
            items = uiState.items + item
        )
    }
}