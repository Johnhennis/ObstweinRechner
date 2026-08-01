package app.johnhennis.obstweinrechner.ui.weinprobe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.johnhennis.obstweinrechner.data.WeinprobeEntry
import app.johnhennis.obstweinrechner.data.WeinprobeRepository
import app.johnhennis.obstweinrechner.data.WineStockItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeinprobeDateGroup(
    val datum: String,
    val entries: List<WeinprobeEntry>
)

class WeinprobeViewModel(
    private val repository: WeinprobeRepository,
    private val wineStockItemRepository: WineStockItemRepository
) : ViewModel() {

    val dateGroups: StateFlow<List<WeinprobeDateGroup>> = repository.allEntries.map { entries ->
        entries.groupBy { it.datum }
            .toSortedMap(compareByDescending { it })
            .map { (datum, list) -> WeinprobeDateGroup(datum = datum, entries = list.sortedBy { it.sorte }) }
    }.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList()
    )

    val trashedByDate: StateFlow<List<WeinprobeDateGroup>> = repository.trashedEntries.map { entries ->
        entries.groupBy { it.datum }
            .toSortedMap(compareByDescending { it })
            .map { (datum, list) -> WeinprobeDateGroup(datum = datum, entries = list.sortedBy { it.sorte }) }
    }.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList()
    )

    fun createWeinprobe(datum: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (repository.datumExists(datum)) {
                onResult(false, "Für dieses Datum gibt es bereits eine Weinprobe.")
                return@launch
            }
            val currentStock = wineStockItemRepository.allItems.first()
            val relevant = currentStock.filter { it.aktuelleMenge > 0.0 }
            if (relevant.isEmpty()) {
                onResult(false, "Kein Weinbestand mit Ist-Menge über 0 vorhanden.")
                return@launch
            }
            repository.insertAll(relevant.map { WeinprobeEntry(datum = datum, sorte = it.sorte) })
            onResult(true, "Weinprobe angelegt mit ${relevant.size} Sorten.")
        }
    }

    fun updateEntry(entry: WeinprobeEntry) {
        viewModelScope.launch { repository.update(entry) }
    }

    fun deleteEntry(entry: WeinprobeEntry) {
        viewModelScope.launch { repository.moveToTrash(entry) }
    }

    fun deleteDatum(datum: String) {
        viewModelScope.launch { repository.moveDatumToTrash(datum) }
    }

    fun restoreEntry(entry: WeinprobeEntry) {
        viewModelScope.launch { repository.restore(entry) }
    }

    fun deleteEntryPermanently(entry: WeinprobeEntry) {
        viewModelScope.launch { repository.deletePermanently(entry) }
    }

    fun restoreDatum(datum: String) {
        viewModelScope.launch { repository.restoreDatum(datum) }
    }

    fun deleteDatumPermanently(datum: String) {
        viewModelScope.launch { repository.deleteDatumPermanently(datum) }
    }
}
