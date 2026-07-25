package app.johnhennis.obstweinrechner.ui.common

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
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

// Prüft beim App-Start und bei jeder Rückkehr in den Vordergrund, ob im Play
// Store eine neuere Version vorliegt, und startet automatisch den von Play
// bereitgestellten Immediate-Update-Dialog. Greift nur bei über den Play
// Store installierten Apps - bei den Testern also automatisch der Fall.
@Composable
fun InAppUpdateChecker() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val appUpdateManager = remember { AppUpdateManagerFactory.create(context) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { /* Play übernimmt bei Immediate-Updates Neustart und Installation selbst. */ }

    fun checkAndStart() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            val darfStarten = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
                info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
            if (darfStarten && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    launcher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                )
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
}
