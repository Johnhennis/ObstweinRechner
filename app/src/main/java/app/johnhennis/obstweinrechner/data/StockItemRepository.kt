package app.johnhennis.obstweinrechner.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
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

    suspend fun insert(item: StockItem) {
        collection.add(item.copy(id = "", geloescht = false)).await()
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

    suspend fun seedIfEmpty() {
        val snapshot = collection.limit(1).get().await()
        if (snapshot.isEmpty) {
            defaultItems().forEach { collection.add(it).await() }
        }
    }

    // Aus Bestand.xls übernommen (2026 + 2027, je 12 Verpackungs-/Zubehör-
    // Positionen). Der Umsatz/Einnahmen-Block der Originaltabelle gehört
    // nicht zur Bestandsliste und wurde bewusst nicht übernommen.
    private fun defaultItems(): List<StockItem> = listOf(
        StockItem(art = "PET-Flaschen", jahr = 2026, quelle = "Internet", einheit = "Stk", bestandVorjahr = "400", einkauf = "960", rest = "640", fuerFolgejahr = "200"),
        StockItem(art = "Pfandetiketten", jahr = 2026, quelle = "Stadt", einheit = "Stk", bestandVorjahr = "280", einkauf = "500", rest = "190", fuerFolgejahr = "500"),
        StockItem(art = "Gewürzgurken", jahr = 2026, quelle = "Selgros", einheit = "10 Liter (40/45)", bestandVorjahr = "1", einkauf = "9", rest = "2", fuerFolgejahr = "6"),
        StockItem(art = "Becher Struch 0,2", jahr = 2026, quelle = "Inernet", einheit = "Stk", bestandVorjahr = "250", einkauf = "0", rest = "50", fuerFolgejahr = "200"),
        StockItem(art = "Becher Wein 0,2", jahr = 2026, quelle = "Selgros", einheit = "Pack (100)", bestandVorjahr = "7", einkauf = "60", rest = "36", fuerFolgejahr = "0"),
        StockItem(art = "Kaffeebecher 0,18", jahr = 2026, quelle = "Selgros", einheit = "100 Stk", bestandVorjahr = "250", einkauf = "0", rest = "200", fuerFolgejahr = "0"),
        StockItem(art = "Kaffee", jahr = 2026, quelle = "Supermarkt", einheit = "Stk", bestandVorjahr = "0", einkauf = "1", rest = "1", fuerFolgejahr = "1"),
        StockItem(art = "Kaffeefilter", jahr = 2026, quelle = "Supermarkt", einheit = "Packung", bestandVorjahr = "1", einkauf = "0", rest = "0.5", fuerFolgejahr = "0"),
        StockItem(art = "Kaffee-Rührstäbchen", jahr = 2026, quelle = "Selgros", einheit = "Stk", bestandVorjahr = "viele", einkauf = "", rest = "", fuerFolgejahr = "0"),
        StockItem(art = "Pappen rechteckig", jahr = 2026, quelle = "Selgros", einheit = "10 x 16", bestandVorjahr = "550", einkauf = "300", rest = "250", fuerFolgejahr = "400"),
        StockItem(art = "Pappschalen", jahr = 2026, quelle = "Selgros", einheit = "9 x 14 x 3", bestandVorjahr = "750", einkauf = "0", rest = "500", fuerFolgejahr = "0"),
        StockItem(art = "Schmalzbecher", jahr = 2026, quelle = "Selgros", einheit = "Stk", bestandVorjahr = "260", einkauf = "0", rest = "260", fuerFolgejahr = "0"),
        StockItem(art = "PET-Flaschen", jahr = 2027, quelle = "Internet", einheit = "Stk", bestandVorjahr = "640", einkauf = "200", rest = "", fuerFolgejahr = ""),
        StockItem(art = "Pfandetiketten", jahr = 2027, quelle = "Stadt", einheit = "Stk", bestandVorjahr = "190", einkauf = "500", rest = "", fuerFolgejahr = ""),
        StockItem(art = "Gewürzgurken", jahr = 2027, quelle = "Selgros", einheit = "10 Liter (40/45)", bestandVorjahr = "2", einkauf = "6", rest = "", fuerFolgejahr = ""),
        StockItem(art = "Becher Struch 0,2", jahr = 2027, quelle = "Inernet", einheit = "Stk", bestandVorjahr = "50", einkauf = "200", rest = "", fuerFolgejahr = ""),
        StockItem(art = "Becher Wein 0,2", jahr = 2027, quelle = "Selgros", einheit = "Pack (100)", bestandVorjahr = "36", einkauf = "0", rest = "", fuerFolgejahr = ""),
        StockItem(art = "Kaffeebecher 0,18", jahr = 2027, quelle = "Selgros", einheit = "100 Stk", bestandVorjahr = "200", einkauf = "0", rest = "", fuerFolgejahr = ""),
        StockItem(art = "Kaffee", jahr = 2027, quelle = "Supermarkt", einheit = "Stk", bestandVorjahr = "0", einkauf = "1", rest = "", fuerFolgejahr = ""),
        StockItem(art = "Kaffeefilter", jahr = 2027, quelle = "Supermarkt", einheit = "Packung", bestandVorjahr = "1", einkauf = "0", rest = "", fuerFolgejahr = ""),
        StockItem(art = "Kaffee-Rührstäbchen", jahr = 2027, quelle = "Selgros", einheit = "Stk", bestandVorjahr = "viele", einkauf = "0", rest = "", fuerFolgejahr = ""),
        StockItem(art = "Pappen rechteckig", jahr = 2027, quelle = "Selgros", einheit = "10 x 16", bestandVorjahr = "250", einkauf = "400", rest = "", fuerFolgejahr = ""),
        StockItem(art = "Pappschalen", jahr = 2027, quelle = "Selgros", einheit = "9 x 14 x 3", bestandVorjahr = "500", einkauf = "0", rest = "", fuerFolgejahr = ""),
        StockItem(art = "Schmalzbecher", jahr = 2027, quelle = "Selgros", einheit = "Stk", bestandVorjahr = "260", einkauf = "0", rest = "", fuerFolgejahr = "")
    )
}
