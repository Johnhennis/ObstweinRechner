package app.johnhennis.obstweinrechner.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.johnhennis.obstweinrechner.ui.AppViewModelFactory
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    factory: AppViewModelFactory,
    onOpenMenu: () -> Unit
) {
    val viewModel: SettingsViewModel = viewModel(factory = factory)
    val savedFontScale by viewModel.fontScale.collectAsState()

    // Lokaler, sofort reagierender Zustand für flüssiges Ziehen.
    // Erst beim Loslassen wird der Wert app-weit übernommen (siehe onValueChangeFinished
    // unten) - so löst jede Fingerbewegung nicht mehr einen kompletten Neuaufbau
    // der ganzen App aus, was das Ziehen vorher blockiert hat.
    var sliderPosition by remember { mutableFloatStateOf(savedFontScale) }
    LaunchedEffect(savedFontScale) { sliderPosition = savedFontScale }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Schriftgröße", style = MaterialTheme.typography.titleMedium)
            Text(
                "Gilt nur auf diesem Gerät und wirkt sich auf die gesamte App aus.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text("${(sliderPosition * 100).roundToInt()} %", style = MaterialTheme.typography.headlineSmall)

            Slider(
                value = sliderPosition,
                onValueChange = { sliderPosition = it },
                onValueChangeFinished = { viewModel.setFontScale(sliderPosition) },
                valueRange = 0.8f..2.0f
            )

            Text("Beispieltext in aktueller Größe", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
