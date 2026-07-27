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
import java.time.Year

enum class ShoppingListSource { STOCK, MANUAL }

data class ShoppingListEntry(
    val itemId: String,
    val name: String,
    val mengeText: String,
    val erledigt: Boolean,
    val quelle: String,
    val source: ShoppingListSource
)

class ShoppingListViewModel(
    private val stockItemRepository: StockItemRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val manualShoppingItemRepository: ManualShoppingItemRepository
) : ViewModel() {

    private val currentYear = Year.now().value

    // Nur das aktuelle Kalenderjahr - vergangene Jahre der Bestandsliste
    // wurden ja bereits eingekauft und sollen nicht wieder auftauchen.
    val entries: StateFlow<List<ShoppingListEntry>> = combine(
        stockItemRepository.allItems,
        shoppingListRepository.allStatus,
        manualShoppingItemRepository.allItems
    ) { stockItems, statusMap, manualItems ->
        val fromStock = stockItems
            .filter { it.jahr == currentYear }
            .filter { item ->
                val v = item.einkauf.trim()
                v.isNotEmpty() && (v.toDoubleOrNull()?.let { it != 0.0 } ?: true)
            }
            .map { item ->
                val status = statusMap[item.id]
                ShoppingListEntry(
                    itemId = item.id,
                    name = item.art,
                    mengeText = "${item.einkauf}${if (item.einheit.isBlank()) "" else " ${item.einheit}"}",
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
