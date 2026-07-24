package app.johnhennis.obstweinrechner.ui.shopping

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.johnhennis.obstweinrechner.ui.AppViewModelFactory
import app.johnhennis.obstweinrechner.ui.common.ScaledContent
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    factory: AppViewModelFactory,
    onOpenMenu: () -> Unit
) {
    val viewModel: ShoppingListViewModel = viewModel(factory = factory)
    val entries by viewModel.entries.collectAsState()
    var noteTarget by remember { mutableStateOf<ShoppingListEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einkaufsliste") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                }
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text("Alles vorhanden – aktuell besteht kein Nachkaufbedarf.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.itemId }) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { noteTarget = entry }
                    ) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = entry.erledigt, onCheckedChange = { viewModel.toggleErledigt(entry) })
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                val einheitSuffix = if (entry.einheit.isBlank()) "" else " ${entry.einheit}"
                                Text(
                                    "${entry.name}: ${fmt(entry.benoetigt)}$einheitSuffix",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textDecoration = if (entry.erledigt) TextDecoration.LineThrough else TextDecoration.None
                                )
                                if (entry.notiz.isNotBlank()) {
                                    Text(
                                        entry.notiz,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textDecoration = if (entry.erledigt) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    noteTarget?.let { entry ->
        NoteDialog(
            factory = factory,
            entry = entry,
            onDismiss = { noteTarget = null },
            onSave = { text -> viewModel.updateNotiz(entry, text); noteTarget = null }
        )
    }
}

@Composable
private fun NoteDialog(
    factory: AppViewModelFactory,
    entry: ShoppingListEntry,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(entry.notiz) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { ScaledContent(factory) { Text(entry.name) } },
        text = {
            ScaledContent(factory) {
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Notiz") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { ScaledContent(factory) { TextButton(onClick = { onSave(text) }) { Text("Speichern") } } },
        dismissButton = { ScaledContent(factory) { TextButton(onClick = onDismiss) { Text("Abbrechen") } } }
    )
}

private fun fmt(value: Double): String = String.format(Locale.GERMANY, "%.2f", value)
