package app.johnhennis.obstweinrechner.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel
import app.johnhennis.obstweinrechner.ui.AppViewModelFactory
import app.johnhennis.obstweinrechner.ui.settings.SettingsViewModel

/**
 * Dialoge (AlertDialog) und Popups (DropdownMenu) laufen in einem eigenen
 * Android-Fenster und erben deshalb NICHT automatisch die App-weite
 * Schriftgrößen-Einstellung. Dieser Wrapper liest die Einstellung direkt aus
 * dem SettingsViewModel und wendet sie innerhalb des Dialog-/Popup-Fensters
 * erneut an.
 */
@Composable
fun ScaledContent(factory: AppViewModelFactory, content: @Composable () -> Unit) {
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
    val fontScale by settingsViewModel.fontScale.collectAsState()
    val baseDensity = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, fontScale)) {
        content()
    }
}
