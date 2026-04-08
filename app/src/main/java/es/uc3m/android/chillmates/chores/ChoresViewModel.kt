package es.uc3m.android.chillmates.chores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.chillmates.model.Chore
import es.uc3m.android.chillmates.model.USERS_COLLECTION
import es.uc3m.android.chillmates.repository.FlatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
                val flatId = userDoc.getString("flatId")
                val userName = userDoc.getString("displayName") ?: "Me"
                _currentUserName.value = userName

                if (flatId != null) {
                    currentFlatId = flatId

                    repository.observeFlat(flatId).collect { flat ->
                        if (flat != null) {

                            // --- AUTO-ROTATION LOGIC ---
                            val currentTime = System.currentTimeMillis()
                            val oneWeekInMillis = 7L * 24 * 60 * 60 * 1000 // 7 days in milliseconds

                            if (flat.lastRotationTimestamp == 0L) {
                                // First time initializing the flat: just set the timer to now
                                repository.updateChores(flatId, flat.chores, currentTime)
                                _chores.value = flat.chores
                            } else if (currentTime - flat.lastRotationTimestamp >= oneWeekInMillis) {
                                // A WEEK HAS PASSED! AUTO-ROTATE!
                                performAutoRotation(flat.chores, flat.memberIds, flatId, currentTime)
                            } else {
                                // Less than a week has passed, load normally
                                _chores.value = flat.chores
                            }
                            // ---------------------------

                            val membersResult = repository.getMemberDetails(flat.memberIds)
                            if (membersResult.isSuccess) {
                                val memberNames = membersResult.getOrNull()?.map { it.second } ?: emptyList()
                                _flatmates.value = listOf("Everyone") + memberNames
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateChoreStatus(choreId: String, isDone: Boolean) {
        val flatId = currentFlatId ?: return
        val updatedList = _chores.value.map {
            if (it.id == choreId) it.copy(isDone = isDone) else it
        }

        // OPTIMISTIC UPDATE: Instant UI feedback before Firebase confirms
        _chores.value = updatedList

        viewModelScope.launch {
            repository.updateChores(flatId, updatedList)
        }
    }

    fun addChore(title: String, assignee: String) {
        val flatId = currentFlatId ?: return
        val newId = System.currentTimeMillis().toString()
        val newChore = Chore(
            id = newId,
            title = title,
            assignee = assignee,
            isDone = false
        )

        val updatedList = _chores.value + newChore

        // OPTIMISTIC UPDATE
        _chores.value = updatedList

        viewModelScope.launch {
            repository.updateChores(flatId, updatedList)
        }
    }

    fun swapChore(choreId: String, newAssignee: String) {
        val flatId = currentFlatId ?: return
        val updatedList = _chores.value.map {
            if (it.id == choreId) it.copy(assignee = newAssignee) else it
        }

        // OPTIMISTIC UPDATE
        _chores.value = updatedList

        viewModelScope.launch {
            repository.updateChores(flatId, updatedList)
        }
    }

    fun saveReconfiguredChores(updatedList: List<Chore>) {
        val flatId = currentFlatId ?: return

        // OPTIMISTIC UPDATE
        _chores.value = updatedList

        viewModelScope.launch {
            repository.updateChores(flatId, updatedList)
        }
    }

    // --- AUTO ROTATION HELPER FUNCTION ---
    private fun performAutoRotation(currentChores: List<Chore>, memberIds: List<String>, flatId: String, currentTime: Long) {
        if (currentChores.isEmpty() || memberIds.isEmpty()) return

        // We use memberIds from the DB to ensure we rotate even if names haven't loaded yet
        viewModelScope.launch {
            val membersResult = repository.getMemberDetails(memberIds)
            val mates = membersResult.getOrNull()?.map { it.second } ?: return@launch
            if (mates.isEmpty()) return@launch

            val updatedList = currentChores.map { chore ->
                if (chore.assignee == "Everyone") {
                    // If it's a shared chore, just reset the completion status
                    chore.copy(isDone = false)
                } else {
                    val currentIndex = mates.indexOf(chore.assignee)

                    val nextAssignee = if (currentIndex != -1 && mates.size > 1) {
                        // Assign to the next person in the list
                        mates[(currentIndex + 1) % mates.size]
                    } else if (currentIndex == -1 && mates.isNotEmpty()) {
                        // Fallback if assignee left the flat
                        mates[0]
                    } else {
                        chore.assignee
                    }

                    chore.copy(assignee = nextAssignee, isDone = false)
                }
            }

            // OPTIMISTIC UPDATE
            _chores.value = updatedList

            // Save the rotated chores AND reset the 7-day timer
            repository.updateChores(flatId, updatedList, currentTime)
        }
    }
}