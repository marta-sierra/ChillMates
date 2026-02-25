package es.uc3m.android.chillmates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Undo
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle

@Composable
fun ShoppingListPage(navController: NavController) {
    val vm: ShoppingViewModel = viewModel()

    ShoppingListRoute(
        state = vm.uiState,
        onFilter = vm::onFilterSupermarket,
        onToggleBought = vm::onToggleBought,
        onDelete = vm::onDeleteItem,
        onAddItem = vm::addItem
    )
}

@Composable
private fun ShoppingListRoute(
    state: ShoppingUiState,
    onFilter: (String?) -> Unit,
    onToggleBought: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAddItem: (ShoppingItemUi) -> Unit
) {
    var showMap by remember { mutableStateOf(false) }
    var showBoughtItems by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Shopping List",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(Modifier.height(16.dp))

        // Filter + Map row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SupermarketDropdown(
                selected = state.selectedSupermarket,
                onFilter = onFilter,
                supermarkets = state.availableSupermarkets,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { showMap = true }) {
                Icon(Icons.Default.Map, contentDescription = "Open Map")
            }
        }

        Spacer(Modifier.height(16.dp))

        // 🔹 LIST TAKES ALL AVAILABLE SPACE
        Box(modifier = Modifier.weight(1f)) {

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filteredItems = if (state.selectedSupermarket == null) {
                    state.items
                } else {
                    state.items.filter { state.selectedSupermarket in it.supermarkets }
                }

                val itemsToShow = if (showBoughtItems) {
                    filteredItems
                } else {
                    filteredItems.filter { !it.isBought }
                }

                items(itemsToShow) { item ->
                    ShoppingItemRow(
                        item = item,
                        onToggleBought = onToggleBought,
                        onDelete = onDelete
                    )
                }
            }
        }

        // 🔹 ALWAYS AT BOTTOM
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add new item")
            }

            OutlinedButton(
                onClick = { showBoughtItems = !showBoughtItems },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    if (showBoughtItems) "Hide recently bought items"
                    else "See recently bought items"
                )
            }
        }
        if (showAddDialog) {
            AddItemDialog(
                supermarkets = state.availableSupermarkets,
                flatmates = listOf("Marta", "Alex", "John"),
                onDismiss = { showAddDialog = false },
                onAdd = { newItem ->
                    onAddItem(newItem)
                    showAddDialog = false
                }
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupermarketDropdown(
    supermarkets: List<String>,
    selected: String?,
    onFilter: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded},
        modifier=modifier
    ) {
        TextField(
            value = selected ?: stringResource(R.string.filter_all),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.filter_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Reset filter
            DropdownMenuItem(
                text = { Text(stringResource(R.string.filter_all)) },
                onClick = {
                    onFilter(null)
                    expanded = false
                }
            )
            // Supermarket options
            supermarkets.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onFilter(name)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingItemUi,
    onToggleBought: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val textDecoration =
        if (item.isBought) TextDecoration.LineThrough
        else TextDecoration.None

    val alpha =
        if (item.isBought) 0.6f
        else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        // 🔹 LEFT SIDE (Info)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            // Name + Quantity
            Text(
                text = "${item.name} (${item.quantity})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textDecoration = textDecoration
            )

            // Needed By
            val neededByText =
                if (item.neededBy.isEmpty()) "for all"
                else "for ${item.neededBy.joinToString(", ")}"

            Text(
                text = neededByText,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic
            )
        }

        Spacer(Modifier.width(12.dp))

        // 🔹 RIGHT SIDE (Actions)
        Row {
            IconButton(
                onClick = { onToggleBought(item.id) }
            ) {
                Icon(
                    imageVector =
                        if (item.isBought)
                            Icons.Default.Undo
                        else
                            Icons.Default.Check,
                    contentDescription = "Toggle Bought"
                )
            }
            IconButton(
                onClick = { onDelete(item.id) }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete"
                )
            }
        }
    }
}

// ADD NEW ITEM POP-UP
@Composable
private fun AddItemDialog(
    supermarkets: List<String>,
    flatmates: List<String>,
    onDismiss: () -> Unit,
    onAdd: (ShoppingItemUi) -> Unit
) {

    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }

    var selectedSupermarkets by remember { mutableStateOf(setOf<String>()) }
    var anySupermarket by remember { mutableStateOf(true) }

    var selectedFlatmates by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(
                            ShoppingItemUi(
                                id = System.currentTimeMillis().toString(),
                                name = name,
                                quantity = quantity,
                                neededBy = selectedFlatmates.toList(),
                                supermarkets =
                                    if (anySupermarket) emptyList()
                                    else selectedSupermarkets.toList()
                            )
                        )
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Add New Item") },
        text = {

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth()
                )

                // 🔹 SUPERMARKETS
                Text("Supermarkets", fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = anySupermarket,
                        onCheckedChange = {
                            anySupermarket = it
                            if (it) selectedSupermarkets = emptySet()
                        }
                    )
                    Text("Any")
                }

                if (!anySupermarket) {
                    supermarkets.forEach { market ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = market in selectedSupermarkets,
                                onCheckedChange = {
                                    selectedSupermarkets =
                                        if (it) selectedSupermarkets + market
                                        else selectedSupermarkets - market
                                }
                            )
                            Text(market)
                        }
                    }
                }

                // 🔹 FLATMATES
                Text("For who", fontWeight = FontWeight.Bold)

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = {
                        selectedFlatmates = flatmates.toSet()
                    }) {
                        Text("Select All")
                    }

                    TextButton(onClick = {
                        selectedFlatmates = emptySet()
                    }) {
                        Text("Unselect All")
                    }
                }

                flatmates.forEach { person ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = person in selectedFlatmates,
                            onCheckedChange = {
                                selectedFlatmates =
                                    if (it) selectedFlatmates + person
                                    else selectedFlatmates - person
                            }
                        )
                        Text(person)
                    }
                }
            }
        }
    )
}

// TODO: PLACE MAP HERE
@Composable
private fun MapPopup(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
            ) {
                // Placeholder for future Map
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🗺 Map Placeholder")
                }

                // Close button (X)
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }
        }
    )
}
