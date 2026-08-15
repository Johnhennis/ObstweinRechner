package app.johnhennis.obstweinrechner.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
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

private fun sortenText(order: WineOrder): String =
    order.positionen.joinToString(", ") { it.sorte }.ifBlank { "Wein" }

private fun pendingIntentFor(context: Context, order: WineOrder, offsetHours: Int): PendingIntent {
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra(ReminderReceiver.EXTRA_WER, order.wer)
        putExtra(ReminderReceiver.EXTRA_SORTE, sortenText(order))
        putExtra(ReminderReceiver.EXTRA_OFFSET, offsetHours)
    }
    return PendingIntent.getBroadcast(
        context, requestCode(order.id, offsetHours), intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

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

// setSmallIcon() erzwingt seitens Android IMMER eine reine Silhouette in der
// Statusleiste, egal welches Bild man uebergibt - das laesst sich nicht
// umgehen. setLargeIcon() dagegen zeigt ein echtes Farbbild innerhalb der
// aufgeklappten Benachrichtigung - dort erscheint jetzt das tatsaechliche,
// bunte App-Icon statt der Standard-Glocke. applicationInfo.icon fragt
// direkt das aktuell konfigurierte Launcher-Icon ab, unabhaengig vom
// genauen Ressourcennamen.
fun showReminderNotification(context: Context, wer: String, sortenText: String, offsetHours: Int) {
    ensureNotificationChannel(context)
    val zeitText = if (offsetHours == 1) "in 1 Stunde" else "in $offsetHours Stunden"
    val appIconRes = context.applicationInfo.icon
    val largeIcon = try {
        BitmapFactory.decodeResource(context.resources, appIconRes)
    } catch (e: Exception) {
        null
    }
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(appIconRes)
        .setContentTitle("Weinvorbestellung $zeitText fällig")
        .setContentText("$wer – $sortenText")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
    if (largeIcon != null) builder.setLargeIcon(largeIcon)
    try {
        NotificationManagerCompat.from(context).notify((wer + sortenText + offsetHours).hashCode(), builder.build())
    } catch (e: SecurityException) {
        // Keine Benachrichtigungs-Berechtigung erteilt.
    }
}
