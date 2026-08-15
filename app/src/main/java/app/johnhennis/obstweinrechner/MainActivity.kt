package app.johnhennis.obstweinrechner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel
import app.johnhennis.obstweinrechner.data.ThemeMode
import app.johnhennis.obstweinrechner.notifications.EXTRA_OPEN_SCREEN
import app.johnhennis.obstweinrechner.ui.AppViewModelFactory
import app.johnhennis.obstweinrechner.ui.navigation.AppNavigation
import app.johnhennis.obstweinrechner.ui.settings.SettingsViewModel
import app.johnhennis.obstweinrechner.ui.theme.FruchtweinRechnerTheme

class MainActivity : ComponentActivity() {

    // Ausserhalb von setContent, damit onNewIntent() (kein Composable) den
    // Wert setzen kann, waehrend die Compose-Oberflaeche ihn reaktiv liest.
    private val pendingOpenScreen = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingOpenScreen.value = intent?.getStringExtra(EXTRA_OPEN_SCREEN)

        val application = application as FruchtweinApplication
        val factory = AppViewModelFactory.from(application)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
            val fontScale by settingsViewModel.fontScale.collectAsState()
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val baseDensity = LocalDensity.current
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDark
            }

            FruchtweinRechnerTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, fontScale)) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavigation(factory = factory, pendingOpenScreen = pendingOpenScreen)
                    }
                }
            }
        }
    }

    // Wird aufgerufen, wenn die App schon im Hintergrund laeuft und die
    // Benachrichtigung angetippt wird (statt eines kompletten Neustarts).
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingOpenScreen.value = intent.getStringExtra(EXTRA_OPEN_SCREEN)
    }
}
