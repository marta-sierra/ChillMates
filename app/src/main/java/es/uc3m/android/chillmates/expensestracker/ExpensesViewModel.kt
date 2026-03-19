package es.uc3m.android.chillmates.expensestracker

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * UI model representing an expense entry in the list.
 */
@Immutable
data class ExpenseUi(
    val id: String,
    val title: String,
    val ticketLabel: String,
    val amount: Double,
    val paidBy: String,
    val ticketUri: String? = null,
    val splitBetween: List<String> = emptyList()
)

/**
 * UI model representing a flatmate's calculated debt or credit.
 */
@Immutable
data class BalanceUi(
    val name: String,
    val balance: Double
)

/**
 * State holder for the Expenses screen, managing the list, balances, and shared flatmate data.
 */
@Immutable
data class ExpensesUiState(
    val title: String = "Expenses",
    val expenses: List<ExpenseUi> = emptyList(),
    val balances: List<BalanceUi> = emptyList(),
    // TODO: Read from DB
    val availableFlatmates: List<String> = listOf("MARTA", "FRAN", "LORENA", "ALEX", "ME")
)

/**
 * Sealed class defining actions triggered by ticket interaction.
 */
sealed class TicketAction {
    data object PickTicketImage : TicketAction()
    data class ViewTicket(val uri: Uri) : TicketAction()
}

class ExpensesViewModel : ViewModel() {
    var uiState by mutableStateOf(
        ExpensesUiState(
            // TODO: Read from DB
            expenses = listOf(
                ExpenseUi("1", "Weekly Bread & Milk", "see ticket", 5.50, "MARTA", splitBetween = listOf("MARTA", "FRAN", "LORENA", "ALEX", "ME")),
                ExpenseUi("2", "IKEA Cutlery Set", "add ticket", 45.63, "MARTA", splitBetween = listOf("MARTA", "FRAN", "LORENA", "ALEX", "ME")),
                ExpenseUi("3", "Groceries (Mercadona)", "see ticket", 82.20, "FRAN", splitBetween = listOf("FRAN", "LORENA", "ME")),
                ExpenseUi("4", "Fiber Internet", "add ticket", 29.99, "LORENA", splitBetween = listOf("MARTA", "FRAN", "LORENA", "ALEX", "ME"))
            )
        )
    )
        private set

    init {
        recalculateBalances()
    }

    /**
     * Adds a new expense provided by AddExpenseActivity and updates the financial balances.
     */
    fun onExpenseAdded(title: String, amount: Double, paidBy: String, splitBetween: List<String>) {
        val newId = System.currentTimeMillis().toString() // Use timestamp for unique identification

        val newExpense = ExpenseUi(
            id = newId,
            title = title,
            ticketLabel = "add ticket",
            amount = amount,
            paidBy = paidBy,
            splitBetween = splitBetween
        )

        uiState = uiState.copy(expenses = uiState.expenses + newExpense)
        recalculateBalances()
    }

    /**
     * Decides whether to open the gallery for a new upload or view an existing receipt.
     */
    fun onTicketRequested(expenseId: String): TicketAction {
        val expense = uiState.expenses.firstOrNull { it.id == expenseId }
            ?: return TicketAction.PickTicketImage

        return if (expense.ticketUri.isNullOrBlank()) {
            TicketAction.PickTicketImage
        } else {
            TicketAction.ViewTicket(Uri.parse(expense.ticketUri))
        }
    }

    /**
     * Updates an expense with a newly selected receipt URI and toggles the UI label.
     */
    fun onTicketSelected(expenseId: String, uri: Uri) {
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

    /**
     * Core logic to calculate net balances across all flatmates.
     * Creditors get positive values, debtors get negative values.
     */
    private fun recalculateBalances() {
        val totals = mutableMapOf<String, Double>()

        // Initialize every flatmate with zero
        uiState.availableFlatmates.forEach { totals[it] = 0.0 }

        uiState.expenses.forEach { expense ->
            val payer = expense.paidBy
            val amount = expense.amount
            val participants = expense.splitBetween

            if (participants.isNotEmpty()) {
                val share = amount / participants.size

                // Payer gets credit for the full amount
                totals[payer] = (totals[payer] ?: 0.0) + amount

                // Everyone in the split owes their share
                participants.forEach { person ->
                    totals[person] = (totals[person] ?: 0.0) - share
                }
            }
        }

        // Calculation results go to the UI state, sorted by highest balance
        val newBalances = totals.map { (name, balance) ->
            BalanceUi(name, balance)
        }.sortedByDescending { it.balance }

        uiState = uiState.copy(balances = newBalances)
    }
}