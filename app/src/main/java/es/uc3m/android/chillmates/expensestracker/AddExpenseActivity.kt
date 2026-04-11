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

class AddExpenseActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val availableFlatmates = intent.getStringArrayListExtra("flatmatesList") ?: arrayListOf()
        val initialTitle = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val initialPaidBy = intent.getStringExtra(EXTRA_PAID_BY) ?: ""
        val initialSplitBetween = intent.getStringArrayListExtra(EXTRA_SPLIT_BETWEEN) ?: arrayListOf()

        setContent {
            ChillMatesTheme {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text(stringResource(R.string.btn_add_expense)) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    ExpenseFormContent(
                        modifier = Modifier.padding(innerPadding),
                        initialTitle = initialTitle,
                        initialPaidBy = initialPaidBy,
                        initialSplitBetween = initialSplitBetween,
                        availableFlatmates = availableFlatmates,
                        onSave = { title, amount, paidBy, splitBetween ->
                            val resultIntent = Intent().apply {
                                putExtra(EXTRA_TITLE, title)
                                putExtra(EXTRA_AMOUNT, amount)
                                putExtra(EXTRA_PAID_BY, paidBy)
                                putStringArrayListExtra(EXTRA_SPLIT_BETWEEN, ArrayList(splitBetween))
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_PAID_BY = "extra_paid_by"
        const val EXTRA_SPLIT_BETWEEN = "extra_split_between"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormContent(
    modifier: Modifier = Modifier,
    initialTitle: String,
    initialPaidBy: String,
    initialSplitBetween: List<String>,
    availableFlatmates: List<String>,
    onSave: (String, Double, String, List<String>) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var amountStr by rememberSaveable { mutableStateOf("") }

    val defaultPaidBy = if (
        initialPaidBy.isNotBlank() && availableFlatmates.contains(initialPaidBy)
    ) {
        initialPaidBy
    } else {
        availableFlatmates.firstOrNull() ?: ""
    }

    var paidBy by rememberSaveable { mutableStateOf(defaultPaidBy) }
    var expandedDropdown by remember { mutableStateOf(false) }

    val initialSelection = if (initialSplitBetween.isNotEmpty()) {
        initialSplitBetween.toSet()
    } else {
        availableFlatmates.toSet()
    }

    var selectedFlatmates by remember { mutableStateOf(initialSelection) }

    val amountValue = amountStr.toDoubleOrNull()
    val canSave = title.isNotBlank() && amountValue != null && amountValue > 0 && paidBy.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.label_chore_title)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = amountStr,
            onValueChange = { amountStr = it },
            label = { Text("Amount (€)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.label_paid_by),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expandedDropdown,
            onExpandedChange = { expandedDropdown = !expandedDropdown }
        ) {
            OutlinedTextField(
                value = paidBy,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expandedDropdown,
                onDismissRequest = { expandedDropdown = false }
            ) {
                availableFlatmates.forEach { person ->
                    DropdownMenuItem(
                        text = { Text(person) },
                        onClick = {
                            paidBy = person
                            expandedDropdown = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                        selectedFlatmates =
                            if (isChecked) selectedFlatmates + person
                            else selectedFlatmates - person
                    }
                )
                Text(text = person)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = {
                val amount = amountValue ?: return@Button
                onSave(title.trim(), amount, paidBy, selectedFlatmates.toList())
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_save))
        }
    }
}