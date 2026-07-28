package app.johnhennis.obstweinrechner.ui.common

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

private const val PREFS_NAME = "app_prefs"
private const val KEY_LAST_RESULT = "lastUpdateCheckResult"
private const val KEY_LAST_TIME = "lastUpdateCheckTime"

fun readLastUpdateCheck(context: Context): Pair<String, Long>? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val result = prefs.getString(KEY_LAST_RESULT, null) ?: return null
    val time = prefs.getLong(KEY_LAST_TIME, 0L)
    return result to time
}

private fun persistResult(context: Context, result: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_LAST_RESULT, result)
        .putLong(KEY_LAST_TIME, System.currentTimeMillis())
        .apply()
}

// Prüft beim App-Start und bei jeder Rückkehr in den Vordergrund, ob im Play
// Store eine neuere Version vorliegt. Bewusst nur noch "Flexible" (laedt im
// Hintergrund, fragt erst beim Fertigsein nach einem Neustart) - "Sofort"
// killt den Prozess zwangsweise mitten in der Nutzung, was zu abgebrochenen
// Firestore-Schreibvorgaengen und in der Folge zu Duplikaten fuehren kann.
@Composable
fun InAppUpdateChecker() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val appUpdateManager = remember { AppUpdateManagerFactory.create(context) }
    var showRestartDialog by remember { mutableStateOf(false) }

    val flexibleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { }

    fun report(text: String) {
        persistResult(context, text)
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    fun checkAndStart() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                when (info.updateAvailability()) {
                    UpdateAvailability.UPDATE_NOT_AVAILABLE -> {
                        report("Kein Update verfügbar (Version aktuell)")
                    }
                    UpdateAvailability.UPDATE_AVAILABLE, UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                            report("Update gefunden (Play-Version ${info.availableVersionCode()}) – lädt im Hintergrund")
                            val listener = InstallStateUpdatedListener { state ->
                                if (state.installStatus() == InstallStatus.DOWNLOADED) {
                                    report("Update heruntergeladen – bereit zum Neustart")
                                    showRestartDialog = true
                                }
                            }
                            appUpdateManager.registerListener(listener)
                            appUpdateManager.startUpdateFlowForResult(
                                info, flexibleLauncher, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                            )
                        } else {
                            report("Update (Play-Version ${info.availableVersionCode()}) verfügbar, aber von Play aktuell NICHT freigegeben")
                        }
                    }
                    else -> {
                        report("Status unbekannt (${info.updateAvailability()})")
                    }
                }
            }
            .addOnFailureListener { e ->
                report("Update-Check fehlgeschlagen: ${e.message}")
            }
    }

    LaunchedEffect(Unit) { checkAndStart() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) checkAndStart()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text("Update bereit") },
            text = { Text("Das Update wurde heruntergeladen. Jetzt neu starten, um es zu installieren?") },
            confirmButton = {
                TextButton(onClick = {
                    showRestartDialog = false
                    appUpdateManager.completeUpdate()
                }) { Text("Jetzt neu starten") }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) { Text("Später") }
            }
        )
    }
}
