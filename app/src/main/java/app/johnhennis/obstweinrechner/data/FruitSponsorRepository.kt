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

class FruitSponsorRepository(private val firestore: FirebaseFirestore) {

    private val collection = firestore.collection("fruitSponsors")

    private val allDocuments: Flow<List<FruitSponsor>> = callbackFlow {
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val items = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(FruitSponsor::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(items)
        }
        awaitClose { listener.remove() }
    }

    val allSponsors: Flow<List<FruitSponsor>> = allDocuments.map { list -> list.filter { !it.geloescht } }
    val trashedSponsors: Flow<List<FruitSponsor>> = allDocuments.map { list -> list.filter { it.geloescht } }

    suspend fun insert(sponsor: FruitSponsor) {
        collection.add(sponsor.copy(id = "", geloescht = false)).await()
    }

    suspend fun update(sponsor: FruitSponsor) {
        collection.document(sponsor.id).set(sponsor).await()
    }

    suspend fun moveToTrash(sponsor: FruitSponsor) {
        collection.document(sponsor.id).update("geloescht", true).await()
    }

    suspend fun restore(sponsor: FruitSponsor) {
        collection.document(sponsor.id).update("geloescht", false).await()
    }

    suspend fun deletePermanently(sponsor: FruitSponsor) {
        collection.document(sponsor.id).delete().await()
    }

    suspend fun moveYearToTrash(jahr: Int) {
        val snapshot = collection.whereEqualTo("jahr", jahr).get().await()
        coroutineScope {
            snapshot.documents.filter { it.getBoolean("geloescht") != true }
                .map { async { it.reference.update("geloescht", true).await() } }
                .awaitAll()
        }
    }

    suspend fun restoreYear(jahr: Int) {
        val snapshot = collection.whereEqualTo("jahr", jahr).get().await()
        coroutineScope {
            snapshot.documents.filter { it.getBoolean("geloescht") == true }
                .map { async { it.reference.update("geloescht", false).await() } }
                .awaitAll()
        }
    }

    suspend fun deleteYearPermanently(jahr: Int) {
        val snapshot = collection.whereEqualTo("jahr", jahr).get().await()
        coroutineScope {
            snapshot.documents.filter { it.getBoolean("geloescht") == true }
                .map { async { it.reference.delete().await() } }
                .awaitAll()
        }
    }
}
