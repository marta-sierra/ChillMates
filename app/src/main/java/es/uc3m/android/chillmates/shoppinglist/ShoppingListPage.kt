package es.uc3m.android.chillmates.shoppinglist

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import es.uc3m.android.chillmates.expensestracker.AddExpenseActivity
import es.uc3m.android.chillmates.LargeSectionHeader
import es.uc3m.android.chillmates.PageContentWrapper
import es.uc3m.android.chillmates.R

@Composable
fun ShoppingListPage(navController: NavController) {
    val vm: ShoppingViewModel = viewModel()
    val context = LocalContext.current
    // State for item prompted for expense addition
    var itemToPrompt by remember { mutableStateOf<ShoppingItemUi?>(null) }
    // Result launcher for AddExpenseActivity
    val expenseLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    ShoppingListRoute(
        state = vm.uiState,
        onFilter = vm::onFilterSupermarket,
        onToggleBought = { itemId ->
            vm.onToggleBought(itemId)
            val item = vm.uiState.items.find { it.id == itemId }
            if (item?.isBought == true) {
                itemToPrompt = item
            }
        },
        onDelete = vm::onDeleteItem,
        onAddItem = vm::addItem,
        onOpenMap = { }
    )

    // Add expense confirmation dialog
    if (itemToPrompt != null) {
        AlertDialog(
            onDismissRequest = { itemToPrompt = null },
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
                        putStringArrayListExtra("flatmatesList", ArrayList(vm.uiState.availableFlatmates))
                        putExtra(AddExpenseActivity.EXTRA_SPLIT_BETWEEN, ArrayList(item.neededBy))
                    }
                    expenseLauncher.launch(intent)
                    itemToPrompt = null
                }) {
                    Text(stringResource(R.string.btn_create_expense))
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToPrompt = null }) {
                    Text(stringResource(R.string.btn_not_now))
                }
            }
        )
    }
}

/**
 * Main layout for the Shopping List, including filters, the list, and action buttons.
 */
@Composable
private fun ShoppingListRoute(
    state: ShoppingUiState,
    onFilter: (String?) -> Unit,
    onToggleBought: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAddItem: (ShoppingItemUi) -> Unit,
    onOpenMap: () -> Unit
) {
    var showMap by remember { mutableStateOf(false) }
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
                    SupermarketDropdown(
                        selected = state.selectedSupermarket,
                        onFilter = onFilter,
                        supermarkets = state.availableSupermarkets,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = { onOpenMap() }) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = stringResource(R.string.cd_open_map)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
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
        // TODO: Read data from DB.
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

/**
 * Dropdown selector to filter the shopping list by specific supermarkets.
 */
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
        modifier = modifier
    ) {
        TextField(
            value = selected ?: stringResource(R.string.filter_all),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.filter_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.filter_all)) },
                onClick = {
                    onFilter(null)
                    expanded = false
                }
            )
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

/**
 * UI row representing a single shopping item with status and delete actions.
 */
@Composable
private fun ShoppingItemRow(
    item: ShoppingItemUi,
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

/**
 * Dialog for creating a new shopping item with supermarket and flatmate assignments.
 */
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
                                id = System.currentTimeMillis().toString(), name = name,
                                quantity = quantity, neededBy = selectedFlatmates.toList(),
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    supermarkets.forEach { market ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = market in selectedSupermarkets,
                                onCheckedChange = {
                                    selectedSupermarkets = if (it) selectedSupermarkets + market else selectedSupermarkets - market
                                }
                            )
                            Text(market)
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
                                selectedFlatmates = if (it) selectedFlatmates + person else selectedFlatmates - person
                            }
                        )
                        Text(person)
                    }
                }
            }
        }
    )
}
