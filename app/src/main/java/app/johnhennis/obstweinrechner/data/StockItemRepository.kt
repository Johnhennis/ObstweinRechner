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

class StockItemRepository(private val firestore: FirebaseFirestore) {

    private val collection = firestore.collection("stockItems")

    private val allDocuments: Flow<List<StockItem>> = callbackFlow {
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val items = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(StockItem::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(items)
        }
        awaitClose { listener.remove() }
    }

    val allItems: Flow<List<StockItem>> = allDocuments.map { list -> list.filter { !it.geloescht } }
    val trashedItems: Flow<List<StockItem>> = allDocuments.map { list -> list.filter { it.geloescht } }

    private fun docId(jahr: Int, art: String): String {
        val slug = art.trim().lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        return "${jahr}_$slug"
    }

    suspend fun insert(item: StockItem) {
        collection.document(docId(item.jahr, item.art)).set(item.copy(id = "", geloescht = false)).await()
    }

    suspend fun update(item: StockItem) {
        collection.document(item.id).set(item).await()
    }

    suspend fun moveToTrash(item: StockItem) {
        collection.document(item.id).update("geloescht", true).await()
    }

    suspend fun restore(item: StockItem) {
        collection.document(item.id).update("geloescht", false).await()
    }

    suspend fun deletePermanently(item: StockItem) {
        collection.document(item.id).delete().await()
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

    suspend fun yearExists(jahr: Int): Boolean {
        val snapshot = collection.whereEqualTo("jahr", jahr).limit(1).get().await()
        return !snapshot.isEmpty
    }

    suspend fun createNextYear(fromYear: Int, toYear: Int): Int {
        val snapshot = collection.whereEqualTo("jahr", fromYear).get().await()
        val items = snapshot.documents
            .mapNotNull { it.toObject(StockItem::class.java) }
            .filter { !it.geloescht }
        coroutineScope {
            items.map { old ->
                async {
                    val neu = StockItem(
                        jahr = toYear,
                        art = old.art,
                        quelle = old.quelle,
                        bestandVorjahr = old.rest,
                        bedarf = old.bedarf,
                        rest = ""
                    )
                    collection.document(docId(toYear, old.art)).set(neu.copy(id = "")).await()
                }
            }.awaitAll()
        }
        return items.size
    }

    suspend fun migrateEinkaufToBedarf() {
        val snapshot = collection.get().await()
        val zuMigrieren = snapshot.documents.filter { doc ->
            doc.getString("bedarf").isNullOrBlank() && !doc.getString("einkauf").isNullOrBlank()
        }
        coroutineScope {
            zuMigrieren.map { doc ->
                async { doc.reference.update("bedarf", doc.getString("einkauf")).await() }
            }.awaitAll()
        }
    }

    suspend fun deduplicateItems() {
        val snapshot = collection.get().await()
        val pairs = snapshot.documents.mapNotNull { doc ->
            doc.toObject(StockItem::class.java)?.copy(id = doc.id)?.let { doc to it }
        }
        val gruppen = pairs.filter { !it.second.geloescht }
            .groupBy { it.second.jahr to it.second.art.trim().lowercase() }

        fun vollstaendigkeit(item: StockItem) = listOf(
            item.quelle, item.bestandVorjahr, item.bedarf, item.rest
        ).count { it.isNotBlank() }

        val zuLoeschen = gruppen.values.filter { it.size > 1 }.flatMap { gruppe ->
            gruppe.sortedByDescending { vollstaendigkeit(it.second) }.drop(1)
        }
        coroutineScope {
            zuLoeschen.map { (doc, _) -> async { doc.reference.update("geloescht", true).await() } }.awaitAll()
        }
    }

    suspend fun seedIfEmpty() {
        val snapshot = collection.limit(1).get().await()
        if (snapshot.isEmpty) {
            coroutineScope {
                defaultItems().map { async { collection.document(docId(it.jahr, it.art)).set(it).await() } }.awaitAll()
            }
        }
    }

    // Aus Bestand.xls übernommen (2026 + 2027, je 12 Verpackungs-/Zubehör-
    // Positionen). Einheit/Bemerkung entfernt, entsprach nicht mehr dem
    // Datenmodell. Der Umsatz/Einnahmen-Block der Originaltabelle gehört
    // nicht zur Bestandsliste und wurde bewusst nicht übernommen.
    private fun defaultItems(): List<StockItem> = listOf(
        StockItem(art = "PET-Flaschen", jahr = 2026, quelle = "Internet", bestandVorjahr = "400", bedarf = "960", rest = "640"),
        StockItem(art = "Pfandetiketten", jahr = 2026, quelle = "Stadt", bestandVorjahr = "280", bedarf = "500", rest = "190"),
        StockItem(art = "Gewürzgurken", jahr = 2026, quelle = "Selgros", bestandVorjahr = "1", bedarf = "9", rest = "2"),
        StockItem(art = "Becher Struch 0,2", jahr = 2026, quelle = "Inernet", bestandVorjahr = "250", bedarf = "0", rest = "50"),
        StockItem(art = "Becher Wein 0,2", jahr = 2026, quelle = "Selgros", bestandVorjahr = "7", bedarf = "60", rest = "36"),
        StockItem(art = "Kaffeebecher 0,18", jahr = 2026, quelle = "Selgros", bestandVorjahr = "250", bedarf = "0", rest = "200"),
        StockItem(art = "Kaffee", jahr = 2026, quelle = "Supermarkt", bestandVorjahr = "0", bedarf = "1", rest = "1"),
        StockItem(art = "Kaffeefilter", jahr = 2026, quelle = "Supermarkt", bestandVorjahr = "1", bedarf = "0", rest = "0.5"),
        StockItem(art = "Kaffee-Rührstäbchen", jahr = 2026, quelle = "Selgros", bestandVorjahr = "viele", bedarf = "", rest = ""),
        StockItem(art = "Pappen rechteckig", jahr = 2026, quelle = "Selgros", bestandVorjahr = "550", bedarf = "300", rest = "250"),
        StockItem(art = "Pappschalen", jahr = 2026, quelle = "Selgros", bestandVorjahr = "750", bedarf = "0", rest = "500"),
        StockItem(art = "Schmalzbecher", jahr = 2026, quelle = "Selgros", bestandVorjahr = "260", bedarf = "0", rest = "260"),
        StockItem(art = "PET-Flaschen", jahr = 2027, quelle = "Internet", bestandVorjahr = "640", bedarf = "200", rest = ""),
        StockItem(art = "Pfandetiketten", jahr = 2027, quelle = "Stadt", bestandVorjahr = "190", bedarf = "500", rest = ""),
        StockItem(art = "Gewürzgurken", jahr = 2027, quelle = "Selgros", bestandVorjahr = "2", bedarf = "6", rest = ""),
        StockItem(art = "Becher Struch 0,2", jahr = 2027, quelle = "Inernet", bestandVorjahr = "50", bedarf = "200", rest = ""),
        StockItem(art = "Becher Wein 0,2", jahr = 2027, quelle = "Selgros", bestandVorjahr = "36", bedarf = "0", rest = ""),
        StockItem(art = "Kaffeebecher 0,18", jahr = 2027, quelle = "Selgros", bestandVorjahr = "200", bedarf = "0", rest = ""),
        StockItem(art = "Kaffee", jahr = 2027, quelle = "Supermarkt", bestandVorjahr = "0", bedarf = "1", rest = ""),
        StockItem(art = "Kaffeefilter", jahr = 2027, quelle = "Supermarkt", bestandVorjahr = "1", bedarf = "0", rest = ""),
        StockItem(art = "Kaffee-Rührstäbchen", jahr = 2027, quelle = "Selgros", bestandVorjahr = "viele", bedarf = "0", rest = ""),
        StockItem(art = "Pappen rechteckig", jahr = 2027, quelle = "Selgros", bestandVorjahr = "250", bedarf = "400", rest = ""),
        StockItem(art = "Pappschalen", jahr = 2027, quelle = "Selgros", bestandVorjahr = "500", bedarf = "0", rest = ""),
        StockItem(art = "Schmalzbecher", jahr = 2027, quelle = "Selgros", bestandVorjahr = "260", bedarf = "0", rest = "")
    )
}
