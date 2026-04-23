package es.uc3m.android.chillmates.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.chillmates.model.Notice
import es.uc3m.android.chillmates.model.UpcomingEvent
import es.uc3m.android.chillmates.model.USERS_COLLECTION
import es.uc3m.android.chillmates.repository.FlatRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Locale

@Immutable
data class NoticeUi(
    val id: String,
    val title: String,
    val details: String,
    val acceptedBy: List<MemberUi> = emptyList()
)

@Immutable
data class MemberUi(val name: String)

@Immutable
data class UpcomingEventUi(
    val id: String,
    val dateLabel: String,
    val description: String,
    val isRemindMeEnabled: Boolean = false,
    val location: String = "",
    val attending: List<MemberUi> = emptyList()
)

@Immutable
data class HomeUiState(
    val notices: List<NoticeUi> = emptyList(),
    val upcomingEvents: List<UpcomingEventUi> = emptyList(),
    val showAllNotices: Boolean = false,
    val expandedEventId: String? = null,
    val currentUserName: String = "Me"
)

class HomeViewModel(
    private val repository: FlatRepository = FlatRepository()
) : ViewModel() {

    var uiState by mutableStateOf(HomeUiState())
        private set

    sealed class HomeAction {
        data class AddToCalendar(val title: String, val description: String, val beginMillis: Long, val endMillis: Long) : HomeAction()
        data class ScheduleReminder(val requestCode: Int, val title: String, val message: String, val triggerAtMillis: Long) : HomeAction()
        data class CancelReminder(val requestCode: Int) : HomeAction()
    }

    var action: HomeAction? by mutableStateOf(null)
        private set

    private var currentFlatId: String? = null
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val localReminderStates = mutableMapOf<String, Boolean>()

    init {
        loadUserAndFlatData()
    }

    private fun loadUserAndFlatData() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                val userDoc = db.collection(USERS_COLLECTION).document(userId).get().await()
                val flatId = userDoc.getString("flatId")
                val userName = userDoc.getString("displayName") ?: "Me"

                if (flatId != null) {
                    currentFlatId = flatId
                    uiState = uiState.copy(currentUserName = userName)

                    launch {
                        repository.observeNotices(flatId).collect { dbNotices ->
                            val mappedNotices = dbNotices.map { dbNotice ->
                                NoticeUi(dbNotice.id, dbNotice.title, dbNotice.details, dbNotice.acceptedBy.map { MemberUi(it) })
                            }
                            uiState = uiState.copy(notices = mappedNotices)
                        }
                    }

                    launch {
                        repository.observeEvents(flatId).collect { dbEvents ->
                            val mappedEvents = dbEvents.map { dbEvent ->
                                val isReminded = localReminderStates[dbEvent.id] ?: false
                                UpcomingEventUi(
                                    id = dbEvent.id,
                                    dateLabel = dbEvent.dateLabel,
                                    description = dbEvent.description,
                                    location = dbEvent.location,
                                    attending = dbEvent.attending.map { MemberUi(it) },
                                    isRemindMeEnabled = isReminded
                                )
                            }.sortedBy { computeNextOccurrenceMillis(it.dateLabel, 60) }
                            uiState = uiState.copy(upcomingEvents = mappedEvents)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onActionConsumed() { action = null }

    fun onAcceptNotice(noticeId: String) {
        val flatId = currentFlatId ?: return
        val userName = uiState.currentUserName
        val target = uiState.notices.find { it.id == noticeId } ?: return
        if (target.acceptedBy.any { it.name == userName }) return

        val newAccepted = target.acceptedBy.map { it.name } + userName
        viewModelScope.launch { repository.updateNoticeAcceptedBy(flatId, noticeId, newAccepted) }
    }

    fun onRejectNotice(noticeId: String) {
        val flatId = currentFlatId ?: return
        val userName = uiState.currentUserName
        val target = uiState.notices.find { it.id == noticeId } ?: return

        val newAccepted = target.acceptedBy.filterNot { it.name == userName }.map { it.name }
        viewModelScope.launch { repository.updateNoticeAcceptedBy(flatId, noticeId, newAccepted) }
    }

    fun onShowAllNotices() {
        uiState = uiState.copy(showAllNotices = !uiState.showAllNotices)
    }

    fun onAddNotice(title: String, details: String) {
        val flatId = currentFlatId ?: return
        val newNotice = Notice(id = "n_${System.currentTimeMillis()}", title = title, details = details, acceptedBy = emptyList())
        viewModelScope.launch { repository.addNotice(flatId, newNotice) }
    }

    fun onAddToCalendar(eventId: String) {
        val e = uiState.upcomingEvents.firstOrNull { it.id == eventId } ?: return
        val beginMillis = computeNextOccurrenceMillis(e.dateLabel, durationMinutes = 60)
        val endMillis = beginMillis + 60L * 60L * 1000L

        action = HomeAction.AddToCalendar(
            title = e.description,
            description = "${e.dateLabel} • ${e.location}",
            beginMillis = beginMillis,
            endMillis = endMillis
        )
    }

    fun onViewMoreDetails(eventId: String) {
        uiState = uiState.copy(expandedEventId = if (uiState.expandedEventId == eventId) null else eventId)
    }

    fun onToggleRemindMe(eventId: String) {
        val event = uiState.upcomingEvents.firstOrNull { it.id == eventId } ?: return
        val isNowEnabled = !event.isRemindMeEnabled
        localReminderStates[eventId] = isNowEnabled

        uiState = uiState.copy(
            upcomingEvents = uiState.upcomingEvents.map { e ->
                if (e.id == eventId) e.copy(isRemindMeEnabled = isNowEnabled) else e
            }
        )

        val requestCode = eventId.hashCode()

        if (isNowEnabled) {
            val eventStart = computeNextOccurrenceMillis(event.dateLabel, durationMinutes = 60)
            val triggerAtMillis = eventStart - 24L * 60L * 60L * 1000L
            action = HomeAction.ScheduleReminder(requestCode, "ChillMates reminder", "${event.description} • ${event.dateLabel}", triggerAtMillis)
        } else {
            action = HomeAction.CancelReminder(requestCode)
        }
    }

    fun onAddEvent(dateLabel: String, description: String, location: String) {
        val flatId = currentFlatId ?: return
        val cleanDateLabel = dateLabel.trim()
        val cleanDescription = description.trim()
        val cleanLocation = location.trim()

        if (cleanDateLabel.isBlank() || cleanDescription.isBlank()) return

        val newDbEvent = UpcomingEvent(id = "e_${System.currentTimeMillis()}", dateLabel = cleanDateLabel, description = cleanDescription, location = cleanLocation, attending = emptyList())
        val newDbNotice = Notice(id = "n_${System.currentTimeMillis()}", title = "New event added", details = "$cleanDescription • $cleanDateLabel", acceptedBy = emptyList())

        viewModelScope.launch {
            repository.addEvent(flatId, newDbEvent)
            repository.addNotice(flatId, newDbNotice)
        }
    }

    private fun computeNextOccurrenceMillis(dateLabel: String, durationMinutes: Int): Long {
        val parts = dateLabel.trim().split(" ")
        val dayPart = parts.getOrNull(0).orEmpty()
        val timePart = parts.getOrNull(1).orEmpty()

        val dayOfWeek = when (dayPart.lowercase(Locale.ROOT).take(3)) {
            "mon" -> Calendar.MONDAY
            "tue" -> Calendar.TUESDAY
            "wed" -> Calendar.WEDNESDAY
            "thu" -> Calendar.THURSDAY
            "fri" -> Calendar.FRIDAY
            "sat" -> Calendar.SATURDAY
            "sun" -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }

        val hour = timePart.substringBefore(":", "12").toIntOrNull() ?: 12
        val minute = timePart.substringAfter(":", "00").toIntOrNull() ?: 0

        val now = Calendar.getInstance()
        val cal = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }

        val currentDow = cal.get(Calendar.DAY_OF_WEEK)
        var delta = (dayOfWeek - currentDow + 7) % 7
        if (delta == 0 && cal.timeInMillis <= now.timeInMillis) delta = 7
        cal.add(Calendar.DAY_OF_YEAR, delta)

        return cal.timeInMillis
    }

    // =========================================================================
    // SUPER MOCK DATA INJECTOR (Delete before presentation!)
    // =========================================================================
    fun injectMockData() {
        val flatId = currentFlatId ?: return
        val currentUserId = auth.currentUser?.uid ?: return
        val me = uiState.currentUserName

        viewModelScope.launch {
            try {
                // 1. INYECTAR USUARIOS FALSOS (Compañeros de piso)
                val fakeUsers = listOf(
                    Pair("mock_u1", "Laura"),
                    Pair("mock_u2", "Carlos"),
                    Pair("mock_u3", "Miguel")
                )
                fakeUsers.forEach { (uid, name) ->
                    db.collection(es.uc3m.android.chillmates.model.USERS_COLLECTION).document(uid).set(
                        mapOf("displayName" to name, "flatId" to flatId, "email" to "${name.lowercase()}@mock.com", "avatarUrl" to null)
                    ).await()
                }

                val allMembers = listOf(currentUserId) + fakeUsers.map { it.first }
                db.collection(es.uc3m.android.chillmates.model.FLATS_COLLECTION).document(flatId).update("memberIds", allMembers).await()

                // 2. INYECTAR SUPERMERCADOS FALSOS
                val supers = listOf(
                    es.uc3m.android.chillmates.model.Supermarket(id = "sup_mercadona", name = "Mercadona", officialName = "Mercadona S.A.", placeId = "ChIJ-wM...", latitude = 40.41, longitude = -3.70, address = "Calle Mayor 1"),
                    es.uc3m.android.chillmates.model.Supermarket(id = "sup_carrefour", name = "Carrefour", officialName = "Carrefour Express", placeId = "ChIJ-xN...", latitude = 40.42, longitude = -3.71, address = "Gran Vía 20"),
                    es.uc3m.android.chillmates.model.Supermarket(id = "sup_lidl", name = "Lidl", officialName = "Lidl Supermercados", placeId = "ChIJ-yO...", latitude = 40.40, longitude = -3.69, address = "Atocha 50")
                )
                supers.forEach { s -> db.collection("supermarkets").document(s.id!!).set(s).await() }
                db.collection(es.uc3m.android.chillmates.model.FLATS_COLLECTION).document(flatId).update("supermarketIds", supers.map { it.id }).await()

                // 3. INYECTAR LISTA DE LA COMPRA MASIVA
                val items = listOf(
                    es.uc3m.android.chillmates.model.ShoppingItem(id = "s1", name = "Leche entera", quantity = "6 bricks", neededBy = listOf("Laura", "Carlos"), supermarkets = listOf("Mercadona"), bought = false),
                    es.uc3m.android.chillmates.model.ShoppingItem(id = "s2", name = "Huevos", quantity = "2 docenas", neededBy = listOf("Everyone"), supermarkets = listOf("Mercadona", "Lidl"), bought = false),
                    es.uc3m.android.chillmates.model.ShoppingItem(id = "s3", name = "Pan de molde", quantity = "1 paquete", neededBy = listOf(me), supermarkets = listOf("Carrefour"), bought = true),
                    es.uc3m.android.chillmates.model.ShoppingItem(id = "s4", name = "Cerveza", quantity = "24 latas", neededBy = listOf("Miguel", "Carlos"), supermarkets = listOf("Lidl", "Carrefour"), bought = false),
                    es.uc3m.android.chillmates.model.ShoppingItem(id = "s5", name = "Papel higiénico", quantity = "24 rollos", neededBy = listOf("Everyone"), supermarkets = listOf("Mercadona"), bought = false)
                )
                items.forEach { repository.addShoppingItem(flatId, it) }

                // 4. INYECTAR TAREAS REPARTIDAS
                repository.addChore(flatId, es.uc3m.android.chillmates.model.Chore(id = "c1", title = "Limpiar cocina a fondo", assignee = "Carlos", isDone = false))
                repository.addChore(flatId, es.uc3m.android.chillmates.model.Chore(id = "c2", title = "Bajar la basura y cartón", assignee = "Everyone", isDone = false))
                repository.addChore(flatId, es.uc3m.android.chillmates.model.Chore(id = "c3", title = "Aspirar el salón", assignee = "Laura", isDone = true))
                repository.addChore(flatId, es.uc3m.android.chillmates.model.Chore(id = "c4", title = "Limpiar los dos baños", assignee = me, isDone = false))

                // 5. INYECTAR GASTOS Y BALANCE DEUDAS
                repository.addExpense(flatId, es.uc3m.android.chillmates.model.Expense(id = "e1", title = "Internet Fibra", amount = 35.50, paidBy = "Laura", ticketUri = null, splitBetween = listOf(me, "Laura", "Carlos", "Miguel")))
                repository.addExpense(flatId, es.uc3m.android.chillmates.model.Expense(id = "e2", title = "Compra Semanal Fuerte", amount = 124.30, paidBy = me, ticketUri = null, splitBetween = listOf(me, "Laura", "Carlos", "Miguel")))
                repository.addExpense(flatId, es.uc3m.android.chillmates.model.Expense(id = "e3", title = "Suscripción Netflix", amount = 15.99, paidBy = "Carlos", ticketUri = null, splitBetween = listOf("Carlos", "Miguel")))

                // 6. INYECTAR REPARACIONES
                repository.addRepair(flatId, es.uc3m.android.chillmates.model.Repair(id = "r1", title = "Lavadora rota", description = "Hace un ruido muy raro al centrifugar.", reportedBy = "Laura", landlordMessage = "", photoUris = emptyList(), status = "PENDING"))
                repository.addRepair(flatId, es.uc3m.android.chillmates.model.Repair(id = "r2", title = "Calefacción no funciona", description = "El radiador de mi cuarto está completamente frío.", reportedBy = "Miguel", landlordMessage = "El técnico irá mañana por la tarde.", photoUris = emptyList(), status = "ACCEPTED"))

                // 7. INYECTAR EVENTOS Y AVISOS
                repository.addEvent(flatId, es.uc3m.android.chillmates.model.UpcomingEvent(id = "ev1", dateLabel = "Fri 21:00", description = "Cena de piso \uD83C\uDF89", location = "Salón", attending = listOf("Laura", "Carlos", me)))
                repository.addNotice(flatId, es.uc3m.android.chillmates.model.Notice(id = "n1", title = "Visita Casero", details = "Viene a ver la caldera el lunes.", acceptedBy = listOf(me)))

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}