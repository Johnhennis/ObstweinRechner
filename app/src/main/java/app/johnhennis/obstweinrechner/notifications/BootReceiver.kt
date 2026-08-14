package app.johnhennis.obstweinrechner.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Android verwirft alle per AlarmManager geplanten Erinnerungen bei einem
// Geräte-Neustart. Dieser Empfänger liest beim Hochfahren alle offenen
// Weinvorbestellungen mit gesetztem Termin erneut aus Firestore und plant
// die Erinnerungen neu. Zusätzliche Absicherung: WineOrderViewModel plant
// beim Öffnen des Tabs ebenfalls alles neu, falls hier z.B. mangels Netz
// beim Hochfahren nichts ankommt.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = FirebaseFirestore.getInstance().collection("wineOrders").get().await()
                snapshot.documents.forEach { doc ->
                    val geloescht = doc.getBoolean("geloescht") == true
                    val abgeholt = doc.getBoolean("abgeholt") == true
                    val wannDatum = doc.getString("wannDatum") ?: ""
                    if (!geloescht && !abgeholt && wannDatum.isNotBlank()) {
                        scheduleReminder(appContext, doc.id, doc.getString("wer") ?: "", doc.getString("sorte") ?: "", wannDatum)
                    }
                }
            } catch (e: Exception) {
                // Kein Netz beim Hochfahren - Absicherung s.o.
            } finally {
                pendingResult.finish()
            }
        }
    }
}
