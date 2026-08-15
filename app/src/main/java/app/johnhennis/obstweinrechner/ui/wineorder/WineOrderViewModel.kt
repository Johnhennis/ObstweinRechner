package app.johnhennis.obstweinrechner.ui.wineorder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.johnhennis.obstweinrechner.data.WineOrder
import app.johnhennis.obstweinrechner.data.WineOrderRepository
import app.johnhennis.obstweinrechner.notifications.cancelAllPossibleReminders
import app.johnhennis.obstweinrechner.notifications.scheduleReminders
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

    fun addOrder(
        context: Context, jahr: Int, wer: String, sorte: String, menge: Double,
        wannZeitpunkt: String, erinnerungenStunden: List<Int>
    ) {
        viewModelScope.launch {
            val order = WineOrder(
                jahr = jahr, wer = wer, sorte = sorte, menge = menge,
                wannZeitpunkt = wannZeitpunkt, erinnerungenStunden = erinnerungenStunden
            )
            val id = repository.insert(order)
            scheduleReminders(context, order.copy(id = id))
        }
    }

    // Deckt sowohl Feldaenderungen als auch Termin-/Erinnerungs-Aenderungen
    // ab - scheduleReminders() storniert intern immer erst alles und plant
    // dann neu, das ist also auch fuer nachtraegliches Bearbeiten sicher.
    fun updateOrder(context: Context, order: WineOrder) {
        viewModelScope.launch {
            repository.update(order)
            scheduleReminders(context, order)
        }
    }

    fun deleteOrder(context: Context, order: WineOrder) {
        viewModelScope.launch {
            cancelAllPossibleReminders(context, order.id)
            repository.moveToTrash(order)
        }
    }

    fun deleteYear(context: Context, jahr: Int) {
        viewModelScope.launch {
            yearGroups.value.find { it.jahr == jahr }?.orders?.forEach { cancelAllPossibleReminders(context, it.id) }
            repository.moveYearToTrash(jahr)
        }
    }

    fun restoreOrder(context: Context, order: WineOrder) {
        viewModelScope.launch {
            repository.restore(order)
            scheduleReminders(context, order)
        }
    }

    fun deleteOrderPermanently(context: Context, order: WineOrder) {
        viewModelScope.launch {
            cancelAllPossibleReminders(context, order.id)
            repository.deletePermanently(order)
        }
    }

    fun restoreYear(context: Context, jahr: Int) {
        viewModelScope.launch {
            repository.restoreYear(jahr)
            trashedByYear.value.find { it.jahr == jahr }?.orders?.forEach { scheduleReminders(context, it) }
        }
    }

    fun deleteYearPermanently(context: Context, jahr: Int) {
        viewModelScope.launch {
            trashedByYear.value.find { it.jahr == jahr }?.orders?.forEach { cancelAllPossibleReminders(context, it.id) }
            repository.deleteYearPermanently(jahr)
        }
    }

    fun rescheduleAllPending(context: Context) {
        yearGroups.value.forEach { group -> group.orders.forEach { order -> scheduleReminders(context, order) } }
    }
}
