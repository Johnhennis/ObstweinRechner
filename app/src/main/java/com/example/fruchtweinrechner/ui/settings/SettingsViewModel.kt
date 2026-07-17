package com.example.fruchtweinrechner.ui.settings

import androidx.lifecycle.ViewModel
import com.example.fruchtweinrechner.data.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    val fontScale: StateFlow<Float> = repository.fontScale

    fun setFontScale(scale: Float) {
        repository.setFontScale(scale)
    }
}
