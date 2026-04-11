package es.uc3m.android.chillmates.repairs

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.uc3m.android.chillmates.LargeSectionHeader
import es.uc3m.android.chillmates.PageContentWrapper

private const val LANDLORD_EMAIL = "landlord@chillmates.app"

@Composable
fun RepairPage(navController: NavController) {
    val vm: RepairViewModel = viewModel()
    val context = LocalContext.current

    var requestText by rememberSaveable { mutableStateOf("") }
    var reviewedText by rememberSaveable { mutableStateOf("") }
    var selectedPhotoUri by rememberSaveable { mutableStateOf<String?>(null) }

    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedPhotoUri = uri?.toString()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LargeSectionHeader(title = "Repairs")

        PageContentWrapper {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(vm.uiState.repairs) { repair ->
                        RepairRequestCard(
                            repair = repair,
                            onStatusSelected = { status ->
                                vm.updateRepairStatus(repair.id, status)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Send a new request",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = requestText,
                        onValueChange = { requestText = it },
                        modifier = Modifier.fillMaxSize(),
                        placeholder = { Text("Describe the issue...") }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = {
                        pickMedia.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (selectedPhotoUri == null) "Add images"
                        else "Image added"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            reviewedText = vm.generateReviewedMessage(requestText)
                            if (reviewedText.isNotBlank()) {
                                requestText = reviewedText
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("AI Review")
                    }

                    Button(
                        onClick = {
                            val finalRepair = vm.createAndMarkSent(
                                rawDescription = requestText,
                                finalMessage = requestText,
                                photoUri = selectedPhotoUri
                            ) ?: return@Button

                            val emailIntent = if (selectedPhotoUri != null) {
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "image/*"
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf(LANDLORD_EMAIL))
                                    putExtra(
                                        Intent.EXTRA_SUBJECT,
                                        "Repair Request: Shared Flat"
                                    )
                                    putExtra(Intent.EXTRA_TEXT, finalRepair.landlordMessage)
                                    putExtra(Intent.EXTRA_STREAM, Uri.parse(selectedPhotoUri))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                            } else {
                                Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:$LANDLORD_EMAIL")
                                    putExtra(
                                        Intent.EXTRA_SUBJECT,
                                        "Repair Request: Shared Flat"
                                    )
                                    putExtra(Intent.EXTRA_TEXT, finalRepair.landlordMessage)
                                }
                            }

                            context.startActivity(
                                Intent.createChooser(emailIntent, "Send repair request")
                            )

                            requestText = ""
                            reviewedText = ""
                            selectedPhotoUri = null
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50),
                        enabled = requestText.isNotBlank()
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    }
}

@Composable
private fun RepairRequestCard(
    repair: RepairUi,
    onStatusSelected: (String) -> Unit
) {
    var statusMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = repair.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = repair.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box {
                    StatusChip(
                        status = repair.status,
                        onClick = { statusMenuExpanded = true }
                    )

                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false }
                    ) {
                        listOf(
                            "PENDING",
                            "SENT",
                            "IN_PROGRESS",
                            "ACCEPTED",
                            "DENIED",
                            "RESOLVED"
                        ).forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status) },
                                onClick = {
                                    onStatusSelected(status)
                                    statusMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    status: String,
    onClick: () -> Unit
) {
    val background = when (status) {
        "PENDING" -> Color(0xFFD84C3E)
        "SENT" -> Color(0xFF4F6DB8)
        "IN_PROGRESS" -> Color(0xFF5A647A)
        "ACCEPTED" -> Color(0xFF2E8B57)
        "DENIED" -> Color(0xFF8B0000)
        "RESOLVED" -> Color(0xFF7B5A7A)
        else -> MaterialTheme.colorScheme.primary
    }

    TextButton(
        onClick = onClick,
        modifier = Modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 2.dp, vertical = 0.dp)
    ) {
        Text(
            text = status.replace("_", " "),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
    }
}