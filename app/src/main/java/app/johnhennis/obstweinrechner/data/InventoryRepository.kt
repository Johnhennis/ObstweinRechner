package app.johnhennis.obstweinrechner.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class InventoryRepository(private val firestore: FirebaseFirestore) {

    private val collection = firestore.collection("inventory")

    private val allDocuments: Flow<List<InventoryItem>> = callbackFlow {
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val items = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(InventoryItem::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(items.sortedBy { it.name })
        }
        awaitClose { listener.remove() }
    }

    val allItems: Flow<List<InventoryItem>> = allDocuments.map { list -> list.filter { !it.geloescht } }
    val trashedItems: Flow<List<InventoryItem>> = allDocuments.map { list -> list.filter { it.geloescht } }

    suspend fun insert(item: InventoryItem) {
        collection.add(item.copy(id = "", geloescht = false)).await()
    }

    suspend fun update(item: InventoryItem) {
        collection.document(item.id).set(item).await()
    }

    suspend fun moveToTrash(item: InventoryItem) {
        collection.document(item.id).update("geloescht", true).await()
    }

    suspend fun restore(item: InventoryItem) {
        collection.document(item.id).update("geloescht", false).await()
    }

    suspend fun deletePermanently(item: InventoryItem) {
        collection.document(item.id).delete().await()
    }

    suspend fun emptyTrash() {
        val snapshot = collection.whereEqualTo("geloescht", true).get().await()
        snapshot.documents.forEach { it.reference.delete().await() }
    }
}
