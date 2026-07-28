package app.johnhennis.obstweinrechner.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.johnhennis.obstweinrechner.data.ManualShoppingItem
import app.johnhennis.obstweinrechner.data.ManualShoppingItemRepository
import app.johnhennis.obstweinrechner.data.ShoppingListRepository
import app.johnhennis.obstweinrechner.data.ShoppingListStatus
import app.johnhennis.obstweinrechner.data.StockItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ShoppingListSource { STOCK, MANUAL }

data class ShoppingListEntry(
    val itemId: String,
    val name: String,
    val mengeText: String,
    val erledigt: Boolean,
    val quelle: String,
    val source: ShoppingListSource
)

private fun parseNum(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

private fun fmtNum(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

// Ermittelt die Einkaufsliste-Menge aus Bedarf minus Bestand. Leerer Bedarf
// -> nichts geplant, taucht nicht auf. Nicht-numerischer Bedarf (z.B.
// "viele") -> direkt übernommen, da keine Differenz berechenbar ist.
// Nicht-numerischer Bestand (z.B. "viele") -> als bereits ausreichend
// gewertet, taucht nicht auf. Differenz <= 0 -> bereits genug vorhanden.
private fun benoetigteMenge(bedarf: String, bestand: String): String? {
    if (bedarf.isBlank()) return null
    val bedarfNum = parseNum(bedarf) ?: return bedarf
    if (bestand.isBlank()) {
        return if (bedarfNum > 0.0001) fmtNum(bedarfNum) else null
    }
    val bestandNum = parseNum(bestand) ?: return null
    val diff = bedarfNum - bestandNum
    return if (diff > 0.0001) fmtNum(diff) else null
}

class ShoppingListViewModel(
    private val stockItemRepository: StockItemRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val manualShoppingItemRepository: ManualShoppingItemRepository
) : ViewModel() {

    // Nicht das echte Kalenderjahr, sondern das neueste in der Bestandsliste
    // vorhandene Jahr - das ist das Jahr, für das gerade aktiv geplant wird
    // (ihr kauft ja bewusst im Voraus fürs Folgejahr ein).
    val entries: StateFlow<List<ShoppingListEntry>> = combine(
        stockItemRepository.allItems,
        shoppingListRepository.allStatus,
        manualShoppingItemRepository.allItems
    ) { stockItems, statusMap, manualItems ->
        val aktivesJahr = stockItems.maxOfOrNull { it.jahr }

        val fromStock = stockItems
            .filter { it.jahr == aktivesJahr }
            .mapNotNull { item ->
                val menge = benoetigteMenge(item.bedarf, item.bestandVorjahr) ?: return@mapNotNull null
                val status = statusMap[item.id]
                ShoppingListEntry(
                    itemId = item.id,
                    name = item.art,
                    mengeText = "$menge${if (item.einheit.isBlank()) "" else " ${item.einheit}"}",
                    erledigt = status?.erledigt ?: false,
                    quelle = status?.quelle?.ifBlank { item.quelle } ?: item.quelle,
                    source = ShoppingListSource.STOCK
                )
            }

        val fromManual = manualItems.map { item ->
            ShoppingListEntry(
                itemId = item.id,
                name = item.name,
                mengeText = item.menge,
                erledigt = item.erledigt,
                quelle = item.quelle,
                source = ShoppingListSource.MANUAL
            )
        }

        (fromStock + fromManual).sortedBy { it.name }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun toggleErledigt(entry: ShoppingListEntry) {
        viewModelScope.launch {
            when (entry.source) {
                ShoppingListSource.MANUAL -> manualShoppingItemRepository.update(
                    ManualShoppingItem(id = entry.itemId, name = entry.name, menge = entry.mengeText, quelle = entry.quelle, erledigt = !entry.erledigt)
                )
                ShoppingListSource.STOCK -> shoppingListRepository.setStatus(entry.itemId, ShoppingListStatus(erledigt = !entry.erledigt, quelle = entry.quelle))
            }
        }
    }

    fun updateQuelle(entry: ShoppingListEntry, quelle: String) {
        viewModelScope.launch {
            when (entry.source) {
                ShoppingListSource.MANUAL -> manualShoppingItemRepository.update(
                    ManualShoppingItem(id = entry.itemId, name = entry.name, menge = entry.mengeText, quelle = quelle, erledigt = entry.erledigt)
                )
                ShoppingListSource.STOCK -> shoppingListRepository.setStatus(entry.itemId, ShoppingListStatus(erledigt = entry.erledigt, quelle = quelle))
            }
        }
    }

    fun addManualItem(name: String, menge: String, quelle: String) {
        viewModelScope.launch {
            manualShoppingItemRepository.insert(ManualShoppingItem(name = name, menge = menge, quelle = quelle))
        }
    }

    fun deleteManualItem(entry: ShoppingListEntry) {
        if (entry.source != ShoppingListSource.MANUAL) return
        viewModelScope.launch {
            manualShoppingItemRepository.delete(ManualShoppingItem(id = entry.itemId))
        }
    }

    fun clearSelection() {
        viewModelScope.launch {
            entries.value.filter { it.erledigt }.forEach { entry ->
                when (entry.source) {
                    ShoppingListSource.MANUAL -> manualShoppingItemRepository.update(
                        ManualShoppingItem(id = entry.itemId, name = entry.name, menge = entry.mengeText, quelle = entry.quelle, erledigt = false)
                    )
                    ShoppingListSource.STOCK -> shoppingListRepository.setStatus(entry.itemId, ShoppingListStatus(erledigt = false, quelle = entry.quelle))
                }
            }
        }
    }
}
