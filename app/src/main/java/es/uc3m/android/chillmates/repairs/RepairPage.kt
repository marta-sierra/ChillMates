package es.uc3m.android.chillmates.repairs

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import es.uc3m.android.chillmates.LargeSectionHeader
import es.uc3m.android.chillmates.PageContentWrapper
import es.uc3m.android.chillmates.R

@Composable
fun RepairPage(navController: NavController) {
    val repairs = listOf(
        Repair("Bathroom door", "Loose hinge. Landlord notified.", "Pending"),
        Repair("Washing machine", "Makes weird noise when spinning.", "In progress"),
        Repair("Sofa", "New sofa delivery completed.", "Resolved")
    )

    var requestText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LargeSectionHeader(title = stringResource(R.string.repairs_title))

        PageContentWrapper {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.repairs_section_current),
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

                item {
                    Text(
                        text = stringResource(R.string.repairs_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = requestText,
                        onValueChange = { requestText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text(stringResource(R.string.repairs_placeholder_describe)) },
                        shape = MaterialTheme.shapes.medium
                    )
                }

                item {
                    Column {
                        TextButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text("📷", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.width(8.dp))
                            val label = if (selectedImageUri == null)
                                stringResource(R.string.repairs_btn_add_images)
                            else
                                stringResource(R.string.repairs_btn_retake_photo)
                            Text(label)
                        }

                        // Image Preview if selected
                        selectedImageUri?.let { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = "Selected Image",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .padding(vertical = 8.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { /* AI logic */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.repairs_btn_ai_review))
                        }

                        Button(
                            onClick = {
                                val subject = "Repair Request: ${requestText.take(30)}"
                                val body = "Issue: $requestText\n\nSent from ChillMates app"

                                try {
                                    if (selectedImageUri != null) {
                                        val emailIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "message/rfc822"
                                            putExtra(Intent.EXTRA_EMAIL, arrayOf("landlord@chillmates.app"))
                                            putExtra(Intent.EXTRA_SUBJECT, subject)
                                            putExtra(Intent.EXTRA_TEXT, body)
                                            putExtra(Intent.EXTRA_STREAM, selectedImageUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(emailIntent, "Send email..."))
                                    } else {
                                        val uriText = "mailto:landlord@chillmates.app" +
                                                "?subject=" + Uri.encode(subject) +
                                                "&body=" + Uri.encode(body)
                                        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse(uriText))
                                        context.startActivity(emailIntent)
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No email app installed.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.repairs_btn_send))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RepairCard(repair: Repair) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                contentDescription = null,
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

                // Fixed Badge implementation
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = when(repair.status) {
                        "Resolved" -> MaterialTheme.colorScheme.tertiaryContainer
                        "In progress" -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        text = repair.status,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = when(repair.status) {
                            "Resolved" -> MaterialTheme.colorScheme.onTertiaryContainer
                            "In progress" -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }
        }
    }
}

data class Repair(val issue: String, val description: String, val status: String)