package app.johnhennis.obstweinrechner.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.johnhennis.obstweinrechner.data.StockItem
import app.johnhennis.obstweinrechner.data.StockItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Year

data class StockYearGroup(
    val jahr: Int,
    val items: List<StockItem>
)

class StockViewModel(
    private val repository: StockItemRepository
) : ViewModel() {

    val currentYear: Int = Year.now().value

    val yearGroups: StateFlow<List<StockYearGroup>> = repository.allItems.map { items ->
        items.groupBy { it.jahr }
            .toSortedMap(compareByDescending { it })
            .map { (jahr, list) -> StockYearGroup(jahr = jahr, items = list.sortedBy { it.art }) }
    }.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList()
    )

    val trashedByYear: StateFlow<List<StockYearGroup>> = repository.trashedItems.map { items ->
        items.groupBy { it.jahr }
            .toSortedMap(compareByDescending { it })
            .map { (jahr, list) -> StockYearGroup(jahr = jahr, items = list.sortedBy { it.art }) }
    }.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList()
    )

    fun addItem(
        jahr: Int, art: String, quelle: String, einheit: String,
        bestandVorjahr: String, einkauf: String, rest: String, fuerFolgejahr: String, bemerkung: String
    ) {
        viewModelScope.launch {
            repository.insert(
                StockItem(
                    jahr = jahr, art = art, quelle = quelle, einheit = einheit,
                    bestandVorjahr = bestandVorjahr, einkauf = einkauf, rest = rest,
                    fuerFolgejahr = fuerFolgejahr, bemerkung = bemerkung
                )
            )
        }
    }

    fun updateItem(item: StockItem) {
        viewModelScope.launch { repository.update(item) }
    }

    fun deleteItem(item: StockItem) {
        viewModelScope.launch { repository.moveToTrash(item) }
    }

    fun deleteYear(jahr: Int) {
        viewModelScope.launch { repository.moveYearToTrash(jahr) }
    }

    fun restoreItem(item: StockItem) {
        viewModelScope.launch { repository.restore(item) }
    }

    fun deleteItemPermanently(item: StockItem) {
        viewModelScope.launch { repository.deletePermanently(item) }
    }

    fun restoreYear(jahr: Int) {
        viewModelScope.launch { repository.restoreYear(jahr) }
    }

    fun deleteYearPermanently(jahr: Int) {
        viewModelScope.launch { repository.deleteYearPermanently(jahr) }
    }
}
