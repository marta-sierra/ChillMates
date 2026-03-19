package es.uc3m.android.chillmates.shoppinglist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.uc3m.android.chillmates.R

/**
 * Activity that displays a map for selecting a supermarket, until map works we have a placeholder.
 * It returns the selected location to the caller.
 */
class MapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window.setGravity(Gravity.BOTTOM)

        setContent {
            // TODO: Read data from DB.
            val supermarkets = listOf( "Mercadona", "Lidl", "Carrefour", "Dia")
            var selectedLocation by remember { mutableStateOf(supermarkets[0]) }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.select_supermarket),
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Selection chips simulating map behaviour
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(480.dp)
                            .background(Color.LightGray, shape = MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Map Placeholder",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            supermarkets.forEach { market ->
                                FilterChip(
                                    selected = selectedLocation == market,
                                    onClick = { selectedLocation = market },
                                    label = { Text(market) },
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons for cancelling or confirming selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { finish() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.btn_cancel))
                        }

                        Button(
                            onClick = {
                                val resultIntent = Intent().apply {
                                    putExtra("selectedLocation", selectedLocation)
                                }
                                setResult(Activity.RESULT_OK, resultIntent)
                                finish()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.btn_confirm))
                        }
                    }
                }
            }
        }
    }
}