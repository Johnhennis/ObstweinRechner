package app.johnhennis.obstweinrechner.ui.sponsors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.johnhennis.obstweinrechner.data.FruitSponsor
import app.johnhennis.obstweinrechner.data.FruitSponsorRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Year

data class SponsorYearGroup(
    val jahr: Int,
    val sponsors: List<FruitSponsor>
)

class SponsorViewModel(
    private val repository: FruitSponsorRepository
) : ViewModel() {

    val currentYear: Int = Year.now().value

    val yearGroups: StateFlow<List<SponsorYearGroup>> = repository.allSponsors.map { list ->
        list.groupBy { it.jahr }
            .toSortedMap(compareByDescending { it })
            .map { (jahr, items) -> SponsorYearGroup(jahr = jahr, sponsors = items.sortedBy { it.wer }) }
    }.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList()
    )

    val trashedByYear: StateFlow<List<SponsorYearGroup>> = repository.trashedSponsors.map { list ->
        list.groupBy { it.jahr }
            .toSortedMap(compareByDescending { it })
            .map { (jahr, items) -> SponsorYearGroup(jahr = jahr, sponsors = items.sortedBy { it.wer }) }
    }.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList()
    )

    fun addSponsor(jahr: Int, wer: String, sorte: String) {
        viewModelScope.launch { repository.insert(FruitSponsor(jahr = jahr, wer = wer, sorte = sorte)) }
    }

    fun updateSponsor(sponsor: FruitSponsor) {
        viewModelScope.launch { repository.update(sponsor) }
    }

    fun deleteSponsor(sponsor: FruitSponsor) {
        viewModelScope.launch { repository.moveToTrash(sponsor) }
    }

    fun deleteYear(jahr: Int) {
        viewModelScope.launch { repository.moveYearToTrash(jahr) }
    }

    fun restoreSponsor(sponsor: FruitSponsor) {
        viewModelScope.launch { repository.restore(sponsor) }
    }

    fun deleteSponsorPermanently(sponsor: FruitSponsor) {
        viewModelScope.launch { repository.deletePermanently(sponsor) }
    }

    fun restoreYear(jahr: Int) {
        viewModelScope.launch { repository.restoreYear(jahr) }
    }

    fun deleteYearPermanently(jahr: Int) {
        viewModelScope.launch { repository.deleteYearPermanently(jahr) }
    }
}
