package app.johnhennis.obstweinrechner.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class WineStockItemRepository(private val firestore: FirebaseFirestore) {

    private val collection = firestore.collection("wineStockItems")

    private val allDocuments: Flow<List<WineStockItem>> = callbackFlow {
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val items = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(WineStockItem::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(items)
        }
        awaitClose { listener.remove() }
    }

    val allItems: Flow<List<WineStockItem>> = allDocuments.map { list -> list.filter { !it.geloescht } }
    val trashedItems: Flow<List<WineStockItem>> = allDocuments.map { list -> list.filter { it.geloescht } }

    // Feste, aus Jahr+Sorte abgeleitete Dokument-ID - verhindert von
    // vornherein zwei Zeilen für dieselbe Sorte im selben Jahr und ist
    // idempotent bei einem wiederholten Schreibvorgang.
    private fun docId(jahr: Int, sorte: String): String {
        val slug = sorte.trim().lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        return "${jahr}_$slug"
    }

    suspend fun insert(item: WineStockItem) {
        collection.document(docId(item.jahr, item.sorte)).set(item.copy(id = "", geloescht = false)).await()
    }

    suspend fun update(item: WineStockItem) {
        collection.document(item.id).set(item).await()
    }

    suspend fun moveToTrash(item: WineStockItem) {
        collection.document(item.id).update("geloescht", true).await()
    }

    suspend fun restore(item: WineStockItem) {
        collection.document(item.id).update("geloescht", false).await()
    }

    suspend fun deletePermanently(item: WineStockItem) {
        collection.document(item.id).delete().await()
    }

    suspend fun moveYearToTrash(jahr: Int) {
        val snapshot = collection.whereEqualTo("jahr", jahr).get().await()
        snapshot.documents.filter { it.getBoolean("geloescht") != true }
            .forEach { it.reference.update("geloescht", true).await() }
    }

    suspend fun restoreYear(jahr: Int) {
        val snapshot = collection.whereEqualTo("jahr", jahr).get().await()
        snapshot.documents.filter { it.getBoolean("geloescht") == true }
            .forEach { it.reference.update("geloescht", false).await() }
    }

    suspend fun deleteYearPermanently(jahr: Int) {
        val snapshot = collection.whereEqualTo("jahr", jahr).get().await()
        snapshot.documents.filter { it.getBoolean("geloescht") == true }
            .forEach { it.reference.delete().await() }
    }
}
