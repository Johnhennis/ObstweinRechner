package app.johnhennis.obstweinrechner.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.johnhennis.obstweinrechner.data.InventoryRepository
import app.johnhennis.obstweinrechner.data.ManualShoppingItem
import app.johnhennis.obstweinrechner.data.ManualShoppingItemRepository
import app.johnhennis.obstweinrechner.data.ShoppingListRepository
import app.johnhennis.obstweinrechner.data.ShoppingListStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class ShoppingListEntry(
    val itemId: String,
    val name: String,
    val mengeText: String,
    val erledigt: Boolean,
    val quelle: String,
    val manual: Boolean
)

class ShoppingListViewModel(
    private val inventoryRepository: InventoryRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val manualShoppingItemRepository: ManualShoppingItemRepository
) : ViewModel() {

    val entries: StateFlow<List<ShoppingListEntry>> = combine(
        inventoryRepository.allItems,
        shoppingListRepository.allStatus,
        manualShoppingItemRepository.allItems
    ) { items, statusMap, manualItems ->
        val fromInventory = items
            .filter { it.soll - it.ist > 0.0001 }
            .map { item ->
                val diff = item.soll - item.ist
                val status = statusMap[item.id] ?: ShoppingListStatus()
                ShoppingListEntry(
                    itemId = item.id,
                    name = item.name,
                    mengeText = "${fmt(diff)}${if (item.einheit.isBlank()) "" else " ${item.einheit}"}",
                    erledigt = status.erledigt,
                    quelle = status.quelle,
                    manual = false
                )
            }

        val fromManual = manualItems.map { item ->
            ShoppingListEntry(
                itemId = item.id,
                name = item.name,
                mengeText = item.menge,
                erledigt = item.erledigt,
                quelle = item.quelle,
                manual = true
            )
        }

        (fromInventory + fromManual).sortedBy { it.name }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun toggleErledigt(entry: ShoppingListEntry) {
        viewModelScope.launch {
            if (entry.manual) {
                manualShoppingItemRepository.update(
                    ManualShoppingItem(id = entry.itemId, name = entry.name, menge = entry.mengeText, quelle = entry.quelle, erledigt = !entry.erledigt)
                )
            } else {
                shoppingListRepository.setStatus(entry.itemId, ShoppingListStatus(erledigt = !entry.erledigt, quelle = entry.quelle))
            }
        }
    }

    fun updateQuelle(entry: ShoppingListEntry, quelle: String) {
        viewModelScope.launch {
            if (entry.manual) {
                manualShoppingItemRepository.update(
                    ManualShoppingItem(id = entry.itemId, name = entry.name, menge = entry.mengeText, quelle = quelle, erledigt = entry.erledigt)
                )
            } else {
                shoppingListRepository.setStatus(entry.itemId, ShoppingListStatus(erledigt = entry.erledigt, quelle = quelle))
            }
        }
    }

    fun addManualItem(name: String, menge: String, quelle: String) {
        viewModelScope.launch {
            manualShoppingItemRepository.insert(ManualShoppingItem(name = name, menge = menge, quelle = quelle))
        }
    }

    fun deleteManualItem(entry: ShoppingListEntry) {
        if (!entry.manual) return
        viewModelScope.launch {
            manualShoppingItemRepository.delete(ManualShoppingItem(id = entry.itemId))
        }
    }

    fun clearSelection() {
        viewModelScope.launch {
            entries.value.filter { it.erledigt }.forEach { entry ->
                if (entry.manual) {
                    manualShoppingItemRepository.update(
                        ManualShoppingItem(id = entry.itemId, name = entry.name, menge = entry.mengeText, quelle = entry.quelle, erledigt = false)
                    )
                } else {
                    shoppingListRepository.setStatus(entry.itemId, ShoppingListStatus(erledigt = false, quelle = entry.quelle))
                }
            }
        }
    }

    private fun fmt(value: Double): String = String.format(Locale.GERMANY, "%.2f", value)
}
