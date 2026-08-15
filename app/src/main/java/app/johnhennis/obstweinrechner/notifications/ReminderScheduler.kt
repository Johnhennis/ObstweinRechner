package app.johnhennis.obstweinrechner.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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

fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

fun canScheduleExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
    return am.canScheduleExactAlarms()
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
    val exactErlaubt = canScheduleExactAlarms(context)
    order.erinnerungenStunden.distinct().filter { it in 1..24 }.forEach { hours ->
        val triggerMillis = termin.minusHours(hours.toLong())
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (triggerMillis > System.currentTimeMillis()) {
            try {
                val pendingIntent = pendingIntentFor(context, order, hours)
                if (exactErlaubt) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } catch (e: SecurityException) {
                // Keine Berechtigung - diese Erinnerung faellt aus.
            }
        }
    }
}

// BitmapFactory.decodeResource() kann grundsaetzlich KEINE Vektor-/Adaptive-
// Icons lesen (nur klassische PNG/JPEG) - bei einem Vektor-Icon wie unserem
// gab das bisher still "null" zurueck, das grosse Icon erschien nie. Richtige
// Methode: das Drawable ganz normal laden (das kennt Vektoren/Adaptive Icons)
// und selbst auf eine Bitmap-Leinwand zeichnen.
private fun appIconAsBitmap(context: Context, size: Int = 192): Bitmap? {
    val drawable = ContextCompat.getDrawable(context, context.applicationInfo.icon) ?: return null
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

fun showReminderNotification(context: Context, wer: String, sortenText: String, offsetHours: Int) {
    ensureNotificationChannel(context)
    val zeitText = if (offsetHours == 1) "in 1 Stunde" else "in $offsetHours Stunden"
    val largeIcon = appIconAsBitmap(context)
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(context.applicationInfo.icon)
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
