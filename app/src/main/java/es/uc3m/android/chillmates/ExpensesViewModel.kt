package es.uc3m.android.chillmates

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// expense item of the list
@Immutable
data class ExpenseUi(
    val id: String,
    val title: String,
    val ticketLabel: String, // "see ticket"/"add ticket"
    val amount: Double,
    val paidBy: String
)

// balances of expenses
@Immutable
data class BalanceUi(
    val name: String,
    val balance: Double
)

// entire screen
@Immutable
data class ExpensesUiState(
    val title: String = "Expenses",
    val expenses: List<ExpenseUi> = emptyList(),
    val balances: List<BalanceUi> = emptyList()
)

class ExpensesViewModel : ViewModel() {

    // UI state exposed to Compose (read-only from the UI layer)
    var uiState by mutableStateOf(
        ExpensesUiState(
            expenses = listOf(
                ExpenseUi("1", "Pan", "add ticket", 29.99, "MARTA"),
                ExpenseUi("2", "Cutlery", "see ticket", 45.63, "MARTA"),
                ExpenseUi("3", "Groceries", "see ticket", 35.95, "FRAN"),
                ExpenseUi("4", "Utilities", "see bill", 9.95, "LORENA"),
            ),
            balances = listOf(
                BalanceUi("MARTA", 36.0),
                BalanceUi("ME", -15.0),
                BalanceUi("LORENA", -21.0),
            )
        )
    )
        private set // only viewModel can change the state

    // UI actions
    fun onAddExpense() {
        // Business logic goes here
    }

    fun onTicketRequested(expenseId: String) {
        // Business logic goes here
    }
}