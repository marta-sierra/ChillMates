package es.uc3m.android.chillmates.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import es.uc3m.android.chillmates.model.Chore
import es.uc3m.android.chillmates.model.Expense
import es.uc3m.android.chillmates.model.Flat
import es.uc3m.android.chillmates.model.Notice
import es.uc3m.android.chillmates.model.UpcomingEvent
import es.uc3m.android.chillmates.model.FLATS_COLLECTION
import es.uc3m.android.chillmates.model.USERS_COLLECTION
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class FlatRepository {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    suspend fun createFlat(name: String, creatorId: String): Result<Flat> {
        return try {
            val inviteCode = generateInviteCode()
            val flat = Flat(
                name = name,
                inviteCode = inviteCode,
                memberIds = listOf(creatorId)
            )
            val docRef = firestore.collection(FLATS_COLLECTION).document()
            flat.id = docRef.id

            docRef.set(flat).await()

            firestore.collection(USERS_COLLECTION).document(creatorId)
                .update(
                    mapOf(
                        "flatId" to docRef.id,
                        "flatName" to name
                    )
                ).await()

            Result.success(flat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFlatByCode(inviteCode: String): Result<Flat> {
        return try {
            val normalizedCode = inviteCode.uppercase()
            val snapshot = firestore.collection(FLATS_COLLECTION)
                .whereEqualTo("inviteCode", normalizedCode)
                .get()
                .await()

            if (snapshot.isEmpty) {
                return Result.failure(Exception("Flat not found with code: $normalizedCode"))
            }

            val doc = snapshot.documents.first()
            val flat = doc.toObject(Flat::class.java)?.apply { id = doc.id }
                ?: return Result.failure(Exception("Failed to parse flat data"))

            Result.success(flat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinFlat(inviteCode: String, userId: String): Result<Flat> {
        return try {
            val normalizedCode = inviteCode.uppercase()
            val flatResult = getFlatByCode(normalizedCode)
            if (flatResult.isFailure) {
                return flatResult
            }

            val flat = flatResult.getOrThrow()
            if (flat.memberIds.contains(userId)) {
                return Result.failure(Exception("You are already a member of this flat"))
            }

            firestore.collection(FLATS_COLLECTION).document(flat.id!!)
                .update("memberIds", FieldValue.arrayUnion(userId)).await()

            firestore.collection(USERS_COLLECTION).document(userId)
                .update(
                    mapOf(
                        "flatId" to flat.id,
                        "flatName" to flat.name
                    )
                ).await()

            val updatedFlat = flat.copy(memberIds = flat.memberIds + userId)
            Result.success(updatedFlat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFlat(flatId: String): Result<Flat> {
        return try {
            val doc = firestore.collection(FLATS_COLLECTION).document(flatId).get().await()
            val flat = doc.toObject(Flat::class.java)?.apply { id = doc.id }
                ?: return Result.failure(Exception("Flat not found"))
            Result.success(flat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveFlat(flatId: String, userId: String): Result<Unit> {
        return try {
            val flatResult = getFlat(flatId)
            if (flatResult.isFailure) {
                return Result.failure(flatResult.exceptionOrNull()!!)
            }

            val flat = flatResult.getOrThrow()

            firestore.collection(FLATS_COLLECTION).document(flatId)
                .update("memberIds", FieldValue.arrayRemove(userId)).await()

            firestore.collection(USERS_COLLECTION).document(userId)
                .update("flatId", null).await()

            if (flat.memberIds.size == 1) {
                firestore.collection(FLATS_COLLECTION).document(flatId).delete().await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeFlat(flatId: String): Flow<Flat?> = callbackFlow {
        val listener = firestore.collection(FLATS_COLLECTION).document(flatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val flat = snapshot.toObject(Flat::class.java)?.apply { id = snapshot.id }
                    trySend(flat)
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateFlatName(flatId: String, newName: String): Result<Unit> {
        return try {
            firestore.collection(FLATS_COLLECTION).document(flatId)
                .update("name", newName).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMemberDetails(memberIds: List<String>): Result<List<Pair<String, String>>> {
        return try {
            val members = memberIds.mapNotNull { userId ->
                try {
                    // Avoid explicit casting by referencing your model properly
                    val doc = firestore.collection(USERS_COLLECTION).document(userId).get().await()
                    val displayName = doc.getString("displayName") ?: "Unknown"
                    userId to displayName
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(members)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // FEATURE UPDATES (CHORES, EXPENSES, HOME)
    // =========================================================================

    // Update Chores (includes the optional rotation timestamp for auto-rotation)
    suspend fun updateChores(flatId: String, chores: List<Chore>, newRotationTimestamp: Long? = null): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>("chores" to chores)

            // If a timestamp is provided, update it in Firebase too
            if (newRotationTimestamp != null) {
                updates["lastRotationTimestamp"] = newRotationTimestamp
            }

            firestore.collection(FLATS_COLLECTION).document(flatId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update Expenses
    suspend fun updateExpenses(flatId: String, expenses: List<Expense>): Result<Unit> {
        return try {
            firestore.collection(FLATS_COLLECTION).document(flatId)
                .update("expenses", expenses).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update Home Notices
    suspend fun updateNotices(flatId: String, notices: List<Notice>): Result<Unit> {
        return try {
            firestore.collection(FLATS_COLLECTION).document(flatId).update("notices", notices).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update Home Events
    suspend fun updateEvents(flatId: String, events: List<UpcomingEvent>): Result<Unit> {
        return try {
            firestore.collection(FLATS_COLLECTION).document(flatId).update("events", events).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}