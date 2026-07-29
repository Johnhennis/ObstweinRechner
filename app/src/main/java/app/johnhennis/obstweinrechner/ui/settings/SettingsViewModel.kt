package app.johnhennis.obstweinrechner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.johnhennis.obstweinrechner.data.InfoEntry
import app.johnhennis.obstweinrechner.data.InfoEntryRepository
import app.johnhennis.obstweinrechner.data.SettingsRepository
import app.johnhennis.obstweinrechner.data.ThemeMode
import app.johnhennis.obstweinrechner.data.ThemeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val infoEntryRepository: InfoEntryRepository,
    private val themeRepository: ThemeRepository
) : ViewModel() {

    val fontScale: StateFlow<Float> = repository.fontScale

    fun setFontScale(scale: Float) {
        repository.setFontScale(scale)
    }

    val themeMode: StateFlow<ThemeMode> = themeRepository.themeMode

    fun setThemeMode(mode: ThemeMode) {
        themeRepository.setThemeMode(mode)
    }

    val infoEntries: StateFlow<List<InfoEntry>> = infoEntryRepository.allEntries.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList()
    )

    val trashedInfoEntries: StateFlow<List<InfoEntry>> = infoEntryRepository.trashedEntries.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList()
    )

    fun addInfoEntry(text: String) {
        viewModelScope.launch { infoEntryRepository.insert(text) }
    }

    fun deleteInfoEntry(entry: InfoEntry) {
        viewModelScope.launch { infoEntryRepository.moveToTrash(entry) }
    }

    fun restoreInfoEntry(entry: InfoEntry) {
        viewModelScope.launch { infoEntryRepository.restore(entry) }
    }

    fun deleteInfoEntryPermanently(entry: InfoEntry) {
        viewModelScope.launch { infoEntryRepository.deletePermanently(entry) }
    }

    fun emptyInfoTrash() {
        viewModelScope.launch { infoEntryRepository.emptyTrash() }
    }
}
