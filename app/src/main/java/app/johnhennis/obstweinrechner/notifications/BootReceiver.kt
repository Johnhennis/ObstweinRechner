package app.johnhennis.obstweinrechner.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.johnhennis.obstweinrechner.data.WineOrder
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Android verwirft alle per AlarmManager geplanten Erinnerungen bei einem
// Geraete-Neustart. Dieser Empfaenger liest beim Hochfahren alle offenen
// Weinvorbestellungen erneut aus Firestore und plant ihre Erinnerungen neu.
// Zusaetzliche Absicherung: WineOrderViewModel plant beim Oeffnen des Tabs
// ebenfalls alles neu, falls hier z.B. mangels Netz nichts ankommt.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = FirebaseFirestore.getInstance().collection("wineOrders").get().await()
                snapshot.documents.forEach { doc ->
                    val order = doc.toObject(WineOrder::class.java)?.copy(id = doc.id) ?: return@forEach
                    if (!order.geloescht) scheduleReminders(appContext, order)
                }
            } catch (e: Exception) {
                // Kein Netz beim Hochfahren - Absicherung s.o.
            } finally {
                pendingResult.finish()
            }
        }
    }
}
