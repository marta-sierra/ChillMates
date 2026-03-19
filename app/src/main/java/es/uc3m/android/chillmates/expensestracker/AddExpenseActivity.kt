package es.uc3m.android.chillmates.expensestracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.uc3m.android.chillmates.R
import es.uc3m.android.chillmates.ui.theme.ChillMatesTheme

/**
 * Activity for creating or editing an expense.
 * Can be initialized via an intent with data from a Shopping List item to pre-fill fields.
 */
class AddExpenseActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val availableFlatmates = intent.getStringArrayListExtra("flatmatesList") ?: arrayListOf()
        val initialTitle = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val initialPaidBy = intent.getStringExtra(EXTRA_PAID_BY) ?: ""
        val initialSplit = intent.getStringArrayListExtra(EXTRA_SPLIT_BETWEEN) ?: emptyList<String>()

        setContent {
            ChillMatesTheme {
                AddExpenseScreen(
                    availableFlatmates = availableFlatmates,
                    initialTitle = initialTitle,
                    initialPaidBy = initialPaidBy,
                    initialSplit = initialSplit,
                    onBack = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    onSave = { title, amount, paidBy, splitBetween ->
                        val data = Intent().apply {
                            putExtra(EXTRA_TITLE, title)
                            putExtra(EXTRA_AMOUNT, amount)
                            putExtra(EXTRA_PAID_BY, paidBy)
                            putStringArrayListExtra(EXTRA_SPLIT_BETWEEN, ArrayList(splitBetween))
                        }
                        setResult(RESULT_OK, data)
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_TITLE = "extraTitle"
        const val EXTRA_AMOUNT = "extraAmount"
        const val EXTRA_PAID_BY = "extraPaidBy"
        const val EXTRA_SPLIT_BETWEEN = "extraSplitBetween"
    }
}

/**
 * Main form for entering expense details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseScreen(
    availableFlatmates: List<String>,
    initialTitle: String,
    initialPaidBy: String,
    initialSplit: List<String>,
    onBack: () -> Unit,
    onSave: (String, Double, String, List<String>) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var paidBy by rememberSaveable { mutableStateOf(initialPaidBy) }

    // Default to all flatmates if no specific split was provided
    var selectedFlatmates by remember {
        mutableStateOf(if (initialSplit.isEmpty()) availableFlatmates.toSet() else initialSplit.toSet())
    }

    val amountValue: Double? = amountText.replace(",", ".").toDoubleOrNull()
    val canSave = title.isNotBlank() && paidBy.isNotBlank() && (amountValue != null) && selectedFlatmates.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_add_expense)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = stringResource(R.string.label_fill_details),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.label_title)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(stringResource(R.string.label_amount_currency)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = paidBy,
                onValueChange = { paidBy = it },
                label = { Text(stringResource(R.string.label_paid_by)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.label_split_between),
                fontWeight = FontWeight.Bold
            )
            availableFlatmates.forEach { person ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = person in selectedFlatmates,
                        onCheckedChange = { isChecked ->
                            selectedFlatmates = if (isChecked) selectedFlatmates + person else selectedFlatmates - person
                        }
                    )
                    Text(text = person)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val amount = amountValue ?: return@Button
                    onSave(title.trim(), amount, paidBy.trim(), selectedFlatmates.toList())
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_save))
            }
        }
    }
}