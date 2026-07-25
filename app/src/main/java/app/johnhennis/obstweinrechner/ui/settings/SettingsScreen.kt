package app.johnhennis.obstweinrechner.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.johnhennis.obstweinrechner.data.InfoEntry
import app.johnhennis.obstweinrechner.ui.AppViewModelFactory
import app.johnhennis.obstweinrechner.ui.common.ScaledContent
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    factory: AppViewModelFactory,
    onOpenMenu: () -> Unit
) {
    val viewModel: SettingsViewModel = viewModel(factory = factory)
    val savedFontScale by viewModel.fontScale.collectAsState()
    val infoEntries by viewModel.infoEntries.collectAsState()
    val trashedInfoEntries by viewModel.trashedInfoEntries.collectAsState()

    var sliderPosition by remember { mutableFloatStateOf(savedFontScale) }
    LaunchedEffect(savedFontScale) { sliderPosition = savedFontScale }

    var showAddInfo by remember { mutableStateOf(false) }
    var showTrash by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<InfoEntry?>(null) }

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

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Informationen", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { showAddInfo = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Eintrag hinzufügen")
                }
            }

            if (infoEntries.isEmpty()) {
                Text(
                    "Noch keine Einträge.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                infoEntries.forEach { entry ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(entry.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { confirmDelete = entry }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Entfernen")
                            }
                        }
                    }
                }
            }

            TextButton(onClick = { showTrash = !showTrash }) {
                Text(if (showTrash) "Papierkorb ausblenden" else "Papierkorb (${trashedInfoEntries.size})")
            }

            if (showTrash) {
                if (trashedInfoEntries.isEmpty()) {
                    Text("Der Papierkorb ist leer.", style = MaterialTheme.typography.bodySmall)
                } else {
                    TextButton(onClick = { viewModel.emptyInfoTrash() }) {
                        Text("Papierkorb leeren", color = MaterialTheme.colorScheme.error)
                    }
                    trashedInfoEntries.forEach { entry ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(entry.text, style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { viewModel.restoreInfoEntry(entry) }) { Text("Wiederherstellen") }
                                    TextButton(onClick = { viewModel.deleteInfoEntryPermanently(entry) }) {
                                        Text("Endgültig löschen", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddInfo) {
        AddInfoDialog(
            factory = factory,
            onDismiss = { showAddInfo = false },
            onAdd = { text -> viewModel.addInfoEntry(text); showAddInfo = false }
        )
    }

    confirmDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { ScaledContent(factory) { Text("In den Papierkorb verschieben?") } },
            text = { ScaledContent(factory) { Text("Der Eintrag kann im Papierkorb wiederhergestellt werden.") } },
            confirmButton = {
                ScaledContent(factory) {
                    TextButton(onClick = { viewModel.deleteInfoEntry(entry); confirmDelete = null }) {
                        Text("Verschieben", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = { ScaledContent(factory) { TextButton(onClick = { confirmDelete = null }) { Text("Abbrechen") } } }
        )
    }
}

@Composable
private fun AddInfoDialog(
    factory: AppViewModelFactory,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { ScaledContent(factory) { Text("Neuer Eintrag") } },
        text = {
            ScaledContent(factory) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Text") }, modifier = Modifier.fillMaxWidth())
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            ScaledContent(factory) {
                TextButton(onClick = {
                    if (text.isBlank()) { error = "Bitte einen Text eingeben."; return@TextButton }
                    onAdd(text.trim())
                }) { Text("Hinzufügen") }
            }
        },
        dismissButton = { ScaledContent(factory) { TextButton(onClick = onDismiss) { Text("Abbrechen") } } }
    )
}
