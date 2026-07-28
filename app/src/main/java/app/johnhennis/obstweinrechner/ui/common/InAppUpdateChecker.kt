package app.johnhennis.obstweinrechner.ui.common

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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

// Liest das Ergebnis des letzten Update-Checks (fuer die Anzeige in den
// Einstellungen) - dauerhaft statt eines fluechtigen Toasts, damit man es
// auch nachtraeglich noch nachschauen kann.
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
// Store eine neuere Version vorliegt. Funktioniert NUR bei einer über den
// Play Store installierten App. Jedes Ergebnis wird sowohl kurz als Toast
// gezeigt als auch dauerhaft gespeichert (sichtbar in den Einstellungen),
// damit ein verpasster Toast nicht mehr die einzige Informationsquelle ist.
@Composable
fun InAppUpdateChecker() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val appUpdateManager = remember { AppUpdateManagerFactory.create(context) }

    val immediateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { }
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
                        when {
                            info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> {
                                report("Update gefunden (Play-Version ${info.availableVersionCode()}) – wird installiert")
                                appUpdateManager.startUpdateFlowForResult(
                                    info, immediateLauncher, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                                )
                            }
                            info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                                report("Update gefunden (Play-Version ${info.availableVersionCode()}) – lädt im Hintergrund")
                                val listener = InstallStateUpdatedListener { state ->
                                    if (state.installStatus() == InstallStatus.DOWNLOADED) {
                                        report("Update heruntergeladen – wird installiert")
                                        appUpdateManager.completeUpdate()
                                    }
                                }
                                appUpdateManager.registerListener(listener)
                                appUpdateManager.startUpdateFlowForResult(
                                    info, flexibleLauncher, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                                )
                            }
                            else -> {
                                report("Update (Play-Version ${info.availableVersionCode()}) verfügbar, aber von Play aktuell NICHT freigegeben")
                            }
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
}
