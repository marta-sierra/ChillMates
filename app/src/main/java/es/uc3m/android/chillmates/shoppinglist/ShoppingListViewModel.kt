package es.uc3m.android.chillmates.shoppinglist

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.Immutable

/**
 * Representation of an item of the shopping list.
 */
@Immutable
data class ShoppingItemUi(
    val id: String,
    val name: String,
    val quantity: String,
    val neededBy: List<String>, // empty = All
    val supermarkets: List<String>, // empty = Any
    val isBought: Boolean = false
)

/**
 * State object containing the list of items, available filters, and participants.
 */
data class ShoppingUiState(
    val items: List<ShoppingItemUi> = emptyList(),
    val selectedSupermarket: String? = null,
    val availableSupermarkets: List<String> = emptyList(),
    // TODO: Read data from DB.
    val availableFlatmates: List<String> = listOf("MARTA", "FRAN", "LORENA", "ALEX", "ME")
)

/**
 * ViewModel responsible for managing the state of the Shopping List.
 * Handles filtering, deleting, adding, and marking items as bought.
 */
class ShoppingViewModel : ViewModel() {
    var uiState by mutableStateOf(
        ShoppingUiState(
            // TODO: Read data from DB.
            availableSupermarkets = listOf("Lidl", "Mercadona", "Carrefour"),
            items = listOf(
                ShoppingItemUi(
                    id = "1",
                    name = "Milk",
                    quantity = "2L",
                    neededBy = listOf("MARTA"),
                    supermarkets = listOf("Lidl"),
                    isBought = false
                ),
                ShoppingItemUi(
                    id = "2",
                    name = "Eggs",
                    quantity = "12",
                    neededBy = emptyList(),
                    supermarkets = emptyList(),
                    isBought = true
                )
            )
        )
    )
        private set

    //Update the current supermarket filter.
    fun onFilterSupermarket(name: String?) {
        uiState = uiState.copy(selectedSupermarket = name)
    }

    //Removes an item from the list by its unique ID.
    fun onDeleteItem(id: String) {
        uiState = uiState.copy(
            items = uiState.items.filterNot { it.id == id }
        )
    }

    // Toggles the 'bought' status of a specific item.
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

    //Appends a new item to the current shopping list.
    fun addItem(item: ShoppingItemUi) {
        uiState = uiState.copy(
            items = uiState.items + item
        )
    }
}