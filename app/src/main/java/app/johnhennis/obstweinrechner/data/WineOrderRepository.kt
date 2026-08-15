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

    // Einmalige Migration: fruehere Version hatte pro Bestellung nur ein
    // einzelnes sorte/menge-Feld statt der jetzigen "positionen"-Liste.
    // Uebernimmt den alten Wert als einzige Position, wo "positionen" noch
    // leer ist.
    suspend fun migrateToPositionen() {
        val snapshot = collection.get().await()
        val zuMigrieren = snapshot.documents.filter { doc ->
            val positionenRaw = doc.get("positionen")
            val hatPositionen = positionenRaw is List<*> && positionenRaw.isNotEmpty()
            val altSorte = doc.getString("sorte")
            !hatPositionen && !altSorte.isNullOrBlank()
        }
        coroutineScope {
            zuMigrieren.map { doc ->
                async {
                    val sorte = doc.getString("sorte") ?: ""
                    val menge = doc.getDouble("menge") ?: 0.0
                    doc.reference.update("positionen", listOf(mapOf("sorte" to sorte, "menge" to menge))).await()
                }
            }.awaitAll()
        }
    }
}
