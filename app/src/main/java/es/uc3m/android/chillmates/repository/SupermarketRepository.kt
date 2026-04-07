package es.uc3m.android.chillmates.repository

import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.chillmates.model.Supermarket
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SupermarketRepository {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun observeSupermarkets(flatId: String): Flow<List<Supermarket>> = callbackFlow {
        val collectionPath = "flats/$flatId/supermarkets"
        val listener = firestore.collection(collectionPath)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val supermarkets = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Supermarket::class.java)?.apply { id = doc.id }
                } ?: emptyList()
                trySend(supermarkets)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addSupermarket(flatId: String, supermarket: Supermarket): Result<Supermarket> {
        return try {
            val docRef = firestore.collection("flats/$flatId/supermarkets").document()
            val supermarketWithId = supermarket.copy(id = docRef.id)
            docRef.set(supermarketWithId).await()
            Result.success(supermarketWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSupermarket(flatId: String, supermarketId: String): Result<Unit> {
        return try {
            firestore.collection("flats/$flatId/supermarkets").document(supermarketId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}