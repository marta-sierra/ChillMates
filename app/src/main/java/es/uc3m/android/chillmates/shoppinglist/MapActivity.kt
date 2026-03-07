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
import androidx.compose.ui.unit.dp

class MapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Window configuration for pop-up aesthetics
        window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window.setGravity(Gravity.BOTTOM)

        setContent {
            val supermarkets = listOf("Mercadona", "Lidl", "Carrefour", "Dia")
            var selectedLocation by remember { mutableStateOf("Mercadona") }
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
                        text = "Select Supermarket",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // MAP CONTAINER (mimicking map behaviour with filter chip)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(480.dp)
                            .background(Color.LightGray, shape = MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "[Map Placeholder]",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Chips to simulate clicking a marker on a map
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

                    // ACTION BUTTONS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Cancel -> close activity without sending data
                        OutlinedButton(
                            onClick = { finish() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        // Confirm -> close activity sending data back to parent
                        Button(
                            onClick = {
                                // Create result intent with selected location
                                val resultIntent = Intent().apply {
                                    putExtra("selected_location", selectedLocation)
                                }
                                setResult(Activity.RESULT_OK, resultIntent)
                                finish()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }
}