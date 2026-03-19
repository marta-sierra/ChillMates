package es.uc3m.android.chillmates.expensestracker

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlin.math.absoluteValue

@Composable
fun ExpensesTrackerPage(navController: NavController) {
    val vm: ExpensesViewModel = viewModel()
    val context = LocalContext.current

    // Expense is waiting for a ticket image upload
    var pendingExpenseId by remember { mutableStateOf<String?>(null) }
    // Which ticket URI is currently being previewed in the dialog
    var ticketToView by remember { mutableStateOf<Uri?>(null) }

    // Launcher for selecting a document/image from the system picker
    val getImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val expenseId = pendingExpenseId
        pendingExpenseId = null

        if (uri != null && expenseId != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }

            vm.onTicketSelected(expenseId, uri)
            Toast.makeText(context, "Ticket attached ✅", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher to handle the result returned from AddExpenseActivity
    val addExpenseLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val title = data?.getStringExtra(AddExpenseActivity.EXTRA_TITLE)
            val amount = data?.getDoubleExtra(AddExpenseActivity.EXTRA_AMOUNT, Double.NaN)
            val paidBy = data?.getStringExtra(AddExpenseActivity.EXTRA_PAID_BY)

            if (!title.isNullOrBlank() && amount != null && !amount.isNaN() && !paidBy.isNullOrBlank()) {
                val splitBetween = data.getStringArrayListExtra(AddExpenseActivity.EXTRA_SPLIT_BETWEEN) ?: emptyList<String>()
                vm.onExpenseAdded(title, amount, paidBy, splitBetween)
                Toast.makeText(context, "Expense added ✅", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Invalid data", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Displays the image preview dialog if a ticket is selected
    if (ticketToView != null) {
        TicketViewerDialog(
            uri = ticketToView!!,
            onDismiss = { ticketToView = null }
        )
    }

    ExpensesRoute(
        state = vm.uiState,
        onBackClick = { navController.popBackStack() },
        onAddExpenseClick = {
            // Prepare intent for AddExpenseActivity with current flatmate list
            val intent = Intent(context, AddExpenseActivity::class.java).apply {
                putStringArrayListExtra("flatmatesList", ArrayList(vm.uiState.availableFlatmates))
            }
            addExpenseLauncher.launch(intent)
        },
        onTicketClick = { expenseId ->
            when (val action = vm.onTicketRequested(expenseId)) {
                is TicketAction.PickTicketImage -> {
                    pendingExpenseId = expenseId
                    getImageLauncher.launch(arrayOf("image/*"))
                }
                is TicketAction.ViewTicket -> {
                    ticketToView = action.uri
                }
            }
        }
    )
}

/**
 * In-app dialog to preview an attached receipt image.
 */
@Composable
private fun TicketViewerDialog(
    uri: Uri,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ticket",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) { Text("Close") }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Legacy ImageView for URI rendering
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            adjustViewBounds = true
                            scaleType = ImageView.ScaleType.FIT_CENTER
                        }
                    },
                    update = { imageView ->
                        imageView.setImageURI(uri)
                    }
                )
            }
        }
    }
}

@Composable
private fun ExpensesRoute(
    state: ExpensesUiState,
    onBackClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onTicketClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ExpensesHeaderSection(
            title = state.title,
            onBackClick = onBackClick
        )

        ExpensesContentSection(
            expenses = state.expenses,
            balances = state.balances,
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
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
            )

            if (expense.splitBetween.isNotEmpty()) {
                Text(
                    text = "For: ${expense.splitBetween.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

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
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
}

private fun formatMoney(amount: Double): String =
    "€" + String.format("%.2f", amount)

/**
 * Opens a ticket URI using an implicit ACTION_VIEW intent.
 */
private fun openTicket(context: Context, uri: Uri) {
    val mimeType = context.contentResolver.getType(uri) ?: "image/*"

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app found to open this file", Toast.LENGTH_LONG).show()
    }
}