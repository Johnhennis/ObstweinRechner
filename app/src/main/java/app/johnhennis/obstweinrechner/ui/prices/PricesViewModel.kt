package app.johnhennis.obstweinrechner.ui.prices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.johnhennis.obstweinrechner.data.FruitPrice
import app.johnhennis.obstweinrechner.data.FruitPriceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Year

data class PriceRow(
    val price: FruitPrice,
    val vorjahresPreis: Double? = null
)

data class YearGroup(
    val jahr: Int,
    val rows: List<PriceRow>
)

class PricesViewModel(
    private val repository: FruitPriceRepository
) : ViewModel() {

    val currentYear: Int = Year.now().value

    val yearGroups: StateFlow<List<YearGroup>> = repository.allPrices.map { prices ->
        val byFruchtart = prices.groupBy { it.fruchtart }
        prices
            .groupBy { it.jahr }
            .toSortedMap(compareByDescending { it })
            .map { (jahr, entries) ->
                val rows = entries.sortedBy { it.fruchtart }.map { price ->
                    val vorjahr = if (jahr == currentYear) {
                        byFruchtart[price.fruchtart]
                            ?.filter { it.jahr < jahr }
                            ?.maxByOrNull { it.jahr }
                            ?.preis
                    } else null
                    PriceRow(price = price, vorjahresPreis = vorjahr)
                }
                YearGroup(jahr = jahr, rows = rows)
            }
    }.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList()
    )

    val trashedByYear: StateFlow<List<YearGroup>> = repository.trashedPrices.map { prices ->
        prices.groupBy { it.jahr }
            .toSortedMap(compareByDescending { it })
            .map { (jahr, entries) -> YearGroup(jahr = jahr, rows = entries.sortedBy { it.fruchtart }.map { PriceRow(it) }) }
    }.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList()
    )

    fun addEntry(jahr: Int, fruchtart: String, datum: String, preis: Double, quelle: String) {
        viewModelScope.launch {
            repository.insert(FruitPrice(fruchtart = fruchtart, jahr = jahr, datum = datum, preis = preis, quelle = quelle))
        }
    }

    fun updateEntry(price: FruitPrice) {
        viewModelScope.launch { repository.update(price) }
    }

    fun deleteEntry(price: FruitPrice) {
        viewModelScope.launch { repository.moveToTrash(price) }
    }

    fun deleteYear(jahr: Int) {
        viewModelScope.launch { repository.moveYearToTrash(jahr) }
    }

    fun restoreEntry(price: FruitPrice) {
        viewModelScope.launch { repository.restore(price) }
    }

    fun deleteEntryPermanently(price: FruitPrice) {
        viewModelScope.launch { repository.deletePermanently(price) }
    }

    fun restoreYear(jahr: Int) {
        viewModelScope.launch { repository.restoreYear(jahr) }
    }

    fun deleteYearPermanently(jahr: Int) {
        viewModelScope.launch { repository.deleteYearPermanently(jahr) }
    }
}
