package app.johnhennis.obstweinrechner.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.johnhennis.obstweinrechner.data.ManualShoppingItem
import app.johnhennis.obstweinrechner.data.ManualShoppingItemRepository
import app.johnhennis.obstweinrechner.data.ShoppingListRepository
import app.johnhennis.obstweinrechner.data.ShoppingListStatus
import app.johnhennis.obstweinrechner.data.StockItem
import app.johnhennis.obstweinrechner.data.StockItemRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

private fun benoetigteMenge(bedarf: String, bestandVorjahr: String): String? {
    if (bedarf.isBlank()) return null
    val bedarfNum = parseNum(bedarf) ?: return bedarf
    if (bestandVorjahr.isBlank()) {
        return if (bedarfNum > 0.0001) fmtNum(bedarfNum) else null
    }
    val vorjahrNum = parseNum(bestandVorjahr) ?: return null
    val diff = bedarfNum - vorjahrNum
    return if (diff > 0.0001) fmtNum(diff) else null
}

class ShoppingListViewModel(
    private val stockItemRepository: StockItemRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val manualShoppingItemRepository: ManualShoppingItemRepository
) : ViewModel() {

    private val stockItems: StateFlow<List<StockItem>> = stockItemRepository.allItems.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList()
    )

    val entries: StateFlow<List<ShoppingListEntry>> = combine(
        stockItems,
        shoppingListRepository.allStatus,
        manualShoppingItemRepository.allItems
    ) { stock, statusMap, manualItems ->
        val aktivesJahr = stock.maxOfOrNull { it.jahr }

        val fromStock = stock
            .filter { it.jahr == aktivesJahr }
            .mapNotNull { item ->
                val menge = benoetigteMenge(item.bedarf, item.bestandVorjahr) ?: return@mapNotNull null
                ShoppingListEntry(
                    itemId = item.id,
                    name = item.art,
                    mengeText = menge,
                    erledigt = statusMap[item.id]?.erledigt ?: false,
                    quelle = item.quelle,
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
                ShoppingListSource.STOCK -> {
                    val item = stockItems.value.find { it.id == entry.itemId } ?: return@launch
                    stockItemRepository.update(item.copy(quelle = quelle))
                }
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
            coroutineScope {
                entries.value.filter { it.erledigt }.map { entry ->
                    async {
                        when (entry.source) {
                            ShoppingListSource.MANUAL -> manualShoppingItemRepository.update(
                                ManualShoppingItem(id = entry.itemId, name = entry.name, menge = entry.mengeText, quelle = entry.quelle, erledigt = false)
                            )
                            ShoppingListSource.STOCK -> shoppingListRepository.setStatus(entry.itemId, ShoppingListStatus(erledigt = false, quelle = entry.quelle))
                        }
                    }
                }.awaitAll()
            }
        }
    }
}
