package es.uc3m.android.chillmates.shoppinglist

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import es.uc3m.android.chillmates.expensestracker.AddExpenseActivity
import es.uc3m.android.chillmates.LargeSectionHeader
import es.uc3m.android.chillmates.PageContentWrapper
import es.uc3m.android.chillmates.R
import es.uc3m.android.chillmates.model.Supermarket

@Composable
fun ShoppingListPage(navController: NavController) {
    val context = LocalContext.current
    val vm: ShoppingViewModel = viewModel()
    val uiState by vm.uiState.collectAsState()

    var showMap by remember { mutableStateOf(false) }
    var showAddSupermarketDialog by remember { mutableStateOf(false) }

    if (showMap) {
        SupermarketMapScreen(
            supermarkets = uiState.availableSupermarkets,
            selectedIds = uiState.selectedSupermarkets,
            onToggleSelection = { vm.onToggleSupermarketSelection(it) },
            onDeleteSupermarket = { id ->
                vm.deleteSupermarket(id) { result: Result<Unit> ->
                    result.onFailure { e ->
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onConfirm = { selected ->
                vm.onFilterSupermarkets(selected)
                showMap = false
            },
            onCancel = { showMap = false },
            onAddClick = { showAddSupermarketDialog = true }
        )
    }

    if (showAddSupermarketDialog) {
        AddSupermarketDialog(
            onDismiss = { showAddSupermarketDialog = false },
            onAdd = { name: String, lat: Double, lng: Double, address: String ->
                vm.addSupermarket(
                    Supermarket(
                        name = name,
                        latitude = lat,
                        longitude = lng,
                        address = address
                    )
                ) { result: Result<Supermarket> ->
                    result.onSuccess {
                        Toast.makeText(context, context.getString(R.string.supermarket_added), Toast.LENGTH_SHORT).show()
                    }
                    result.onFailure { e ->
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
                showAddSupermarketDialog = false
            }
        )
    }

    val itemToPrompt by remember { mutableStateOf<ShoppingItemUi?>(null) }
    val expenseLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    ShoppingListRoute(
        state = uiState,
        onToggleBought = { itemId ->
            vm.onToggleBought(itemId)
            val item = uiState.items.find { it.id == itemId }
            if (item?.isBought == true) {
                itemToPrompt?.let {
                    // handled in dialog below
                }
            }
        },
        onDelete = vm::onDeleteItem,
        onAddItem = vm::addItem,
        onOpenMap = { showMap = true },
        onAddSupermarket = { showAddSupermarketDialog = true }
    )

    if (itemToPrompt != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.dialog_add_expense_title)) },
            text = {
                Text(stringResource(R.string.dialog_add_expense_text, itemToPrompt?.name ?: ""))
            },
            confirmButton = {
                Button(onClick = {
                    val item = itemToPrompt!!
                    val intent = Intent(context, AddExpenseActivity::class.java).apply {
                        putExtra(AddExpenseActivity.EXTRA_TITLE, item.name)
                        putExtra(AddExpenseActivity.EXTRA_AMOUNT, 0.0)
                        putExtra(AddExpenseActivity.EXTRA_PAID_BY, "ME")
                        putStringArrayListExtra("flatmatesList", ArrayList(uiState.availableFlatmates))
                        putExtra(AddExpenseActivity.EXTRA_SPLIT_BETWEEN, ArrayList(item.neededBy))
                    }
                    expenseLauncher.launch(intent)
                }) {
                    Text(stringResource(R.string.btn_create_expense))
                }
            },
            dismissButton = {
                TextButton(onClick = { }) {
                    Text(stringResource(R.string.btn_not_now))
                }
            }
        )
    }
}

@Composable
private fun ShoppingListRoute(
    state: ShoppingUiState,
    onToggleBought: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAddItem: (ShoppingItemUi) -> Unit,
    onOpenMap: () -> Unit,
    onAddSupermarket: () -> Unit
) {
    var showBoughtItems by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        LargeSectionHeader(title = stringResource(R.string.shopping_list_title))
        PageContentWrapper {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.selectedSupermarkets.isNotEmpty(),
                        onClick = onOpenMap,
                        label = {
                            Text(
                                if (state.selectedSupermarkets.isEmpty()) {
                                    stringResource(R.string.filter_all)
                                } else {
                                    stringResource(R.string.supermarkets_selected_count, state.selectedSupermarkets.size)
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Map,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )

                    if (state.selectedSupermarkets.isNotEmpty()) {
                        AssistChip(
                            onClick = onAddSupermarket,
                            label = { Text(stringResource(R.string.btn_add)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.AddLocation,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filteredItems = if (state.selectedSupermarkets.isEmpty()) {
                            state.items
                        } else {
                            state.items.filter { item ->
                                item.supermarkets.isEmpty() ||
                                item.supermarkets.any { it in state.selectedSupermarkets }
                            }
                        }

                        val itemsToShow = if (showBoughtItems) {
                            filteredItems
                        } else {
                            filteredItems.filter { !it.isBought }
                        }

                        items(itemsToShow) { item ->
                            ShoppingItemRow(
                                item = item,
                                supermarketNames = state.availableSupermarkets
                                    .filter { s -> s.id in item.supermarkets }
                                    .map { s -> s.name },
                                onToggleBought = onToggleBought,
                                onDelete = onDelete
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_add_new_item))
                    }

                    OutlinedButton(
                        onClick = { showBoughtItems = !showBoughtItems },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            if (showBoughtItems) stringResource(R.string.btn_hide_bought)
                            else stringResource(R.string.btn_see_bought)
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            supermarkets = state.availableSupermarkets.map { s -> s.id!! to s.name },
            flatmates = state.availableFlatmates,
            onDismiss = { showAddDialog = false },
            onAdd = { newItem ->
                onAddItem(newItem)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingItemUi,
    supermarketNames: List<String>,
    onToggleBought: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val textDecoration = if (item.isBought) TextDecoration.LineThrough else TextDecoration.None
    val alpha = if (item.isBought) 0.6f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${item.name} (${item.quantity})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textDecoration = textDecoration
            )

            val neededByText = if (item.neededBy.isEmpty()) {
                stringResource(R.string.needed_by_all)
            } else {
                stringResource(R.string.needed_by_format, item.neededBy.joinToString(", "))
            }

            Text(
                text = neededByText,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic
            )

            if (supermarketNames.isNotEmpty()) {
                Text(
                    text = supermarketNames.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (item.supermarkets.isEmpty()) {
                Text(
                    text = stringResource(R.string.any_supermarket),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Row {
            IconButton(onClick = { onToggleBought(item.id) }) {
                Icon(
                    imageVector = if (item.isBought) Icons.AutoMirrored.Filled.Undo else Icons.Default.Check,
                    contentDescription = stringResource(R.string.cd_toggle_bought)
                )
            }
            IconButton(onClick = { onDelete(item.id) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_delete)
                )
            }
        }
    }
}

@Composable
private fun AddItemDialog(
    supermarkets: List<Pair<String, String>>,
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
                                supermarkets = if (anySupermarket) emptyList() else selectedSupermarkets.toList()
                            )
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.btn_add))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
        title = { Text(stringResource(R.string.dialog_add_item_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_product_name)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text(stringResource(R.string.label_quantity)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(stringResource(R.string.label_supermarkets), fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = anySupermarket,
                        onCheckedChange = {
                            anySupermarket = it
                            if (it) selectedSupermarkets = emptySet()
                        }
                    )
                    Text(stringResource(R.string.label_any))
                }

                if (!anySupermarket) {
                    if (supermarkets.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_supermarkets_add_first),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        supermarkets.forEach { (id, marketName) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = id in selectedSupermarkets,
                                    onCheckedChange = {
                                        selectedSupermarkets = if (it) {
                                            selectedSupermarkets + id
                                        } else {
                                            selectedSupermarkets - id
                                        }
                                    }
                                )
                                Text(marketName)
                            }
                        }
                    }
                }

                Text(stringResource(R.string.label_for_who), fontWeight = FontWeight.Bold)

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = { selectedFlatmates = flatmates.toSet() }) {
                        Text(stringResource(R.string.btn_select_all))
                    }

                    TextButton(onClick = { selectedFlatmates = emptySet() }) {
                        Text(stringResource(R.string.btn_unselect_all))
                    }
                }

                flatmates.forEach { person ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = person in selectedFlatmates,
                            onCheckedChange = {
                                selectedFlatmates = if (it) {
                                    selectedFlatmates + person
                                } else {
                                    selectedFlatmates - person
                                }
                            }
                        )
                        Text(person)
                    }
                }
            }
        }
    )
}
