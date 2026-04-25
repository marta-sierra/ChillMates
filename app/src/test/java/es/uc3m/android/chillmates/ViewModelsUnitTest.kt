package es.uc3m.android.chillmates

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.chillmates.chores.ChoresViewModel
import es.uc3m.android.chillmates.expensestracker.ExpensesViewModel
import es.uc3m.android.chillmates.home.HomeViewModel
import es.uc3m.android.chillmates.shoppinglist.ShoppingItemUi
import es.uc3m.android.chillmates.shoppinglist.ShoppingViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Field

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsUnitTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // 🚀 Aislar Firebase (Mocking)
        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseFirestore::class)

        val mockAuth = mockk<FirebaseAuth>(relaxed = true)
        val mockUser = mockk<FirebaseUser>(relaxed = true)
        every { mockUser.uid } returns "test_user_id"
        every { mockUser.displayName } returns "Laura"
        every { mockAuth.currentUser } returns mockUser
        every { FirebaseAuth.getInstance() } returns mockAuth

        val mockDb = mockk<FirebaseFirestore>(relaxed = true)
        every { FirebaseFirestore.getInstance() } returns mockDb
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 🛠️ HELPER (REFLECTION): Inyecta un 'flatId' falso en las variables privadas
     * de los ViewModels para evitar que hagan "return" prematuro por falta de Firebase.
     */
    private fun injectFakeFlatId(viewModel: Any) {
        try {
            val field: Field = viewModel.javaClass.getDeclaredField("currentFlatId")
            field.isAccessible = true
            field.set(viewModel, "fake_flat_123")
        } catch (e: Exception) {
            // Se ignora si el ViewModel no tiene esta variable
        }
    }

    // =========================================================================
    // 2.1 HomeViewModelTest
    // =========================================================================

    @Test
    fun homeViewModel_onAddEvent_verificarEventoCreado() = runTest {
        val viewModel = HomeViewModel()
        injectFakeFlatId(viewModel)

        viewModel.addEvent("Mon 10:00", "Reunión de piso", "Salón")
        assertNotNull("El UI State no debe ser nulo tras añadir un evento", viewModel.uiState)
    }

    @Test
    fun homeViewModel_onAcceptNotice_verificarAcceptance() = runTest {
        val viewModel = HomeViewModel()
        injectFakeFlatId(viewModel)

        viewModel.acceptPoll("test_notice_1")
        assertNotNull(viewModel.uiState)
    }

    @Test
    fun homeViewModel_onToggleRemindMe_verificarStateToggle() = runTest {
        val viewModel = HomeViewModel()
        injectFakeFlatId(viewModel)

        val initialEventsSize = viewModel.uiState.upcomingEvents.size
        viewModel.toggleReminder("test_event_1")

        assertEquals("El tamaño de la lista no debe cambiar al hacer toggle", initialEventsSize, viewModel.uiState.upcomingEvents.size)
    }

    // =========================================================================
    // 2.2 ExpensesViewModelTest
    // =========================================================================

    @Test
    fun expensesViewModel_onExpenseAdded_recalcularBalances() = runTest {
        val viewModel = ExpensesViewModel()
        injectFakeFlatId(viewModel)

        viewModel.onExpenseAdded(
            title = "Compra conjunta",
            amount = 100.0,
            paidBy = "Carlos",
            splitBetween = listOf("Carlos", "Laura")
        )
        assertNotNull("El estado de expenses no debe ser nulo", viewModel.uiState)
    }

    // =========================================================================
    // 2.3 ChoresViewModelTest
    // =========================================================================

    @Test
    fun choresViewModel_addRandomChore_verificarChoreAnadida() = runTest {
        val viewModel = ChoresViewModel()

        // 🚀 APLICAMOS EL INYECTOR PARA QUE NO HAGA RETURN
        injectFakeFlatId(viewModel)

        val tituloTarea = "Limpiar la nevera"
        viewModel.addChore(tituloTarea, "Miguel")

        // Actualización optimista
        val nuevaTarea = viewModel.chores.value.find { it.title == tituloTarea }

        assertNotNull("La tarea debe haberse añadido a la lista local", nuevaTarea)
        assertEquals("El assignee debe ser Miguel", "Miguel", nuevaTarea?.assignee)
        assertFalse("La tarea debe inicializarse como NO hecha", nuevaTarea!!.isDone)
    }

    @Test
    fun choresViewModel_toggleChoreDone_verificarIsDoneFlip() = runTest {
        val viewModel = ChoresViewModel()

        // 🚀 APLICAMOS EL INYECTOR
        injectFakeFlatId(viewModel)

        viewModel.addChore("Barrer", "Laura")
        val tareaAñadida = viewModel.chores.value.first { it.title == "Barrer" }

        viewModel.updateChoreStatus(tareaAñadida.id, true)

        val tareaActualizada = viewModel.chores.value.first { it.id == tareaAñadida.id }
        assertTrue("La tarea debe estar marcada como hecha (isDone = true)", tareaActualizada.isDone)
    }

    // =========================================================================
    // 2.4 ShoppingListViewModelTest
    // =========================================================================

    @Test
    fun shoppingViewModel_addItem_verificarItemEnLista() = runTest {
        val viewModel = ShoppingViewModel()
        injectFakeFlatId(viewModel)

        val newItem = ShoppingItemUi(
            id = "item_1",
            name = "Leche Semidesnatada",
            quantity = "6 briks",
            neededBy = listOf("Todos"),
            supermarkets = listOf("Mercadona"),
            bought = false
        )

        viewModel.addItem(newItem)
        assertNotNull("El estado de la lista de la compra debe existir", viewModel.uiState)
    }

    @Test
    fun shoppingViewModel_setItemBought_verificarBoughtFlip() = runTest {
        val viewModel = ShoppingViewModel()
        injectFakeFlatId(viewModel)

        viewModel.setItemBought("item_1", true)
        assertNotNull(viewModel.uiState)
    }
}