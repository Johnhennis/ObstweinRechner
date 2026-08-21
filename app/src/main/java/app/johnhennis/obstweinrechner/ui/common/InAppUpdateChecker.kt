package app.johnhennis.obstweinrechner.ui.common

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

// Prueft beim App-Start und bei jeder Rueckkehr in den Vordergrund still im
// Hintergrund auf ein neues Play-Store-Update.
@Composable
fun InAppUpdateChecker() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val appUpdateManager = remember { AppUpdateManagerFactory.create(context) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var listenerRegistered by remember { mutableStateOf(false) }

    val flexibleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { }

    fun ensureListenerRegistered() {
        if (listenerRegistered) return
        appUpdateManager.registerListener(InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                showRestartDialog = true
            }
        })
        listenerRegistered = true
    }

    fun checkAndStart() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            when (info.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        ensureListenerRegistered()
                        appUpdateManager.startUpdateFlowForResult(
                            info, flexibleLauncher, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                        )
                    }
                }
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    // Laeuft schon (Nutzer hat "Aktualisieren" bereits
                    // angetippt) - NICHT erneut den Bestaetigungsdialog
                    // starten, sonst erscheint er ein zweites Mal direkt
                    // hintereinander beim Zurueckkehren zur App. Nur
                    // sicherstellen, dass wir benachrichtigt werden,
                    // sobald der Download fertig ist.
                    ensureListenerRegistered()
                    if (info.installStatus() == InstallStatus.DOWNLOADED) {
                        showRestartDialog = true
                    }
                }
                else -> { /* kein Update verfuegbar - nichts zu tun */ }
            }
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
