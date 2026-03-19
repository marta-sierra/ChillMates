package es.uc3m.android.chillmates.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.uc3m.android.chillmates.R
import es.uc3m.android.chillmates.ui.theme.ChillMatesTheme

/**
 * Activity for adding a new event.
 * Returns the event details to the calling activity.
 */
class AddEventActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ChillMatesTheme {
                AddEventScreen(
                    onBack = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    onSave = { dateLabel, description, location ->
                        val data = Intent().apply {
                            putExtra(EXTRA_DATE_LABEL, dateLabel)
                            putExtra(EXTRA_DESCRIPTION, description)
                            putExtra(EXTRA_LOCATION, location)
                        }
                        setResult(RESULT_OK, data)
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
    var day by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }

    //Day, Time, and Description are required.
    val canSave = day.trim().isNotBlank() &&
            time.trim().isNotBlank() &&
            description.trim().isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_event_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.add_event_cd_back)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = stringResource(R.string.add_event_header),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Day input (Weekday shorthand)
            OutlinedTextField(
                value = day,
                onValueChange = { day = it },
                label = { Text(stringResource(R.string.add_event_label_day)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Time input (HH:MM)
            OutlinedTextField(
                value = time,
                onValueChange = { time = it },
                label = { Text(stringResource(R.string.add_event_label_time)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Event Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.add_event_label_description)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Optional Location
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text(stringResource(R.string.add_event_label_location)) },
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
                Text(stringResource(R.string.add_event_btn_save))
            }
        }
    }
}