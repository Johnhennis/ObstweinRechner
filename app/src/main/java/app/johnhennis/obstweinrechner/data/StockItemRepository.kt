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

    suspend fun yearExists(jahr: Int): Boolean {
        val snapshot = collection.whereEqualTo("jahr", jahr).limit(1).get().await()
        return !snapshot.isEmpty
    }

    // Legt ein neues Jahr an: Vorjahr-Bestand = bisheriger Rest. Bedarf und
    // Rest starten leer, die sollen ja frisch für die neue Saison überlegt
    // bzw. erst am Ende eingetragen werden.
    suspend fun createNextYear(fromYear: Int, toYear: Int): Int {
        val snapshot = collection.whereEqualTo("jahr", fromYear).get().await()
        val items = snapshot.documents
            .mapNotNull { it.toObject(StockItem::class.java) }
            .filter { !it.geloescht }
        items.forEach { old ->
            collection.add(
                StockItem(
                    jahr = toYear,
                    art = old.art,
                    quelle = old.quelle,
                    einheit = old.einheit,
                    bestandVorjahr = old.rest,
                    bedarf = "",
                    rest = "",
                    bemerkung = ""
                )
            ).await()
        }
        return items.size
    }

    // Einmalige Migration: das Feld hieß früher "einkauf", jetzt "bedarf"
    // (Umbenennung mit Bedeutungswechsel von "tatsächlich gekauft" zu
    // "geplanter Bedarf"). Übernimmt den alten Wert überall dort, wo
    // "bedarf" noch leer ist, damit keine bereits erfasste Planung verloren
    // geht. Greift auf die Rohdaten zu, weil das Datenmodell das alte Feld
    // gar nicht mehr kennt.
    suspend fun migrateEinkaufToBedarf() {
        val snapshot = collection.get().await()
        snapshot.documents.forEach { doc ->
            val bedarfCurrent = doc.getString("bedarf")
            if (bedarfCurrent.isNullOrBlank()) {
                val oldEinkauf = doc.getString("einkauf")
                if (!oldEinkauf.isNullOrBlank()) {
                    doc.reference.update("bedarf", oldEinkauf).await()
                }
            }
        }
    }

    suspend fun seedIfEmpty() {
        val snapshot = collection.limit(1).get().await()
        if (snapshot.isEmpty) {
            defaultItems().forEach { collection.add(it).await() }
        }
    }

    // Aus Bestand.xls übernommen (2026 + 2027, je 12 Verpackungs-/Zubehör-
    // Positionen). "Bedarf" ist hier retrospektiv mit den damaligen
    // tatsächlichen Einkaufsmengen befüllt (für vergangene/laufende Jahre
    // entspricht das dem geplanten Bedarf). Der Umsatz/Einnahmen-Block der
    // Originaltabelle gehört nicht zur Bestandsliste und wurde bewusst
    // nicht übernommen.
    private fun defaultItems(): List<StockItem> = listOf(
        StockItem(art = "PET-Flaschen", jahr = 2026, quelle = "Internet", einheit = "Stk", bestandVorjahr = "400", bedarf = "960", rest = "640"),
        StockItem(art = "Pfandetiketten", jahr = 2026, quelle = "Stadt", einheit = "Stk", bestandVorjahr = "280", bedarf = "500", rest = "190"),
        StockItem(art = "Gewürzgurken", jahr = 2026, quelle = "Selgros", einheit = "10 Liter (40/45)", bestandVorjahr = "1", bedarf = "9", rest = "2"),
        StockItem(art = "Becher Struch 0,2", jahr = 2026, quelle = "Inernet", einheit = "Stk", bestandVorjahr = "250", bedarf = "0", rest = "50"),
        StockItem(art = "Becher Wein 0,2", jahr = 2026, quelle = "Selgros", einheit = "Pack (100)", bestandVorjahr = "7", bedarf = "60", rest = "36"),
        StockItem(art = "Kaffeebecher 0,18", jahr = 2026, quelle = "Selgros", einheit = "100 Stk", bestandVorjahr = "250", bedarf = "0", rest = "200"),
        StockItem(art = "Kaffee", jahr = 2026, quelle = "Supermarkt", einheit = "Stk", bestandVorjahr = "0", bedarf = "1", rest = "1"),
        StockItem(art = "Kaffeefilter", jahr = 2026, quelle = "Supermarkt", einheit = "Packung", bestandVorjahr = "1", bedarf = "0", rest = "0.5"),
        StockItem(art = "Kaffee-Rührstäbchen", jahr = 2026, quelle = "Selgros", einheit = "Stk", bestandVorjahr = "viele", bedarf = "", rest = ""),
        StockItem(art = "Pappen rechteckig", jahr = 2026, quelle = "Selgros", einheit = "10 x 16", bestandVorjahr = "550", bedarf = "300", rest = "250"),
        StockItem(art = "Pappschalen", jahr = 2026, quelle = "Selgros", einheit = "9 x 14 x 3", bestandVorjahr = "750", bedarf = "0", rest = "500"),
        StockItem(art = "Schmalzbecher", jahr = 2026, quelle = "Selgros", einheit = "Stk", bestandVorjahr = "260", bedarf = "0", rest = "260"),
        StockItem(art = "PET-Flaschen", jahr = 2027, quelle = "Internet", einheit = "Stk", bestandVorjahr = "640", bedarf = "200", rest = ""),
        StockItem(art = "Pfandetiketten", jahr = 2027, quelle = "Stadt", einheit = "Stk", bestandVorjahr = "190", bedarf = "500", rest = ""),
        StockItem(art = "Gewürzgurken", jahr = 2027, quelle = "Selgros", einheit = "10 Liter (40/45)", bestandVorjahr = "2", bedarf = "6", rest = ""),
        StockItem(art = "Becher Struch 0,2", jahr = 2027, quelle = "Inernet", einheit = "Stk", bestandVorjahr = "50", bedarf = "200", rest = ""),
        StockItem(art = "Becher Wein 0,2", jahr = 2027, quelle = "Selgros", einheit = "Pack (100)", bestandVorjahr = "36", bedarf = "0", rest = ""),
        StockItem(art = "Kaffeebecher 0,18", jahr = 2027, quelle = "Selgros", einheit = "100 Stk", bestandVorjahr = "200", bedarf = "0", rest = ""),
        StockItem(art = "Kaffee", jahr = 2027, quelle = "Supermarkt", einheit = "Stk", bestandVorjahr = "0", bedarf = "1", rest = ""),
        StockItem(art = "Kaffeefilter", jahr = 2027, quelle = "Supermarkt", einheit = "Packung", bestandVorjahr = "1", bedarf = "0", rest = ""),
        StockItem(art = "Kaffee-Rührstäbchen", jahr = 2027, quelle = "Selgros", einheit = "Stk", bestandVorjahr = "viele", bedarf = "0", rest = ""),
        StockItem(art = "Pappen rechteckig", jahr = 2027, quelle = "Selgros", einheit = "10 x 16", bestandVorjahr = "250", bedarf = "400", rest = ""),
        StockItem(art = "Pappschalen", jahr = 2027, quelle = "Selgros", einheit = "9 x 14 x 3", bestandVorjahr = "500", bedarf = "0", rest = ""),
        StockItem(art = "Schmalzbecher", jahr = 2027, quelle = "Selgros", einheit = "Stk", bestandVorjahr = "260", bedarf = "0", rest = "")
    )
}
