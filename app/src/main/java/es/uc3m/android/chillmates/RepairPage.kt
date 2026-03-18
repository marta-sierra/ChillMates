package es.uc3m.android.chillmates

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage

@Composable
fun RepairPage(navController: NavController) {
    // Standard reparations data
    val repairs = listOf(
        Repair("Bathroom door", "Loose hinge. Landlord notified.", "Pending"),
        Repair("Washing machine", "Makes weird noise when spinning.", "In progress"),
        Repair("Sofa", "New sofa delivery completed.", "Resolved")
    )

    // State to hold user input and selected image URI
    var requestText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Context needed to launch intents and show toasts
    val context = LocalContext.current

    // Launcher to pick a single image from storage/camera via system picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Column(modifier = Modifier.fillMaxSize()) {
        RepairHeaderSection(title = "Repairs")

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- SECTION: CURRENT REPARATIONS ---
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Current reparations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(repairs) { repair ->
                RepairCard(repair)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // --- SECTION: SEND A NEW REQUEST ---
            item {
                Text(
                    text = "Send a new request",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Text input for describing the issue
            item {
                OutlinedTextField(
                    value = requestText,
                    onValueChange = { requestText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = { Text("Describe the issue...") },
                    shape = MaterialTheme.shapes.medium
                )
            }

            // Functional button to add or retake an image
            item {
                TextButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("📷", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedImageUri == null) "Add images" else "Retake photo")
                }
            }

            // If an image is selected/captured, display it using AsyncImage (Coil)
            if (selectedImageUri != null) {
                item {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected issue photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Action buttons section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* AI Review logic */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "AI Review",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("AI Review")
                    }

                    Button(
                        onClick = {
                            try {
                                if (selectedImageUri != null) {
                                    // Intent with image attachment
                                    val emailIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "message/rfc822"
                                        putExtra(Intent.EXTRA_EMAIL, arrayOf("jimenofernandezf@gmail.com"))
                                        putExtra(Intent.EXTRA_SUBJECT, "Repair Request: Shared Flat")
                                        putExtra(Intent.EXTRA_TEXT, "Issue description:\n$requestText")
                                        putExtra(Intent.EXTRA_STREAM, selectedImageUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(emailIntent, "Send email..."))
                                } else {
                                    // Robust Intent with text only
                                    val uriText = "mailto:landlord@chillmates.app" +
                                            "?subject=" + Uri.encode("Repair Request: Shared Flat") +
                                            "&body=" + Uri.encode("Issue description:\n$requestText")
                                    val uri = Uri.parse(uriText)
                                    val emailIntent = Intent(Intent.ACTION_SENDTO, uri)
                                    context.startActivity(emailIntent)
                                }
                            } catch (e: Exception) {
                                // Fallback if no email app is installed
                                Toast.makeText(
                                    context,
                                    "No email app installed to handle this request.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Send")
                    }
                }
            }
        }
    }
}

@Composable
private fun RepairHeaderSection(title: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp, bottom = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun RepairCard(repair: Repair) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Build,
                contentDescription = "Tool icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 12.dp)
            )
            Column {
                Text(
                    text = repair.issue,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = repair.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Badge(
                    containerColor = when(repair.status) {
                        "Resolved" -> MaterialTheme.colorScheme.tertiary
                        "In progress" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error // Error red for pending
                    }
                ) {
                    Text(
                        text = repair.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

data class Repair(val issue: String, val description: String, val status: String)