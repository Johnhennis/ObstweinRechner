package app.johnhennis.obstweinrechner.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class InfoEntryRepository(private val firestore: FirebaseFirestore) {

    private val collection = firestore.collection("infoEntries")

    private val allDocuments: Flow<List<InfoEntry>> = callbackFlow {
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val entries = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(InfoEntry::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(entries)
        }
        awaitClose { listener.remove() }
    }

    val allEntries: Flow<List<InfoEntry>> = allDocuments.map { list -> list.filter { !it.geloescht } }
    val trashedEntries: Flow<List<InfoEntry>> = allDocuments.map { list -> list.filter { it.geloescht } }

    suspend fun insert(text: String) {
        collection.add(InfoEntry(text = text)).await()
    }

    suspend fun moveToTrash(entry: InfoEntry) {
        collection.document(entry.id).update("geloescht", true).await()
    }

    suspend fun restore(entry: InfoEntry) {
        collection.document(entry.id).update("geloescht", false).await()
    }

    suspend fun deletePermanently(entry: InfoEntry) {
        collection.document(entry.id).delete().await()
    }

    suspend fun emptyTrash() {
        val snapshot = collection.whereEqualTo("geloescht", true).get().await()
        snapshot.documents.forEach { it.reference.delete().await() }
    }

    // Einmalige Reparatur: entfernt echte Duplikate (identischer Text).
    // Verschiebt sie in den Papierkorb statt sie zu loeschen.
    suspend fun deduplicateEntries() {
        val snapshot = collection.get().await()
        val pairs = snapshot.documents.mapNotNull { doc ->
            doc.toObject(InfoEntry::class.java)?.copy(id = doc.id)?.let { doc to it }
        }
        val gruppen = pairs.filter { !it.second.geloescht }.groupBy { it.second.text.trim() }
        gruppen.values.filter { it.size > 1 }.forEach { gruppe ->
            gruppe.drop(1).forEach { (doc, _) -> doc.reference.update("geloescht", true).await() }
        }
    }
}
