package app.johnhennis.obstweinrechner.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FruitPriceRepository(private val firestore: FirebaseFirestore) {

    private val collection = firestore.collection("fruitPrices")

    private val allDocuments: Flow<List<FruitPrice>> = callbackFlow {
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val prices = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(FruitPrice::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(prices)
        }
        awaitClose { listener.remove() }
    }

    val allPrices: Flow<List<FruitPrice>> = allDocuments.map { list -> list.filter { !it.geloescht } }
    val trashedPrices: Flow<List<FruitPrice>> = allDocuments.map { list -> list.filter { it.geloescht } }

    suspend fun insert(price: FruitPrice) {
        collection.add(price.copy(id = "", geloescht = false)).await()
    }

    suspend fun update(price: FruitPrice) {
        collection.document(price.id).set(price).await()
    }

    suspend fun moveToTrash(price: FruitPrice) {
        collection.document(price.id).update("geloescht", true).await()
    }

    suspend fun restore(price: FruitPrice) {
        collection.document(price.id).update("geloescht", false).await()
    }

    suspend fun deletePermanently(price: FruitPrice) {
        collection.document(price.id).delete().await()
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

    // Einmalige Reparatur: entfernt echte Duplikate (ALLE Felder exakt
    // gleich - bewusst nicht nur Fruchtart+Jahr, da zwei echte Käufe
    // derselben Frucht im selben Jahr an unterschiedlichen Tagen/Preisen
    // gewollt sein können und nicht verschmolzen werden sollen). Duplikate
    // wandern in den Papierkorb statt geloescht zu werden.
    suspend fun deduplicatePrices() {
        val snapshot = collection.get().await()
        val pairs = snapshot.documents.mapNotNull { doc ->
            doc.toObject(FruitPrice::class.java)?.copy(id = doc.id)?.let { doc to it }
        }
        val gruppen = pairs.filter { !it.second.geloescht }.groupBy {
            listOf(
                it.second.fruchtart.trim().lowercase(),
                it.second.jahr,
                it.second.datum.trim(),
                it.second.preis,
                it.second.quelle.trim().lowercase()
            )
        }
        gruppen.values.filter { it.size > 1 }.forEach { gruppe ->
            gruppe.drop(1).forEach { (doc, _) -> doc.reference.update("geloescht", true).await() }
        }
    }

    suspend fun seedIfEmpty() {
        val snapshot = collection.limit(1).get().await()
        if (snapshot.isEmpty) {
            defaultPrices().forEach { collection.add(it).await() }
        }
    }

    // Aus Früchtekauf.xlsx übernommen (53 Einträge, Stand Juli 2026).
    // "198.6." bei Rhabarber 2026 ist ein vermuteter Original-Tippfehler, bewusst unverändert.
    private fun defaultPrices(): List<FruitPrice> = listOf(
        FruitPrice(fruchtart = "Aprikose", jahr = 2023, datum = "8.10.", preis = 5.72, quelle = "BÄKO"),
        FruitPrice(fruchtart = "Aprikose", jahr = 2024, datum = "30.7.", preis = 5.72, quelle = "BÄKO"),
        FruitPrice(fruchtart = "Aprikose", jahr = 2025, datum = "28.7.", preis = 5.56, quelle = "BÄKO"),
        FruitPrice(fruchtart = "Aprikose", jahr = 2026, datum = "13.7.", preis = 5.83, quelle = "BÄKO"),
        FruitPrice(fruchtart = "Aronia", jahr = 2023, datum = "6.9.", preis = 1.6, quelle = "Seidel"),
        FruitPrice(fruchtart = "Aronia", jahr = 2024, datum = "12.9.", preis = 2.2, quelle = "Seidel"),
        FruitPrice(fruchtart = "Aronia", jahr = 2025, datum = "1.9.", preis = 2.1, quelle = "Seidel"),
        FruitPrice(fruchtart = "Birnen", jahr = 2023, datum = "19.9.", preis = 0.85, quelle = "Seidel"),
        FruitPrice(fruchtart = "Brombeeren", jahr = 2020, datum = "16.7.", preis = 1.0, quelle = "Gaube"),
        FruitPrice(fruchtart = "Brombeeren", jahr = 2023, datum = "8.10.", preis = 4.27, quelle = "BÄKO"),
        FruitPrice(fruchtart = "Brombeeren", jahr = 2024, datum = "30.7.", preis = 3.37, quelle = "BÄKO"),
        FruitPrice(fruchtart = "Brombeeren", jahr = 2026, datum = "13.7.", preis = 4.01, quelle = "BÄKO"),
        FruitPrice(fruchtart = "Erdbeeren", jahr = 2020, datum = "22.6.", preis = 1.7, quelle = "Gaube/Seidel"),
        FruitPrice(fruchtart = "Erdbeeren", jahr = 2023, datum = "16.6.", preis = 2.0, quelle = "Wache"),
        FruitPrice(fruchtart = "Erdbeeren", jahr = 2024, datum = "31.5.", preis = 2.5, quelle = "Wache"),
        FruitPrice(fruchtart = "Erdbeeren", jahr = 2025, datum = "16.6.", preis = 2.5, quelle = "Wache"),
        FruitPrice(fruchtart = "Erdbeeren", jahr = 2026, datum = "11.6.", preis = 2.7, quelle = "Wache"),
        FruitPrice(fruchtart = "Heidelbeeren", jahr = 2023, datum = "10.8.", preis = 4.0, quelle = "Winkelmann"),
        FruitPrice(fruchtart = "Heidelbeeren", jahr = 2024, datum = "22.7.", preis = 3.5, quelle = "Winkelmann"),
        FruitPrice(fruchtart = "Heidelbeeren", jahr = 2025, datum = "1.8.", preis = 3.5, quelle = "Winkelmann"),
        FruitPrice(fruchtart = "Heidelbeeren TK", jahr = 2020, datum = "10.7.", preis = 3.56, quelle = "BÄKO"),
        FruitPrice(fruchtart = "Heidelbeeren TK", jahr = 2025, datum = "28.7.", preis = 5.56, quelle = "BÄKO"),
        FruitPrice(fruchtart = "Himbeeren", jahr = 2020, datum = "10.7.", preis = 3.36, quelle = "Bäko"),
        FruitPrice(fruchtart = "Himbeeren", jahr = 2023, datum = "20.6.", preis = 4.7, quelle = "BÄKO"),
        FruitPrice(fruchtart = "Himbeeren", jahr = 2024, datum = "30.7.", preis = 3.32, quelle = "BÄKO"),
        FruitPrice(fruchtart = "Himbeeren", jahr = 2025, datum = "28.7.", preis = 5.56, quelle = "BÄKO"),
        FruitPrice(fruchtart = "Himbeeren", jahr = 2026, datum = "5.6.", preis = 7.12, quelle = "BÄKO"),
        FruitPrice(fruchtart = "Johanna rot", jahr = 2023, datum = "20.6.", preis = 1.44, quelle = "BÄKO"),
        FruitPrice(fruchtart = "Johanna rot", jahr = 2024, datum = "30.7.", preis = 1.18, quelle = "BÄKO"),
        FruitPrice(fruchtart = "Johanna rot", jahr = 2025, datum = "28.7.", preis = 1.55, quelle = "BÄKO"),
        FruitPrice(fruchtart = "Johanna sw", jahr = 2020, datum = "10.7.", preis = 1.63, quelle = "Bäko"),
        FruitPrice(fruchtart = "Johanna sw", jahr = 2023, datum = "5.7.", preis = 2.0, quelle = "Seidel"),
        FruitPrice(fruchtart = "Johanna sw", jahr = 2024, datum = "17.7.", preis = 2.8, quelle = "Seidel"),
        FruitPrice(fruchtart = "Johanna sw", jahr = 2025, datum = "3.7.", preis = 3.2, quelle = "Seidel"),
        FruitPrice(fruchtart = "Pflaume", jahr = 2023, datum = "15.8.", preis = 1.3, quelle = "Seidel"),
        FruitPrice(fruchtart = "Pflaume", jahr = 2024, datum = "30.7.", preis = 1.34, quelle = "BÄKO"),
        FruitPrice(fruchtart = "Quitten", jahr = 2023, datum = "1.10.", preis = 1.0, quelle = "Seidel"),
        FruitPrice(fruchtart = "Quitten", jahr = 2024, datum = "3.10.", preis = 1.3, quelle = "Seidel"),
        FruitPrice(fruchtart = "Rhabarber", jahr = 2020, datum = "10.7.", preis = 0.9, quelle = "Bäko"),
        FruitPrice(fruchtart = "Rhabarber", jahr = 2023, datum = "26.5.", preis = 1.2, quelle = "Gaube/Seidel"),
        FruitPrice(fruchtart = "Rhabarber", jahr = 2024, datum = "30.5.", preis = 1.6, quelle = "Seidel"),
        FruitPrice(fruchtart = "Rhabarber", jahr = 2025, datum = "12.6.", preis = 1.6, quelle = "Seidel"),
        FruitPrice(fruchtart = "Rhabarber", jahr = 2026, datum = "198.6.", preis = 1.66, quelle = "Werderfrucht"),
        FruitPrice(fruchtart = "Sauerkirschen", jahr = 2020, datum = "7.7.", preis = 1.5, quelle = "Gaube/Seidel"),
        FruitPrice(fruchtart = "Sauerkirschen", jahr = 2023, datum = "18.7.", preis = 2.0, quelle = "Seidel"),
        FruitPrice(fruchtart = "Sauerkirschen", jahr = 2024, datum = "24.6.", preis = 2.4, quelle = "Seidel"),
        FruitPrice(fruchtart = "Sauerkirschen", jahr = 2025, datum = "15.7.", preis = 2.4, quelle = "Seidel"),
        FruitPrice(fruchtart = "Stachelbeeren", jahr = 2023, datum = "20.6.", preis = 1.07, quelle = "BÄKO"),
        FruitPrice(fruchtart = "Stachelbeeren", jahr = 2024, datum = "30.7.", preis = 1.28, quelle = "BÄKO"),
        FruitPrice(fruchtart = "Stachelbeeren", jahr = 2025, datum = "3.7.", preis = 2.1, quelle = "Seidel"),
        FruitPrice(fruchtart = "Stachelbeeren", jahr = 2026, datum = "13.7.", preis = 2.68, quelle = "BÄKO"),
        FruitPrice(fruchtart = "Äpfel", jahr = 2023, datum = "19.9.", preis = 0.28, quelle = "Seidel"),
        FruitPrice(fruchtart = "Äpfel", jahr = 2024, datum = "3.10.", preis = 0.3, quelle = "Seidel")
    )
}
