package app.johnhennis.obstweinrechner.ui.wineorder

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.johnhennis.obstweinrechner.notifications.canScheduleExactAlarms
import app.johnhennis.obstweinrechner.notifications.isIgnoringBatteryOptimizations

// Zeigt einen Hinweis, solange Akku-Optimierung und/oder exakte Alarme
// nicht erlaubt sind - beides sorgt dafuer, dass Android geplante
// Erinnerungen im Hintergrund verzoegert oder erst beim naechsten
// App-Start nachholt. Prueft bei jeder Rueckkehr in den Vordergrund neu
// (z.B. nach einem Ausflug in die Einstellungen).
@Composable
fun ReminderPermissionsBanner() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var batteryOk by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    var exactOk by remember { mutableStateOf(canScheduleExactAlarms(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                batteryOk = isIgnoringBatteryOptimizations(context)
                exactOk = canScheduleExactAlarms(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!batteryOk || !exactOk) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Für zuverlässige Erinnerungen im Hintergrund bitte freigeben:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                if (!batteryOk) {
                    TextButton(onClick = {
                        val intent = Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }) { Text("Akku-Optimierung deaktivieren") }
                }
                if (!exactOk) {
                    TextButton(onClick = {
                        val intent = Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }) { Text("Genaue Alarme erlauben") }
                }
                Text(
                    "Bei Xiaomi/MIUI-Geräten zusätzlich wichtig: Einstellungen → Apps → ObstweinRechner → Autostart aktivieren, Akku-Sparmodus auf \"Keine Einschränkungen\".",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
