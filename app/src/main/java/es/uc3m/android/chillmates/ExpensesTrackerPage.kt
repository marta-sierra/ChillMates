package es.uc3m.android.chillmates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlin.math.absoluteValue

@Composable
fun ExpensesTrackerPage(navController: NavController) {
    val vm: ExpensesViewModel = viewModel()

    // events
    ExpensesRoute(
        state = vm.uiState,
        onBackClick = { navController.popBackStack() },
        onAddExpenseClick = { vm.onAddExpense() },
        onTicketClick = { expenseId -> vm.onTicketRequested(expenseId) }
    )
}

@Composable
private fun ExpensesRoute(
    state: ExpensesUiState,
    onBackClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onTicketClick: (String) -> Unit
) {
    // Screen layout: header + content (list and balances)
    Column(modifier = Modifier.fillMaxSize()) {
        ExpensesHeaderSection(
            title = state.title,
            onBackClick = onBackClick
        )

        ExpensesContentSection(
            expenses = state.expenses,
            balances = state.balances,
            // actions
            onAddExpenseClick = onAddExpenseClick,
            onTicketClick = onTicketClick
        )
    }
}

@Composable
private fun ExpensesHeaderSection(
    title: String,
    onBackClick: () -> Unit
) {
    // Header area under the app-level TopBar: back arrow + "Expenses" title
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 14.dp)
        ) {
            // back arrow
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ExpensesContentSection(
    expenses: List<ExpenseUi>,
    balances: List<BalanceUi>,
    onAddExpenseClick: () -> Unit,
    onTicketClick: (String) -> Unit
) {
    // Main content area: scrollable expenses list + add button + balances summary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(expenses, key = { it.id }) { expense ->
                ExpenseRowItem(
                    expense = expense,
                    onTicketClick = { onTicketClick(expense.id) }
                )
                DividerThin()
            }

            // "Add expense" button
            item {
                Spacer(modifier = Modifier.height(6.dp))
                AddExpenseAction(
                    onAddClick = onAddExpenseClick
                )
            }
        }

        BalancesFooterSection(
            balances = balances,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun ExpenseRowItem(
    expense: ExpenseUi,
    onTicketClick: () -> Unit
) {
    // Expense list row: item name/ticket on the left, amount/payer on the right
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
            )

            Spacer(modifier = Modifier.height(2.dp))

            // "see ticket"/"Add ticket"
            Text(
                text = expense.ticketLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                modifier = Modifier
                    .clickable(onClick = onTicketClick)
                    .padding(vertical = 2.dp)
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatMoney(expense.amount),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = expense.paidBy,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun AddExpenseAction(onAddClick: () -> Unit) {
    // Add expense action: full-width button centered under the list
    OutlinedButton(
        onClick = onAddClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = "Add expense")
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "Add expense")
    }
}

@Composable
private fun BalancesFooterSection(
    balances: List<BalanceUi>,
    modifier: Modifier = Modifier
) {
    // Footer area: per-user balance summary cards
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        balances.forEach { b ->
            BalanceCardItem(
                name = b.name,
                balance = b.balance,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BalanceCardItem(
    name: String,
    balance: Double,
    modifier: Modifier = Modifier
) {
    // Balance card: user name + signed amount (positive/negative)
    val isPositive = balance >= 0.0
    val amountText =
        (if (isPositive) "+" else "-") +
                formatMoney(balance.absoluteValue).replace("€", "") +
                "€"

    ElevatedCard(
        modifier = modifier.height(70.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Start
            )

            Text(
                text = amountText,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                )
            )
        }
    }
}

@Composable
private fun DividerThin() {
    // List separator between expense items
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
}

private fun formatMoney(amount: Double): String =
    "€" + String.format("%.2f", amount)