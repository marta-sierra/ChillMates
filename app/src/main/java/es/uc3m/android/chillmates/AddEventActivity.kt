// AddEventActivity.kt
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import es.uc3m.android.chillmates.ui.theme.ChillMatesTheme

// "Add event" screen. return the entered data to the caller [UI]
class AddEventActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ChillMatesTheme {
                AddEventScreen(
                    onBack = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    },
                    onSave = { dateLabel: String, description: String, location: String ->
                        val data = Intent().apply {
                            putExtra(EXTRA_DATE_LABEL, dateLabel)
                            putExtra(EXTRA_DESCRIPTION, description)
                            putExtra(EXTRA_LOCATION, location)
                        }
                        setResult(Activity.RESULT_OK, data)
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_DATE_LABEL = "extra_date_label"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_LOCATION = "extra_location"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEventScreen(
    onBack: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    // values typed by the user
    var day by rememberSaveable { mutableStateOf("") }        // e.g., "Fri"
    var time by rememberSaveable { mutableStateOf("") }       // e.g., "20:00"
    var description by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }

    // simple validation for required fields
    val canSave = day.trim().isNotBlank() && time.trim().isNotBlank() && description.trim().isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add event") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Fill the details",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Day input (expects Mon/Tue/Wed/Thu/Fri/Sat/Sun)
            OutlinedTextField(
                value = day,
                onValueChange = { newValue -> day = newValue },
                label = { Text("Day (Mon/Tue/Wed/Thu/Fri/Sat/Sun)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Time input
            OutlinedTextField(
                value = time,
                onValueChange = { newValue -> time = newValue },
                label = { Text("Time (HH:MM)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description input
            OutlinedTextField(
                value = description,
                onValueChange = { newValue -> description = newValue },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Location input (optional)
            OutlinedTextField(
                value = location,
                onValueChange = { newValue -> location = newValue },
                label = { Text("Location (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val dateLabel = "${day.trim()} ${time.trim()}"
                    onSave(dateLabel, description.trim(), location.trim())
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}