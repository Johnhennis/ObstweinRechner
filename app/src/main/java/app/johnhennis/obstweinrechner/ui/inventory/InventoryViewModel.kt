package app.johnhennis.obstweinrechner.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.johnhennis.obstweinrechner.data.InventoryItem
import app.johnhennis.obstweinrechner.data.InventoryRepository
import app.johnhennis.obstweinrechner.data.ShoppingListRepository
import app.johnhennis.obstweinrechner.data.ShoppingListStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel(
    private val repository: InventoryRepository,
    private val shoppingListRepository: ShoppingListRepository
) : ViewModel() {

    val items: StateFlow<List<InventoryItem>> = repository.allItems.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList()
    )

    val trashedItems: StateFlow<List<InventoryItem>> = repository.trashedItems.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList()
    )

    val statusMap: StateFlow<Map<String, ShoppingListStatus>> = shoppingListRepository.allStatus.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyMap()
    )

    fun addItem(item: InventoryItem, quelle: String) {
        viewModelScope.launch {
            val id = repository.insert(item)
            if (quelle.isNotBlank()) {
                shoppingListRepository.setStatus(id, ShoppingListStatus(quelle = quelle))
            }
        }
    }

    fun updateItem(item: InventoryItem) {
        viewModelScope.launch { repository.update(item) }
    }

    fun updateQuelle(itemId: String, quelle: String) {
        viewModelScope.launch {
            val current = statusMap.value[itemId] ?: ShoppingListStatus()
            shoppingListRepository.setStatus(itemId, current.copy(quelle = quelle))
        }
    }

    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch { repository.moveToTrash(item) }
    }

    fun restore(item: InventoryItem) {
        viewModelScope.launch { repository.restore(item) }
    }

    fun deletePermanently(item: InventoryItem) {
        viewModelScope.launch { repository.deletePermanently(item) }
    }

    fun emptyTrash() {
        viewModelScope.launch { repository.emptyTrash() }
    }
}
