// AddExpenseActivity.kt
package es.uc3m.android.chillmates

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.uc3m.android.chillmates.ui.theme.ChillMatesTheme

// Activity that hosts the "Add expense" screen and returns the entered data to the caller
class AddExpenseActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ChillMatesTheme {
                AddExpenseScreen(
                    // User cancels: return RESULT_CANCELED to the previous screen
                    onBack = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    },
                    // User saves: pack the form data into an Intent and return RESULT_OK
                    onSave = { title: String, amount: Double, paidBy: String ->
                        val data = Intent().apply {
                            putExtra(EXTRA_TITLE, title)
                            putExtra(EXTRA_AMOUNT, amount)
                            putExtra(EXTRA_PAID_BY, paidBy)
                        }
                        setResult(Activity.RESULT_OK, data)
                        finish()
                    }
                )
            }
        }
    }

    // Keys used to pass data back to the caller
    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_PAID_BY = "extra_paid_by"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseScreen(
    onBack: () -> Unit,
    onSave: (String, Double, String) -> Unit
) {
    val context = LocalContext.current

    // values typed by the user
    var title by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var paidBy by rememberSaveable { mutableStateOf("") }

    // convert the amount field into a Double
    val amountValue: Double? = amountText.replace(",", ".").toDoubleOrNull()
    val canSave = title.isNotBlank() && paidBy.isNotBlank() && (amountValue != null)

    // Screen structure: top app bar + content area
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add expense") },
                navigationIcon = {
                    // Back navigation for this screen
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        // Main content: form fields + save button
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // Section header text
            Text(
                text = "Fill the details",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title input.
            OutlinedTextField(
                value = title,
                onValueChange = { newValue -> title = newValue },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Amount input (numeric keyboard).
            OutlinedTextField(
                value = amountText,
                onValueChange = { newValue -> amountText = newValue },
                label = { Text("Amount (€)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // "Paid by" input
            OutlinedTextField(
                value = paidBy,
                onValueChange = { newValue -> paidBy = newValue },
                label = { Text("Paid by") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Save action. Only enabled when validation passes
            Button(
                onClick = {
                    val amount = amountValue ?: return@Button
                    onSave(title.trim(), amount, paidBy.trim())
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}