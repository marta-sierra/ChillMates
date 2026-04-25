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
    private var lastRotationChecked = false // Prevents infinite rotation loops

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

                // 1. Observe Flat (for members and auto-rotation timestamp)
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

                // 2. Observe Chores Subcollection
                launch {
                    repository.observeChores(flatId).collect { choresList ->
                        _chores.value = choresList
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

        // ACTUALIZACIÓN OPTIMISTA: Cambia al instante en la UI
        _chores.value = _chores.value.map {
            if (it.id == choreId) it.copy(isDone = isDone) else it
        }

        viewModelScope.launch {
            repository.updateChoreStatus(flatId, choreId, isDone)
        }
    }

    fun addChore(title: String, assignee: String) {
        val flatId = currentFlatId ?: return
        val newChore = Chore(
            id = System.currentTimeMillis().toString(),
            title = title,
            assignee = assignee,
            isDone = false
        )

        // ACTUALIZACIÓN OPTIMISTA
        _chores.value = _chores.value + newChore

        viewModelScope.launch {
            repository.addChore(flatId, newChore)
        }
    }

    fun swapChore(choreId: String, newAssignee: String) {
        val flatId = currentFlatId ?: return

        // ACTUALIZACIÓN OPTIMISTA
        _chores.value = _chores.value.map {
            if (it.id == choreId) it.copy(assignee = newAssignee) else it
        }

        viewModelScope.launch {
            repository.updateChoreAssignee(flatId, choreId, newAssignee)
        }
    }

    fun saveReconfiguredChores(updatedList: List<Chore>) {
        val flatId = currentFlatId ?: return

        // ACTUALIZACIÓN OPTIMISTA
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

            val updatedList = currentChores.map { chore ->
                if (chore.assignee == "Everyone") {
                    chore.copy(isDone = false)
                } else {
                    val currentIndex = mates.indexOf(chore.assignee)
                    val nextAssignee = if (currentIndex != -1 && mates.size > 1) {
                        mates[(currentIndex + 1) % mates.size]
                    } else if (currentIndex == -1 && mates.isNotEmpty()) {
                        mates[0]
                    } else {
                        chore.assignee
                    }
                    chore.copy(assignee = nextAssignee, isDone = false)
                }
            }

            // ACTUALIZACIÓN OPTIMISTA
            _chores.value = updatedList
            repository.performAutoRotationBatch(flatId, updatedList, currentTime)
        }
    }
}