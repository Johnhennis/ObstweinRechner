package app.johnhennis.obstweinrechner.ui.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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
// besteht - bewusst kein Toast, da der nur kurz aufblitzt und die App
// komplett auf Firestore/Internet angewiesen ist. Verschwindet automatisch,
// sobald die Verbindung wieder da ist.
@Composable
fun NoInternetBanner() {
    val context = LocalContext.current
    var isOnline by remember { mutableStateOf(currentlyOnline(context)) }

    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline = true
            }
            override fun onLost(network: Network) {
                isOnline = currentlyOnline(context)
            }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                isOnline = currentlyOnline(context)
            }
        }
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, callback)
        } catch (e: SecurityException) {
            // Fehlende Berechtigung - Banner bleibt dann einfach dauerhaft aus.
        }
        onDispose {
            try {
                connectivityManager?.unregisterNetworkCallback(callback)
            } catch (e: Exception) { }
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
