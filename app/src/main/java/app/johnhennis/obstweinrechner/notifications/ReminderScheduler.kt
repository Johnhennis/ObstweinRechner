package app.johnhennis.obstweinrechner.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.johnhennis.obstweinrechner.data.WineOrder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val CHANNEL_ID = "wine_order_reminders"
val WANN_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Weinvorbestellung-Erinnerungen", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Erinnerung an den Termin einer Weinvorbestellung"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}

fun parseWannZeitpunkt(value: String): LocalDateTime? = try {
    LocalDateTime.parse(value, WANN_FORMAT)
} catch (e: Exception) {
    null
}

private fun requestCode(orderId: String, offsetHours: Int): Int = "${orderId}_$offsetHours".hashCode()

private fun pendingIntentFor(context: Context, order: WineOrder, offsetHours: Int): PendingIntent {
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra(ReminderReceiver.EXTRA_WER, order.wer)
        putExtra(ReminderReceiver.EXTRA_SORTE, order.sorte)
        putExtra(ReminderReceiver.EXTRA_OFFSET, offsetHours)
    }
    return PendingIntent.getBroadcast(
        context, requestCode(order.id, offsetHours), intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

// Storniert vorsorglich ALLE denkbaren Erinnerungs-Zeitpunkte (1-24 Stunden
// vorher) fuer diese Bestellung - ein Abbrechen eines nie gesetzten Alarms
// ist wirkungslos. So muss nirgendwo extra mitgefuehrt werden, welche
// Erinnerungen zuvor genau geplant waren, wenn sich die Auswahl aendert.
fun cancelAllPossibleReminders(context: Context, orderId: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    (1..24).forEach { hours ->
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode(orderId, hours), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}

// Plant alle in order.erinnerungenStunden konfigurierten Erinnerungen neu.
// Storniert vorher immer alles - so bleibt es korrekt, egal ob sich seit
// dem letzten Planen der Termin, die Erinnerungsliste oder "abgeholt"
// geaendert hat. Kann daher nach JEDER Aenderung einfach erneut aufgerufen
// werden, ohne die vorherige Planung extra nachzuhalten.
fun scheduleReminders(context: Context, order: WineOrder) {
    cancelAllPossibleReminders(context, order.id)
    if (order.abgeholt) return
    val termin = parseWannZeitpunkt(order.wannZeitpunkt) ?: return
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    order.erinnerungenStunden.distinct().filter { it in 1..24 }.forEach { hours ->
        val triggerMillis = termin.minusHours(hours.toLong())
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (triggerMillis > System.currentTimeMillis()) {
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntentFor(context, order, hours))
            } catch (e: SecurityException) {
                // Keine Berechtigung - diese Erinnerung faellt aus.
            }
        }
    }
}

fun showReminderNotification(context: Context, wer: String, sorte: String, offsetHours: Int) {
    ensureNotificationChannel(context)
    val zeitText = if (offsetHours == 1) "in 1 Stunde" else "in $offsetHours Stunden"
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle("Weinvorbestellung $zeitText fällig")
        .setContentText("$wer – $sorte")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    try {
        NotificationManagerCompat.from(context).notify((wer + sorte + offsetHours).hashCode(), notification)
    } catch (e: SecurityException) {
        // Keine Benachrichtigungs-Berechtigung erteilt.
    }
}
