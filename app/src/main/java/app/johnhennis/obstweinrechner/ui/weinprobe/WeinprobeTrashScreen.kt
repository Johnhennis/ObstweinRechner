package app.johnhennis.obstweinrechner.ui.weinprobe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.johnhennis.obstweinrechner.data.WeinprobeEntry
import app.johnhennis.obstweinrechner.ui.AppViewModelFactory
import app.johnhennis.obstweinrechner.ui.common.ScaledContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeinprobeTrashScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit
) {
    val viewModel: WeinprobeViewModel = viewModel(factory = factory)
    val trashedByDate by viewModel.trashedByDate.collectAsState()
    var confirmDeleteEntry by remember { mutableStateOf<WeinprobeEntry?>(null) }
    var confirmDeleteDatum by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Papierkorb") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        if (trashedByDate.isEmpty()) {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text("Der Papierkorb ist leer.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                trashedByDate.forEach { group ->
                    item(key = "header_${group.datum}") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${displayDate(group.datum)} (${group.entries.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row {
                                TextButton(onClick = { viewModel.restoreDatum(group.datum) }) { Text("Wiederherstellen") }
                                TextButton(onClick = { confirmDeleteDatum = group.datum }) {
                                    Text("Endgültig löschen", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    items(group.entries, key = { it.id }) { entry ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(entry.sorte, style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { viewModel.restoreEntry(entry) }) { Text("Wiederherstellen") }
                                    TextButton(onClick = { confirmDeleteEntry = entry }) {
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

    confirmDeleteEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmDeleteEntry = null },
            title = { ScaledContent(factory) { Text("Endgültig löschen?") } },
            text = { ScaledContent(factory) { Text("\"${entry.sorte}\" wird unwiderruflich gelöscht.") } },
            confirmButton = {
                ScaledContent(factory) {
                    TextButton(onClick = { viewModel.deleteEntryPermanently(entry); confirmDeleteEntry = null }) {
                        Text("Löschen", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = { ScaledContent(factory) { TextButton(onClick = { confirmDeleteEntry = null }) { Text("Abbrechen") } } }
        )
    }

    confirmDeleteDatum?.let { datum ->
        AlertDialog(
            onDismissRequest = { confirmDeleteDatum = null },
            title = { ScaledContent(factory) { Text("Endgültig löschen?") } },
            text = { ScaledContent(factory) { Text("Alle Einträge vom ${displayDate(datum)} werden unwiderruflich gelöscht.") } },
            confirmButton = {
                ScaledContent(factory) {
                    TextButton(onClick = { viewModel.deleteDatumPermanently(datum); confirmDeleteDatum = null }) {
                        Text("Alle löschen", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = { ScaledContent(factory) { TextButton(onClick = { confirmDeleteDatum = null }) { Text("Abbrechen") } } }
        )
    }
}
