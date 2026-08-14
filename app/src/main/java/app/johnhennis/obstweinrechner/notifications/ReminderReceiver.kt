package app.johnhennis.obstweinrechner.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_WER = "wer"
        const val EXTRA_SORTE = "sorte"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val wer = intent.getStringExtra(EXTRA_WER) ?: ""
        val sorte = intent.getStringExtra(EXTRA_SORTE) ?: ""
        showReminderNotification(context, wer, sorte)
    }
}
