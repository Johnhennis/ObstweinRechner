package app.johnhennis.obstweinrechner.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// Bewusst ohne eigenen Papierkorb: gedacht für den kurzfristigen Gebrauch
// (vor dem Einkauf hinzufügen, danach löschen).
class ManualShoppingItemRepository(private val firestore: FirebaseFirestore) {

    private val collection = firestore.collection("manualShoppingItems")

    val allItems: Flow<List<ManualShoppingItem>> = callbackFlow {
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val items = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ManualShoppingItem::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(items)
        }
        awaitClose { listener.remove() }
    }

    suspend fun insert(item: ManualShoppingItem) {
        collection.add(item.copy(id = "")).await()
    }

    suspend fun update(item: ManualShoppingItem) {
        collection.document(item.id).set(item).await()
    }

    suspend fun delete(item: ManualShoppingItem) {
        collection.document(item.id).delete().await()
    }
}
