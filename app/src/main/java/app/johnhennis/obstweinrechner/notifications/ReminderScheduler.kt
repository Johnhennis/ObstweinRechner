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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private const val CHANNEL_ID = "wine_order_reminders"
private const val REMINDER_HOUR = 9

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

// Plant eine (nicht-exakte, aber Doze-taugliche) Erinnerung um 9 Uhr morgens
// am "wann"-Datum. Bewusst nicht-exakt (setAndAllowWhileIdle statt
// setExactAndAllowWhileIdle), da das keine zusätzliche, auf neueren Android-
// Versionen oft manuell zu erteilende Berechtigung braucht und für eine
// Tages-Erinnerung völlig ausreicht.
fun scheduleReminder(context: Context, orderId: String, wer: String, sorte: String, wannIso: String) {
    val date = try { LocalDate.parse(wannIso) } catch (e: Exception) { return }
    val triggerMillis = date.atTime(LocalTime.of(REMINDER_HOUR, 0))
        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    if (triggerMillis <= System.currentTimeMillis()) return

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra(ReminderReceiver.EXTRA_WER, wer)
        putExtra(ReminderReceiver.EXTRA_SORTE, sorte)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context, orderId.hashCode(), intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    try {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    } catch (e: SecurityException) {
        // Keine Berechtigung - Erinnerung faellt in diesem Fall aus.
    }
}

fun cancelReminder(context: Context, orderId: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, ReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, orderId.hashCode(), intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
}

fun showReminderNotification(context: Context, wer: String, sorte: String) {
    ensureNotificationChannel(context)
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle("Weinvorbestellung fällig")
        .setContentText("$wer – $sorte")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    try {
        NotificationManagerCompat.from(context).notify((wer + sorte).hashCode(), notification)
    } catch (e: SecurityException) {
        // Keine Benachrichtigungs-Berechtigung erteilt.
    }
}
