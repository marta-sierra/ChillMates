package es.uc3m.android.chillmates

import android.app.Activity
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.uc3m.android.chillmates.ui.theme.ChillMatesTheme

class AddExpenseActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Extraemos los datos del Intent que viene de la ShoppingListPage
        val availableFlatmates = intent.getStringArrayListExtra("FLATMATES_LIST") ?: arrayListOf()
        val initialTitle = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val initialPaidBy = intent.getStringExtra(EXTRA_PAID_BY) ?: ""
        val initialSplit = intent.getStringArrayListExtra(EXTRA_SPLIT_BETWEEN) ?: emptyList<String>()

        setContent {
            ChillMatesTheme {
                AddExpenseScreen(
                    availableFlatmates = availableFlatmates,
                    // 2. Pasamos los valores iniciales al Composable
                    initialTitle = initialTitle,
                    initialPaidBy = initialPaidBy,
                    initialSplit = initialSplit,
                    onBack = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    },
                    onSave = { title, amount, paidBy, splitBetween ->
                        val data = Intent().apply {
                            putExtra(EXTRA_TITLE, title)
                            putExtra(EXTRA_AMOUNT, amount)
                            putExtra(EXTRA_PAID_BY, paidBy)
                            putStringArrayListExtra(EXTRA_SPLIT_BETWEEN, ArrayList(splitBetween))
                        }
                        setResult(Activity.RESULT_OK, data)
                        finish()
                    }
                )
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
private fun AddExpenseScreen(
    availableFlatmates: List<String>,
    initialTitle: String,      // 👈 Nuevo
    initialPaidBy: String,    // 👈 Nuevo
    initialSplit: List<String>, // 👈 Nuevo
    onBack: () -> Unit,
    onSave: (String, Double, String, List<String>) -> Unit
) {
    // 3. Inicializamos los estados con los valores recibidos
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var paidBy by rememberSaveable { mutableStateOf(initialPaidBy) }

    // Si initialSplit está vacío, seleccionamos a todos por defecto; si no, solo a los recibidos
    var selectedFlatmates by remember {
        mutableStateOf(if (initialSplit.isEmpty()) availableFlatmates.toSet() else initialSplit.toSet())
    }

    val amountValue: Double? = amountText.replace(",", ".").toDoubleOrNull()
    val canSave = title.isNotBlank() && paidBy.isNotBlank() && (amountValue != null) && selectedFlatmates.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add expense") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
            Text(text = "Fill the details", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount (€)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = paidBy,
                onValueChange = { paidBy = it },
                label = { Text("Paid by") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(text = "Split between", fontWeight = FontWeight.Bold)
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
                Text("Save")
            }
        }
    }
}