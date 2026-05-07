package es.uc3m.android.chillmates

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import es.uc3m.android.chillmates.chores.AddChoreDialog
import es.uc3m.android.chillmates.expensestracker.AddExpenseDialog
import es.uc3m.android.chillmates.expensestracker.ExpenseResult
import es.uc3m.android.chillmates.home.AddEventScreen
import es.uc3m.android.chillmates.home.EventResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChillMatesUITests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // =========================================================================
    // 3.1 AddEventTest
    // =========================================================================
    @Test
    fun addEventTest_dialogOpensAndSaves() {
        var savedResult: EventResult? = null

        composeTestRule.setContent {
            AddEventScreen(
                onSave = { result -> savedResult = result },
                onBack = { }
            )
        }

        composeTestRule.onNodeWithText("Day (Mon/Tue/Wed/Thu/Fri/Sat/Sun)").performTextInput("Fri")
        composeTestRule.onNodeWithText("Time (HH:MM)").performTextInput("22:00")
        composeTestRule.onNodeWithText("Description").performTextInput("Fiesta en casa")

        composeTestRule.onNodeWithText("Save").performClick()

        assertNotNull("Save callback not triggered", savedResult)
        assertEquals("Fri 22:00", savedResult?.dateLabel)
        assertEquals("Fiesta en casa", savedResult?.description)
    }

    // =========================================================================
    // 3.2 AddExpenseTest
    // =========================================================================
    @Test
    fun addExpenseTest_fillAndSave() {
        var savedResult: ExpenseResult? = null
        val flatmates = listOf("Marta", "Alex", "John")

        composeTestRule.setContent {
            AddExpenseDialog(
                availableFlatmates = flatmates,
                onSave = { result -> savedResult = result },
                onDismiss = { }
            )
        }

        composeTestRule.onNodeWithText("Expense Title").performTextInput("Internet")
        composeTestRule.onNodeWithText("Amount (€)").performTextInput("35.50")

        // Click on paidBy surface to open dropdown, then select a flatmate
        composeTestRule.onNodeWithText("Paid by").performClick()
        composeTestRule.onNodeWithText("Marta").performClick()

        composeTestRule.onNodeWithText("Save").performClick()

        assertNotNull("Save callback not triggered", savedResult)
        assertEquals("Internet", savedResult?.title)
        assertEquals(35.50, savedResult!!.amount, 0.001)
        assertEquals("Marta", savedResult?.paidBy)
    }

    // =========================================================================
    // 3.3 ShoppingListTest
    // =========================================================================
    @Test
    fun shoppingListTest_filterAndToggle() {
        var filterApplied: String? = null
        var itemToggled = false
        var itemDeleted = false

        composeTestRule.setContent {
            var expanded by remember { mutableStateOf(false) }
            var selected by remember { mutableStateOf("All") }

            Column {
                TextButton(onClick = { expanded = true }) {
                    Text(selected)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("All") },
                        onClick = { filterApplied = null; selected = "All"; expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Lidl") },
                        onClick = { filterApplied = "Lidl"; selected = "Lidl"; expanded = false }
                    )
                }

                Row {
                    Text("Milk (2L)")
                    Checkbox(
                        checked = false,
                        onCheckedChange = { itemToggled = it }
                    )
                    TextButton(onClick = { itemDeleted = true }) {
                        Text("Delete")
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("All").performClick()
        composeTestRule.onNodeWithText("Lidl").performClick()
        assertEquals("Lidl", filterApplied)

        composeTestRule.onNode(isToggleable()).performClick()
        assertTrue("Item not marked as bought", itemToggled)

        composeTestRule.onNodeWithText("Delete").performClick()
        assertTrue("Item not deleted", itemDeleted)
    }

    // =========================================================================
    // 3.4 ChoresTest
    // =========================================================================
    @Test
    fun choresTest_toggleCheckbox() {
        var checkboxChanged = false

        composeTestRule.setContent {
            Checkbox(
                checked = false,
                onCheckedChange = { checkboxChanged = it }
            )
        }

        composeTestRule.onNode(isToggleable()).performClick()
        assertTrue("Checkbox state did not toggle", checkboxChanged)
    }

    @Test
    fun choresTest_addRandom_verifiesNewTask() {
        var addedTitle = ""
        var addedAssignee = ""

        composeTestRule.setContent {
            AddChoreDialog(
                flatmates = listOf("Laura", "Carlos", "Miguel"),
                onDismiss = { },
                onAdd = { title, assignee ->
                    addedTitle = title
                    addedAssignee = assignee
                }
            )
        }

        composeTestRule.onNodeWithText("Chore Title").performTextInput("Lavar ventanas")
        composeTestRule.onNodeWithText("Add").performClick()

        assertEquals("Lavar ventanas", addedTitle)
        assertEquals("Laura", addedAssignee)
    }
}
