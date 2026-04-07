package es.uc3m.android.chillmates.shoppinglist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items // Importante para el LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import es.uc3m.android.chillmates.R
import es.uc3m.android.chillmates.model.Supermarket // Importación corregida

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupermarketMapScreen(
    supermarkets: List<Supermarket>, // Corregido: Tipo de clase
    selectedIds: Set<String>,
    onToggleSelection: (String) -> Unit,
    onDeleteSupermarket: (String) -> Unit,
    onConfirm: (Set<String>) -> Unit,
    onCancel: () -> Unit,
    onAddClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf<Supermarket?>(null) }
    val cameraPositionState = rememberCameraPositionState()

    // Ajustar la cámara automáticamente cuando carguen los supermercados
    LaunchedEffect(supermarkets) {
        if (supermarkets.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.builder()
            supermarkets.forEach { boundsBuilder.include(it.toLatLng()) }
            try {
                val bounds = boundsBuilder.build()
                cameraPositionState.position = CameraPosition.fromLatLngZoom(bounds.center, 13f)
            } catch (e: Exception) {
                supermarkets.firstOrNull()?.let {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(it.toLatLng(), 14f)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.select_supermarkets)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onAddClick) {
                        Icon(Icons.Default.AddLocation, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (supermarkets.isEmpty()) {
                EmptySupermarketsContent(onAddClick = onAddClick)
            } else {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false)
                ) {
                    supermarkets.forEach { supermarket ->
                        val isSelected = supermarket.id in selectedIds
                        Marker(
                            state = MarkerState(position = supermarket.toLatLng()),
                            title = supermarket.name,
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (isSelected) BitmapDescriptorFactory.HUE_GREEN
                                else BitmapDescriptorFactory.HUE_RED
                            ),
                            onClick = {
                                supermarket.id?.let { onToggleSelection(it) }
                                false // Permite que se muestre el info window si quieres
                            }
                        )
                    }
                }

                // Panel inferior con la lista horizontal y acciones
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    tonalElevation = 8.dp,
                    shadowElevation = 10.dp
                ) {
                    BottomBarContent(
                        supermarkets = supermarkets,
                        selectedIds = selectedIds,
                        onAddClick = onAddClick,
                        onToggleSelection = onToggleSelection,
                        onShowDeleteDialog = { showDeleteDialog = it },
                        onConfirm = onConfirm,
                        onCancel = onCancel
                    )
                }
            }
        }
    }

    // Diálogo de confirmación para borrar
    showDeleteDialog?.let { supermarket ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.delete_supermarket)) },
            text = { Text(stringResource(R.string.delete_supermarket_confirm, supermarket.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        supermarket.id?.let { onDeleteSupermarket(it) }
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.btn_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun BottomBarContent(
    supermarkets: List<Supermarket>,
    selectedIds: Set<String>,
    onAddClick: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onShowDeleteDialog: (Supermarket) -> Unit,
    onConfirm: (Set<String>) -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text(
            text = if (selectedIds.isEmpty()) stringResource(R.string.no_supermarkets_selected)
            else stringResource(R.string.supermarkets_selected_count, selectedIds.size),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(supermarkets) { supermarket ->
                val isSelected = supermarket.id in selectedIds
                FilterChip(
                    selected = isSelected,
                    onClick = { supermarket.id?.let { onToggleSelection(it) } },
                    label = { Text(supermarket.name) },
                    trailingIcon = {
                        if (!isSelected) {
                            Icon(
                                Icons.Default.Close,
                                null,
                                Modifier.size(16.dp).clickable { onShowDeleteDialog(supermarket) }
                            )
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, Modifier.weight(1f)) {
                Text(stringResource(R.string.btn_cancel))
            }
            Button(onClick = { onConfirm(selectedIds) }, Modifier.weight(1f)) {
                Text(stringResource(R.string.btn_confirm_filter, selectedIds.size))
            }
        }
    }
}

@Composable
private fun EmptySupermarketsContent(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.AddLocation, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.no_supermarkets), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.add_supermarket_hint), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAddClick) {
            Text(stringResource(R.string.btn_add_supermarket))
        }
    }
}