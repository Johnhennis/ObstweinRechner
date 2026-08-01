package app.johnhennis.obstweinrechner.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class WeinprobeRepository(private val firestore: FirebaseFirestore) {

    private val collection = firestore.collection("weinproben")

    private fun docId(datum: String, sorte: String): String {
        val slug = sorte.trim().lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        return "${datum}_$slug"
    }

    private val allDocuments: Flow<List<WeinprobeEntry>> = callbackFlow {
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val entries = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(WeinprobeEntry::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(entries)
        }
        awaitClose { listener.remove() }
    }

    val allEntries: Flow<List<WeinprobeEntry>> = allDocuments.map { list -> list.filter { !it.geloescht } }
    val trashedEntries: Flow<List<WeinprobeEntry>> = allDocuments.map { list -> list.filter { it.geloescht } }

    suspend fun insert(entry: WeinprobeEntry) {
        collection.document(docId(entry.datum, entry.sorte)).set(entry.copy(id = "", geloescht = false)).await()
    }

    // Legt mehrere Positionen auf einmal an (z.B. beim Erstellen einer neuen
    // Weinprobe aus dem Ist-Bestand) - parallel statt nacheinander.
    suspend fun insertAll(entries: List<WeinprobeEntry>) {
        coroutineScope {
            entries.map { async { insert(it) } }.awaitAll()
        }
    }

    suspend fun update(entry: WeinprobeEntry) {
        collection.document(entry.id).set(entry).await()
    }

    suspend fun moveToTrash(entry: WeinprobeEntry) {
        collection.document(entry.id).update("geloescht", true).await()
    }

    suspend fun restore(entry: WeinprobeEntry) {
        collection.document(entry.id).update("geloescht", false).await()
    }

    suspend fun deletePermanently(entry: WeinprobeEntry) {
        collection.document(entry.id).delete().await()
    }

    suspend fun moveDatumToTrash(datum: String) {
        val snapshot = collection.whereEqualTo("datum", datum).get().await()
        coroutineScope {
            snapshot.documents.filter { it.getBoolean("geloescht") != true }
                .map { async { it.reference.update("geloescht", true).await() } }
                .awaitAll()
        }
    }

    suspend fun restoreDatum(datum: String) {
        val snapshot = collection.whereEqualTo("datum", datum).get().await()
        coroutineScope {
            snapshot.documents.filter { it.getBoolean("geloescht") == true }
                .map { async { it.reference.update("geloescht", false).await() } }
                .awaitAll()
        }
    }

    suspend fun deleteDatumPermanently(datum: String) {
        val snapshot = collection.whereEqualTo("datum", datum).get().await()
        coroutineScope {
            snapshot.documents.filter { it.getBoolean("geloescht") == true }
                .map { async { it.reference.delete().await() } }
                .awaitAll()
        }
    }

    suspend fun datumExists(datum: String): Boolean {
        val snapshot = collection.whereEqualTo("datum", datum).limit(1).get().await()
        return !snapshot.isEmpty
    }
}
