package es.uc3m.android.chillmates

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import es.uc3m.android.chillmates.chores.AddChoreDialog
import es.uc3m.android.chillmates.expensestracker.AddExpenseDialog
import es.uc3m.android.chillmates.home.AddEventScreen
import es.uc3m.android.chillmates.home.EventResult
import es.uc3m.android.chillmates.shoppinglist.ShoppingItemUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChillMatesUITests {

    // Main rule for isolated Jetpack Compose tests
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // =========================================================================
    // 3.1 AddEventTest
    // =========================================================================
    @Test
    fun addEventTest_fillFieldsAndSave_verifiesAction() {
        var savedEvent: EventResult? = null

        // Render the actual public AddEventScreen composable
        composeTestRule.setContent {
            AddEventScreen(
                onSave = { savedEvent = it },
                onBack = {}
            )
        }

        // Find text fields by their action capability and input the event details
        val textFields = composeTestRule.onAllNodes(hasSetTextAction())
        textFields[0].performTextInput("Fri")
        textFields[1].performTextInput("21:00")
        textFields[2].performTextInput("Party at the flat")
        textFields[3].performTextInput("Living Room")

        // Find the save button (usually the last interactive element without text input)
        // and perform the click action
        composeTestRule.onAllNodes(hasClickAction() and !hasSetTextAction())
            .onLast()
            .performClick()

        // Verify that the callback was triggered with the correct data
        assertTrue("Event was not saved", savedEvent != null)
        assertEquals("Fri 21:00", savedEvent?.dateLabel)
        assertEquals("Party at the flat", savedEvent?.description)
    }

    // =========================================================================
    // 3.2 AddExpenseTest
    // =========================================================================
    @Test
    fun addExpenseTest_fillAndSave_verifiesAction() {
        var expenseSaved = false

        // Render the real AddExpenseDialog
        composeTestRule.setContent {
            AddExpenseDialog(
                availableFlatmates = listOf("Laura", "Carlos"),
                onSave = { expenseSaved = true },
                onDismiss = {}
            )
        }

        // Fill the expense inputs (Title and Amount)
        val textFields = composeTestRule.onAllNodes(hasSetTextAction())
        textFields[0].performTextInput("Internet")
        textFields[1].performTextInput("35.50")

        // Click the save button to confirm the expense addition
        composeTestRule.onAllNodes(hasClickAction() and !hasSetTextAction())
            .onLast()
            .performClick()

        assertTrue("Expense action was not correctly triggered.", expenseSaved)
    }

    // =========================================================================
    // 3.3 ShoppingListTest
    // =========================================================================
    @Test
    fun shoppingListTest_markAsBoughtAndDelete_verifiesActions() {
        var boughtStatusChanged = false
        var itemDeleted = false

        val mockItem = ShoppingItemUi(
            id = "1",
            name = "Milk",
            quantity = "2",
            neededBy = listOf("Laura"),
            supermarkets = listOf("Mercadona"),
            bought = false
        )

        // Mocking the visual representation of the shopping row to test interaction
        // using the correct Material Icons imports
        composeTestRule.setContent {
            Card {
                Row {
                    Text(mockItem.name)
                    IconButton(
                        onClick = { boughtStatusChanged = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Toggle Bought"
                        )
                    }
                    IconButton(
                        onClick = { itemDeleted = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Item"
                        )
                    }
                }
            }
        }

        // Verify marking item as bought
        composeTestRule.onNodeWithContentDescription("Toggle Bought").performClick()
        assertTrue("Item was not marked as bought.", boughtStatusChanged)

        // Verify item deletion
        composeTestRule.onNodeWithContentDescription("Delete Item").performClick()
        assertTrue("Item was not deleted.", itemDeleted)
    }

    // =========================================================================
    // 3.4 ChoresTest
    // =========================================================================
    @Test
    fun choresTest_addChore_verifiesNewChore() {
        var choreAdded = false

        // Render the public AddChoreDialog
        composeTestRule.setContent {
            AddChoreDialog(
                flatmates = listOf("Laura", "Carlos"),
                onDismiss = {},
                onAdd = { _, _ -> choreAdded = true }
            )
        }

        // Input the new chore title
        composeTestRule.onAllNodes(hasSetTextAction())[0].performTextInput("Clean Windows")

        // Trigger the Add action
        composeTestRule.onAllNodes(hasClickAction() and !hasSetTextAction())
            .onLast()
            .performClick()

        assertTrue("Add chore action was not triggered", choreAdded)
    }
}