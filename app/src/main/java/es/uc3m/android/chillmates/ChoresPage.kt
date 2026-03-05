package es.uc3m.android.chillmates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ChoresPage(navController: NavController) {
    val chores = remember {
        mutableStateListOf(
            Chore(1, "Clean the bathroom", "Marta", false),
            Chore(2, "Take out the trash", "Me", false),
            Chore(3, "Wash the dishes", "Lorena", true),
            Chore(4, "Sweep the living room", "Everyone", false)
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ChoresHeaderSection(title = "Chores")

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "To make living together easier.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(chores) { chore ->
                ChoreCard(
                    chore = chore,
                    onUpdateChore = { updatedChore ->
                        val index = chores.indexOfFirst { it.id == updatedChore.id }
                        if (index != -1) {
                            chores[index] = updatedChore
                        }
                    }
                )
            }
        }

        // --- SECCIÓN DE BOTONES INFERIORES ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { /* Acción para reconfigurar tareas */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reconfigure",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Reconfigure Chores")
                }

                Button(
                    onClick = { /* Acción para añadir nueva tarea */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Chore",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Add new chore")
                }
            }
        }
    }
}

@Composable
private fun ChoresHeaderSection(title: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp, bottom = 16.dp) // Ajustado para que respire bien sin la flecha
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
fun ChoreCard(chore: Chore, onUpdateChore: (Chore) -> Unit) {
    val isMe = chore.assignee == "Me" || chore.assignee == "Everyone"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chore.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (chore.isDone) TextDecoration.LineThrough else null
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Assigned to: ${chore.assignee}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (isMe) {
                Checkbox(
                    checked = chore.isDone,
                    onCheckedChange = { onUpdateChore(chore.copy(isDone = it)) }
                )
            } else {
                if (chore.isDone) {
                    TextButton(
                        onClick = { onUpdateChore(chore.copy(isDone = false)) }
                    ) {
                        Text("Redo", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    TextButton(
                        onClick = { /* Lógica para proponer el cambio de tarea */ }
                    ) {
                        Text("Swap")
                    }
                }
            }
        }
    }
}

data class Chore(val id: Int, val title: String, val assignee: String, val isDone: Boolean)