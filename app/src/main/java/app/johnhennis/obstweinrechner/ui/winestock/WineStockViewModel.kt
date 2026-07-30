package app.johnhennis.obstweinrechner.ui.winestock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.johnhennis.obstweinrechner.data.WineStockItem
import app.johnhennis.obstweinrechner.data.WineStockItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Year

data class WineStockYearGroup(
    val jahr: Int,
    val items: List<WineStockItem>,
    val sollSumme: Double,
    val aktuelleSumme: Double
)

class WineStockViewModel(
    private val repository: WineStockItemRepository
) : ViewModel() {

    val currentYear: Int = Year.now().value

    val yearGroups: StateFlow<List<WineStockYearGroup>> = repository.allItems.map { items ->
        items.groupBy { it.jahr }
            .toSortedMap(compareByDescending { it })
            .map { (jahr, list) ->
                val sorted = list.sortedBy { it.sorte }
                WineStockYearGroup(
                    jahr = jahr,
                    items = sorted,
                    sollSumme = sorted.sumOf { it.sollmenge },
                    aktuelleSumme = sorted.sumOf { it.aktuelleMenge }
                )
            }
    }.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList()
    )

    val trashedByYear: StateFlow<List<WineStockYearGroup>> = repository.trashedItems.map { items ->
        items.groupBy { it.jahr }
            .toSortedMap(compareByDescending { it })
            .map { (jahr, list) ->
                val sorted = list.sortedBy { it.sorte }
                WineStockYearGroup(
                    jahr = jahr,
                    items = sorted,
                    sollSumme = sorted.sumOf { it.sollmenge },
                    aktuelleSumme = sorted.sumOf { it.aktuelleMenge }
                )
            }
    }.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList()
    )

    fun addItem(jahr: Int, sorte: String, sollmenge: Double, aktuelleMenge: Double) {
        viewModelScope.launch {
            repository.insert(WineStockItem(jahr = jahr, sorte = sorte, sollmenge = sollmenge, aktuelleMenge = aktuelleMenge))
        }
    }

    fun updateItem(item: WineStockItem) {
        viewModelScope.launch { repository.update(item) }
    }

    fun deleteItem(item: WineStockItem) {
        viewModelScope.launch { repository.moveToTrash(item) }
    }

    fun deleteYear(jahr: Int) {
        viewModelScope.launch { repository.moveYearToTrash(jahr) }
    }

    fun restoreItem(item: WineStockItem) {
        viewModelScope.launch { repository.restore(item) }
    }

    fun deleteItemPermanently(item: WineStockItem) {
        viewModelScope.launch { repository.deletePermanently(item) }
    }

    fun restoreYear(jahr: Int) {
        viewModelScope.launch { repository.restoreYear(jahr) }
    }

    fun deleteYearPermanently(jahr: Int) {
        viewModelScope.launch { repository.deleteYearPermanently(jahr) }
    }
}
