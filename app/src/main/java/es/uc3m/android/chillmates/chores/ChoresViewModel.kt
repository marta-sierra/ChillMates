package es.uc3m.android.chillmates.chores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.chillmates.model.Chore
import es.uc3m.android.chillmates.model.ChoreAssignment
import es.uc3m.android.chillmates.model.USERS_COLLECTION
import es.uc3m.android.chillmates.repository.FlatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class ChoresViewModel(
    private val repository: FlatRepository = FlatRepository()
) : ViewModel() {

    private val _chores = MutableStateFlow<List<Chore>>(emptyList())
    val chores: StateFlow<List<Chore>> = _chores

    private val _flatmates = MutableStateFlow<List<String>>(listOf("Everyone"))
    val flatmates: StateFlow<List<String>> = _flatmates

    private val _currentUserName = MutableStateFlow<String>("Me")
    val currentUserName: StateFlow<String> = _currentUserName

    private var currentFlatId: String? = null
    private var lastRotationChecked = false

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    init {
        loadUserAndFlatData()
    }

    private fun loadUserAndFlatData() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch

            try {
                val userDoc = db.collection(USERS_COLLECTION).document(userId).get().await()
                val flatId = userDoc.getString("flatId") ?: return@launch
                val userName = userDoc.getString("displayName") ?: "Me"

                _currentUserName.value = userName
                currentFlatId = flatId

                launch {
                    repository.observeFlat(flatId).collect { flat ->
                        if (flat != null) {
                            val membersResult = repository.getMemberDetails(flat.memberIds)
                            if (membersResult.isSuccess) {
                                val memberNames = membersResult.getOrNull()?.map { it.second } ?: emptyList()
                                _flatmates.value = listOf("Everyone") + memberNames
                            }

                            if (!lastRotationChecked) {
                                lastRotationChecked = true
                                checkAutoRotation(flatId, flat.lastRotationTimestamp, flat.memberIds)
                            }
                        }
                    }
                }

                launch {
                    repository.observeChores(flatId).collect { dbChores ->
                        _chores.value = dbChores
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkAutoRotation(flatId: String, lastRotationTimestamp: Long, memberIds: List<String>) {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            val oneWeekInMillis = 7L * 24 * 60 * 60 * 1000 // 7 days

            if (lastRotationTimestamp == 0L) {
                repository.updateFlatRotationTimestamp(flatId, currentTime)
            } else if (currentTime - lastRotationTimestamp >= oneWeekInMillis) {
                val currentChoresResult = repository.getChoresOnce(flatId)
                if (currentChoresResult.isSuccess) {
                    val currentChores = currentChoresResult.getOrNull() ?: emptyList()
                    performAutoRotation(currentChores, memberIds, flatId, currentTime)
                }
            }
        }
    }

    fun updateChoreStatus(choreId: String, isDone: Boolean) {
        val flatId = currentFlatId ?: return
        val weekKey = currentWeekKey()
        val currentChore = _chores.value.firstOrNull { it.id == choreId } ?: return
        val assignee = currentChore.assignments[weekKey]?.assignee ?: currentChore.assignee

        _chores.value = _chores.value.map { chore ->
            if (chore.id == choreId) {
                val assignment = chore.assignments[weekKey]
                    ?: ChoreAssignment(assignee = chore.assignee, isDone = chore.isDone)
                chore.copy(
                    isDone = isDone,
                    assignments = chore.assignments + (weekKey to assignment.copy(isDone = isDone))
                )
            } else chore
        }

        viewModelScope.launch {
            try {
                repository.updateChoreStatus(flatId, choreId, weekKey, isDone, assignee)
            } catch (e: Exception) {
                repository.getChoresOnce(flatId).getOrNull()?.let { refreshed ->
                    _chores.value = refreshed
                }
            }
        }
    }

     fun addChore(title: String, assignee: String) {
         val flatId = currentFlatId ?: return
        val weekKey = currentWeekKey()
         val newChore = Chore(
             id = System.currentTimeMillis().toString(),
             title = title,
             assignee = assignee,
             isDone = false,
             assignments = mapOf(weekKey to ChoreAssignment(assignee = assignee, isDone = false))
         )

         _chores.value = _chores.value + newChore

        viewModelScope.launch {
            repository.addChore(flatId, newChore)
        }
    }

    fun swapChore(choreId: String, newAssignee: String) {
        val flatId = currentFlatId ?: return
        val weekKey = currentWeekKey()
        val currentChore = _chores.value.firstOrNull { it.id == choreId } ?: return
        val currentAssignment = currentChore.assignments[weekKey]
            ?: ChoreAssignment(assignee = currentChore.assignee, isDone = currentChore.isDone)
        val updatedAssignment = currentAssignment.copy(assignee = newAssignee)

         _chores.value = _chores.value.map {
            if (it.id == choreId) {
                it.copy(
                    assignee = newAssignee,
                    assignments = it.assignments + (weekKey to updatedAssignment)
                )
            } else {
                it
            }
         }

         viewModelScope.launch {
            repository.updateChoreAssignee(
                flatId = flatId,
                choreId = choreId,
                weekKey = weekKey,
                newAssignee = newAssignee,
                isDone = updatedAssignment.isDone
            )
         }
     }

    fun saveReconfiguredChores(updatedList: List<Chore>) {
        val flatId = currentFlatId ?: return

        _chores.value = updatedList

        viewModelScope.launch {
            repository.saveChoresBatch(flatId, updatedList)
        }
    }

    private fun performAutoRotation(currentChores: List<Chore>, memberIds: List<String>, flatId: String, currentTime: Long) {
        if (currentChores.isEmpty() || memberIds.isEmpty()) return

        viewModelScope.launch {
            val membersResult = repository.getMemberDetails(memberIds)
            val mates = membersResult.getOrNull()?.map { it.second } ?: return@launch
            if (mates.isEmpty()) return@launch
            val weekKey = weekKeyForTimestamp(currentTime)

            val updatedList = currentChores.map { chore ->
                if (chore.assignee == "Everyone") {
                    val assignment = ChoreAssignment(assignee = chore.assignee, isDone = false)
                    chore.copy(
                        isDone = false,
                        assignments = chore.assignments + (weekKey to assignment)
                    )
                } else {
                    val currentIndex = mates.indexOf(chore.assignee)
                    val nextAssignee = if (currentIndex != -1 && mates.size > 1) {
                        mates[(currentIndex + 1) % mates.size]
                    } else if (currentIndex == -1 && mates.isNotEmpty()) {
                        mates[0]
                    } else {
                        chore.assignee
                    }
                    val assignment = ChoreAssignment(assignee = nextAssignee, isDone = false)
                    chore.copy(
                        assignee = nextAssignee,
                        isDone = false,
                        assignments = chore.assignments + (weekKey to assignment)
                    )
                }
            }

            _chores.value = updatedList
            repository.performAutoRotationBatch(flatId, updatedList, currentTime)
        }
    }

    private fun currentWeekKey(): String {
        return weekKeyForTimestamp(System.currentTimeMillis())
    }

    private fun weekKeyForTimestamp(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
        }
        val week = calendar.get(Calendar.WEEK_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)
        return String.format("%04d-W%02d", year, week)
    }
}