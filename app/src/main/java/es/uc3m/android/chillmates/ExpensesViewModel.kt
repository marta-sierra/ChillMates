// ExpensesViewModel.kt
package es.uc3m.android.chillmates

import android.net.Uri

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
    val paidBy: String,
    val ticketUri: String? = null  // store the selected ticket
)

// balances of expenses
@Immutable
data class BalanceUi(
    val name: String,
    val balance: Double
)

// Expenses screen
@Immutable
data class ExpensesUiState(
    val title: String = "Expenses",
    val expenses: List<ExpenseUi> = emptyList(),
    val balances: List<BalanceUi> = emptyList()
)

// one-shot actions (photo picker vs viewer)
sealed class TicketAction {
    // request the system gallery/photo picker to attach a ticket image
    data object PickTicketImage : TicketAction()

    // open an existing ticket Uri with an external viewer
    data class ViewTicket(val uri: Uri) : TicketAction()
}

class ExpensesViewModel : ViewModel() {

    // UI state exposed to Compose
    var uiState by mutableStateOf(
        ExpensesUiState(
            expenses = listOf(
                ExpenseUi("1", "Pan", "add ticket", 29.99, "MARTA"),
                ExpenseUi("2", "Cutlery", "add ticket", 45.63, "MARTA"),
                ExpenseUi("3", "Groceries", "add ticket", 35.95, "FRAN"),
                ExpenseUi("4", "Utilities", "add ticket", 9.95, "LORENA"),
            ),
            balances = listOf(
                BalanceUi("MARTA", 36.0),
                BalanceUi("ME", -15.0),
                BalanceUi("LORENA", -21.0),
            )
        )
    )
        private set // only viewModel can change the state

    // add a new expense coming from AddExpenseActivity
    fun onExpenseAdded(title: String, amount: Double, paidBy: String) {
        val newId = (uiState.expenses.size + 1).toString()

        uiState = uiState.copy(
            expenses = uiState.expenses + ExpenseUi(
                id = newId,
                title = title,
                ticketLabel = "add ticket",
                amount = amount,
                paidBy = paidBy
            )
        )

        // Here you would recalculate balances properly.
    }

    fun onTicketRequested(expenseId: String): TicketAction {
        // if no ticket pick from gallery, else view it
        val expense = uiState.expenses.firstOrNull { it.id == expenseId }
            ?: return TicketAction.PickTicketImage

        return if (expense.ticketUri.isNullOrBlank()) {
            TicketAction.PickTicketImage
        } else {
            TicketAction.ViewTicket(Uri.parse(expense.ticketUri))
        }
    }

    fun onTicketSelected(expenseId: String, uri: Uri) {
        // persist the selected ticket Uri and update the label to "see ticket"
        uiState = uiState.copy(
            expenses = uiState.expenses.map { e ->
                if (e.id == expenseId) {
                    e.copy(
                        ticketUri = uri.toString(),
                        ticketLabel = "see ticket"
                    )
                } else e
            }
        )
    }
}