package com.example.fruchtweinrechner.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruchtweinrechner.data.InventoryItem
import com.example.fruchtweinrechner.data.InventoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel(
    private val repository: InventoryRepository
) : ViewModel() {

    val items: StateFlow<List<InventoryItem>> = repository.allItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun addItem(item: InventoryItem) {
        viewModelScope.launch { repository.insert(item) }
    }

    fun updateItem(item: InventoryItem) {
        viewModelScope.launch { repository.update(item) }
    }

    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch { repository.delete(item) }
    }
}
