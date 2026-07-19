package com.example.fruchtweinrechner.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruchtweinrechner.data.InventoryRepository
import com.example.fruchtweinrechner.data.ShoppingListRepository
import com.example.fruchtweinrechner.data.ShoppingListStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShoppingListEntry(
    val itemId: String,
    val name: String,
    val einheit: String,
    val benoetigt: Double,
    val erledigt: Boolean,
    val notiz: String
)

class ShoppingListViewModel(
    private val inventoryRepository: InventoryRepository,
    private val shoppingListRepository: ShoppingListRepository
) : ViewModel() {

    val entries: StateFlow<List<ShoppingListEntry>> = combine(
        inventoryRepository.allItems,
        shoppingListRepository.allStatus
    ) { items, statusMap ->
        items
            .map { item ->
                val diff = item.soll - item.ist
                val status = statusMap[item.id] ?: ShoppingListStatus()
                ShoppingListEntry(
                    itemId = item.id,
                    name = item.name,
                    einheit = item.einheit,
                    benoetigt = diff,
                    erledigt = status.erledigt,
                    notiz = status.notiz
                )
            }
            .filter { it.benoetigt > 0.0001 }
            .sortedBy { it.name }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun toggleErledigt(entry: ShoppingListEntry) {
        viewModelScope.launch {
            shoppingListRepository.setStatus(entry.itemId, ShoppingListStatus(erledigt = !entry.erledigt, notiz = entry.notiz))
        }
    }

    fun updateNotiz(entry: ShoppingListEntry, notiz: String) {
        viewModelScope.launch {
            shoppingListRepository.setStatus(entry.itemId, ShoppingListStatus(erledigt = entry.erledigt, notiz = notiz))
        }
    }
}
