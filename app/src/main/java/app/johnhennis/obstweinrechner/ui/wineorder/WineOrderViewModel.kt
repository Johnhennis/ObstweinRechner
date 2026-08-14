package app.johnhennis.obstweinrechner.ui.wineorder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.johnhennis.obstweinrechner.data.WineOrder
import app.johnhennis.obstweinrechner.data.WineOrderRepository
import app.johnhennis.obstweinrechner.notifications.cancelReminder
import app.johnhennis.obstweinrechner.notifications.scheduleReminder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Year

data class WineOrderYearGroup(
    val jahr: Int,
    val orders: List<WineOrder>
)

class WineOrderViewModel(
    private val repository: WineOrderRepository
) : ViewModel() {

    val currentYear: Int = Year.now().value

    val yearGroups: StateFlow<List<WineOrderYearGroup>> = repository.allOrders.map { orders ->
        orders.groupBy { it.jahr }
            .toSortedMap(compareByDescending { it })
            .map { (jahr, list) -> WineOrderYearGroup(jahr = jahr, orders = list.sortedBy { it.wer }) }
    }.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList()
    )

    val trashedByYear: StateFlow<List<WineOrderYearGroup>> = repository.trashedOrders.map { orders ->
        orders.groupBy { it.jahr }
            .toSortedMap(compareByDescending { it })
            .map { (jahr, list) -> WineOrderYearGroup(jahr = jahr, orders = list.sortedBy { it.wer }) }
    }.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList()
    )

    fun addOrder(context: Context, jahr: Int, wer: String, sorte: String, menge: Double, wannDatum: String) {
        viewModelScope.launch {
            val id = repository.insert(WineOrder(jahr = jahr, wer = wer, sorte = sorte, menge = menge, wannDatum = wannDatum))
            if (wannDatum.isNotBlank()) scheduleReminder(context, id, wer, sorte, wannDatum)
        }
    }

    fun updateOrder(context: Context, order: WineOrder) {
        viewModelScope.launch {
            repository.update(order)
            if (order.abgeholt || order.wannDatum.isBlank()) {
                cancelReminder(context, order.id)
            } else {
                scheduleReminder(context, order.id, order.wer, order.sorte, order.wannDatum)
            }
        }
    }

    fun deleteOrder(context: Context, order: WineOrder) {
        viewModelScope.launch {
            cancelReminder(context, order.id)
            repository.moveToTrash(order)
        }
    }

    fun deleteYear(context: Context, jahr: Int) {
        viewModelScope.launch {
            yearGroups.value.find { it.jahr == jahr }?.orders?.forEach { cancelReminder(context, it.id) }
            repository.moveYearToTrash(jahr)
        }
    }

    fun restoreOrder(context: Context, order: WineOrder) {
        viewModelScope.launch {
            repository.restore(order)
            if (!order.abgeholt && order.wannDatum.isNotBlank()) {
                scheduleReminder(context, order.id, order.wer, order.sorte, order.wannDatum)
            }
        }
    }

    fun deleteOrderPermanently(order: WineOrder) {
        viewModelScope.launch { repository.deletePermanently(order) }
    }

    fun restoreYear(context: Context, jahr: Int) {
        viewModelScope.launch {
            repository.restoreYear(jahr)
            trashedByYear.value.find { it.jahr == jahr }?.orders?.forEach { order ->
                if (!order.abgeholt && order.wannDatum.isNotBlank()) {
                    scheduleReminder(context, order.id, order.wer, order.sorte, order.wannDatum)
                }
            }
        }
    }

    fun deleteYearPermanently(jahr: Int) {
        viewModelScope.launch { repository.deleteYearPermanently(jahr) }
    }

    fun rescheduleAllPending(context: Context) {
        yearGroups.value.forEach { group ->
            group.orders.forEach { order ->
                if (!order.abgeholt && order.wannDatum.isNotBlank()) {
                    scheduleReminder(context, order.id, order.wer, order.sorte, order.wannDatum)
                }
            }
        }
    }
}
