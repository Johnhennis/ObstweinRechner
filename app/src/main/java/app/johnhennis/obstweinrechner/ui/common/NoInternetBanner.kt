package app.johnhennis.obstweinrechner.ui.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private fun currentlyOnline(context: Context): Boolean {
    return try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (e: SecurityException) {
        true
    }
}

// Zeigt einen durchgehenden Streifen, solange keine Internetverbindung
// besteht. Zwei unabhängige Mechanismen kombiniert, da reine Callback-
// Benachrichtigungen (registerNetworkCallback) auf manchen Android-Varianten
// bei einem Verbindungswechsel WÄHREND die App bereits läuft unzuverlässig
// ankommen: registerDefaultNetworkCallback (robuster als die alte
// Capability-gefilterte Variante) für die schnelle Reaktion, zusätzlich
// eine Nachprüfung alle 3 Sekunden als Sicherheitsnetz, das den Zustand so
// oder so irgendwann richtigstellt.
@Composable
fun NoInternetBanner() {
    val context = LocalContext.current
    var isOnline by remember { mutableStateOf(currentlyOnline(context)) }

    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline = currentlyOnline(context)
            }
            override fun onLost(network: Network) {
                isOnline = currentlyOnline(context)
            }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                isOnline = currentlyOnline(context)
            }
            override fun onUnavailable() {
                isOnline = currentlyOnline(context)
            }
        }
        try {
            connectivityManager?.registerDefaultNetworkCallback(callback)
        } catch (e: Exception) {
            // Fehlende Berechtigung o.ä. - die 3-Sekunden-Nachprüfung unten
            // fängt das trotzdem auf, nur etwas verzögert statt sofort.
        }
        onDispose {
            try {
                connectivityManager?.unregisterNetworkCallback(callback)
            } catch (e: Exception) { }
        }
    }

    LaunchedEffect(context) {
        while (true) {
            delay(3000)
            val actual = currentlyOnline(context)
            if (actual != isOnline) isOnline = actual
        }
    }

    AnimatedVisibility(visible = !isOnline) {
        Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Keine Internetverbindung – Änderungen werden gespeichert, sobald sie wieder da ist",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 12.dp)
            )
        }
    }
}
