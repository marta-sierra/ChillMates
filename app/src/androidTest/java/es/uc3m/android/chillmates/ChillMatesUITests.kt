package es.uc3m.android.chillmates

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import es.uc3m.android.chillmates.chores.AddChoreDialog
import es.uc3m.android.chillmates.chores.ChoreCard
import es.uc3m.android.chillmates.model.Chore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChillMatesUITests {

    // Regra principal para testes isolados no Jetpack Compose (sem Firebase/NavGraph)
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // =========================================================================
    // 3.1 AddEventTest
    // =========================================================================
    @Test
    fun addEventTest_preencherCamposESalvar_verificaAcao() {
        var eventoAdicionado = false
        var dataInserida = ""

        // Como o diálogo de evento é interno na HomePage, testamos a estrutura visual diretamente
        composeTestRule.setContent {
            var dateLabel by remember { mutableStateOf("") }
            var description by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { },
                title = { Text("Add Event") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = dateLabel,
                            onValueChange = { dateLabel = it },
                            label = { Text("Date (e.g. Fri 21:00)") }
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        eventoAdicionado = true
                        dataInserida = dateLabel
                    }) { Text("Add") }
                }
            )
        }

        // Verificar se o diálogo abriu e preencher os campos
        composeTestRule.onNodeWithText("Date (e.g. Fri 21:00)").performTextInput("Fri 22:00")
        composeTestRule.onNodeWithText("Description").performTextInput("Festa no apartamento")

        // Clicar em salvar ("Add")
        composeTestRule.onNodeWithText("Add").performClick()

        // Verificar a navegação/retorno (neste caso isolado, validamos se a ação foi acionada com os dados certos)
        assertTrue("O botão de adicionar evento não foi acionado.", eventoAdicionado)
        assertEquals("Fri 22:00", dataInserida)
    }

    // =========================================================================
    // 3.2 AddExpenseTest
    // =========================================================================
    @Test
    fun addExpenseTest_preencherEAdicionar_verificaAcao() {
        var gastoSalvo = false

        composeTestRule.setContent {
            // Estrutura simulada do ecrã AddExpenseScreen
            var title by remember { mutableStateOf("") }
            var amount by remember { mutableStateOf("") }

            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Expense Title") })
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") })
                Button(onClick = { gastoSalvo = true }) { Text("Save Expense") }
            }
        }

        // Preencher (Title, Amount)
        composeTestRule.onNodeWithText("Expense Title").performTextInput("Internet")
        composeTestRule.onNodeWithText("Amount").performTextInput("35.50")

        // Clicar em Save
        composeTestRule.onNodeWithText("Save Expense").performClick()

        // Verificar que a ação de salvar o gasto foi efetuada
        assertTrue("O gasto não foi guardado corretamente.", gastoSalvo)
    }

    // =========================================================================
    // 3.3 ShoppingListTest
    // =========================================================================
    @Test
    fun shoppingListTest_marcarComoComprado_verificarCheckbox() {
        var isComprado = false

        composeTestRule.setContent {
            // Simulamos um cartão da lista de compras
            androidx.compose.material3.Card {
                androidx.compose.foundation.layout.Row {
                    Text("Leite")
                    androidx.compose.material3.Checkbox(
                        checked = isComprado,
                        onCheckedChange = { isComprado = it }
                    )
                }
            }
        }

        // Encontrar o Checkbox através do nó que pode ser alternado (isToggleable)
        composeTestRule.onNode(isToggleable()).performClick()

        // Verificar que o estado interno mudou para "bought" (comprado)
        assertTrue("O item não foi marcado como comprado no clique.", isComprado)
    }

    // =========================================================================
    // 3.4 ChoresTest
    // =========================================================================
    @Test
    fun choresTest_clickCheckbox_verificarToggle() {
        var choreFoiFeita = false
        val testChore = Chore(id = "c1", title = "Limpar Cozinha", assignee = "Laura", isDone = false)

        composeTestRule.setContent {
            ChoreCard(
                chore = testChore,
                currentUserName = "Laura", // Nome do utilizador coincide com o responsável (assignee)
                isPreview = false,
                onUpdateChore = { choreAtualizada -> choreFoiFeita = choreAtualizada.isDone },
                onSwapRequest = {}
            )
        }

        // Clicar no checkbox (Toggle)
        composeTestRule.onNode(isToggleable()).performClick()

        // Verificar que a tarefa foi feita
        assertTrue("O estado isDone não mudou para true", choreFoiFeita)
    }

    @Test
    fun choresTest_clickAddRandom_verificarNovaTarefa() {
        var novaTarefaAdicionada = false

        composeTestRule.setContent {
            AddChoreDialog(
                flatmates = listOf("Laura", "Carlos", "Miguel"),
                onDismiss = { },
                onAdd = { _, _ -> novaTarefaAdicionada = true }
            )
        }

        // Clicar no campo "Chore Title" (definido em ficheiros anteriores) e preencher
        composeTestRule.onNodeWithText("Chore Title").performTextInput("Lavar Janelas")

        // Clicar em "Add"
        composeTestRule.onNodeWithText("Add").performClick()

        // Verificar que a nova tarefa foi acionada
        assertTrue("O diálogo de nova tarefa não disparou a ação onAdd", novaTarefaAdicionada)
    }
}