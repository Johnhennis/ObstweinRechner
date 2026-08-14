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

class WineOrderRepository(private val firestore: FirebaseFirestore) {

    private val collection = firestore.collection("wineOrders")

    private val allDocuments: Flow<List<WineOrder>> = callbackFlow {
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val orders = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(WineOrder::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(orders)
        }
        awaitClose { listener.remove() }
    }

    val allOrders: Flow<List<WineOrder>> = allDocuments.map { list -> list.filter { !it.geloescht } }
    val trashedOrders: Flow<List<WineOrder>> = allDocuments.map { list -> list.filter { it.geloescht } }

    // Zufällige statt fester ID: dieselbe Person könnte dieselbe Sorte im
    // selben Jahr durchaus zweimal bestellen (unterschiedliche Termine) -
    // eine feste Jahr+Name-ID würde solche echten Zweitbestellungen fälschlich
    // verschmelzen. Gibt die generierte ID zurück, damit direkt danach eine
    // Erinnerung dafür geplant werden kann.
    suspend fun insert(order: WineOrder): String {
        val ref = collection.add(order.copy(id = "", geloescht = false)).await()
        return ref.id
    }

    suspend fun update(order: WineOrder) {
        collection.document(order.id).set(order).await()
    }

    suspend fun moveToTrash(order: WineOrder) {
        collection.document(order.id).update("geloescht", true).await()
    }

    suspend fun restore(order: WineOrder) {
        collection.document(order.id).update("geloescht", false).await()
    }

    suspend fun deletePermanently(order: WineOrder) {
        collection.document(order.id).delete().await()
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
