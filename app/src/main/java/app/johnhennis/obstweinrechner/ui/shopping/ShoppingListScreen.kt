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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateMapOf
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

private const val UNKATEGORISIERT = "Unkategorisiert"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    factory: AppViewModelFactory,
    onOpenMenu: () -> Unit
) {
    val viewModel: ShoppingListViewModel = viewModel(factory = factory)
    val entries by viewModel.entries.collectAsState()
    var quelleTarget by remember { mutableStateOf<ShoppingListEntry?>(null) }
    var showAddManual by remember { mutableStateOf(false) }
    var confirmDeleteManual by remember { mutableStateOf<ShoppingListEntry?>(null) }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    val groups = remember(entries) {
        val map = entries.groupBy { it.quelle.ifBlank { UNKATEGORISIERT } }
        val sortedKeys = map.keys.filter { it != UNKATEGORISIERT }.sorted() +
            (if (map.containsKey(UNKATEGORISIERT)) listOf(UNKATEGORISIERT) else emptyList())
        sortedKeys.associateWith { map.getValue(it) }
    }
    val hatAuswahl = entries.any { it.erledigt }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einkaufsliste") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                actions = {
                    if (hatAuswahl) {
                        TextButton(onClick = { viewModel.clearSelection() }) { Text("Auswahl aufheben") }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddManual = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Position hinzufügen")
            }
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
                groups.forEach { (quelle, groupEntries) ->
                    val isExpanded = expanded[quelle] ?: true
                    item(key = "header_$quelle") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded[quelle] = !isExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "$quelle (${groupEntries.size})",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Icon(
                                if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }
                    if (isExpanded) {
                        items(groupEntries, key = { it.itemId }) { entry ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { quelleTarget = entry }
                            ) {
                                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = entry.erledigt, onCheckedChange = { viewModel.toggleErledigt(entry) })
                                    Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
                                        val mengeSuffix = if (entry.mengeText.isBlank()) "" else ": ${entry.mengeText}"
                                        Text(
                                            "${entry.name}$mengeSuffix",
                                            style = MaterialTheme.typography.bodyLarge,
                                            textDecoration = if (entry.erledigt) TextDecoration.LineThrough else TextDecoration.None
                                        )
                                    }
                                    if (entry.manual) {
                                        IconButton(onClick = { confirmDeleteManual = entry }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Entfernen")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    quelleTarget?.let { entry ->
        QuelleDialog(
            factory = factory,
            entry = entry,
            onDismiss = { quelleTarget = null },
            onSave = { text -> viewModel.updateQuelle(entry, text); quelleTarget = null }
        )
    }

    if (showAddManual) {
        AddManualItemDialog(
            factory = factory,
            onDismiss = { showAddManual = false },
            onAdd = { name, menge, quelle -> viewModel.addManualItem(name, menge, quelle); showAddManual = false }
        )
    }

    confirmDeleteManual?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmDeleteManual = null },
            title = { ScaledContent(factory) { Text("Endgültig löschen?") } },
            text = { ScaledContent(factory) { Text("\"${entry.name}\" hat keinen Papierkorb und wird sofort unwiderruflich entfernt.") } },
            confirmButton = {
                ScaledContent(factory) {
                    TextButton(onClick = { viewModel.deleteManualItem(entry); confirmDeleteManual = null }) {
                        Text("Löschen", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = { ScaledContent(factory) { TextButton(onClick = { confirmDeleteManual = null }) { Text("Abbrechen") } } }
        )
    }
}

@Composable
private fun QuelleDialog(
    factory: AppViewModelFactory,
    entry: ShoppingListEntry,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(entry.quelle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { ScaledContent(factory) { Text(entry.name) } },
        text = {
            ScaledContent(factory) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Quelle (z. B. Seidel, BÄKO, Rewe)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { ScaledContent(factory) { TextButton(onClick = { onSave(text) }) { Text("Speichern") } } },
        dismissButton = { ScaledContent(factory) { TextButton(onClick = onDismiss) { Text("Abbrechen") } } }
    )
}

@Composable
private fun AddManualItemDialog(
    factory: AppViewModelFactory,
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var menge by remember { mutableStateOf("") }
    var quelle by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { ScaledContent(factory) { Text("Position hinzufügen") } },
        text = {
            ScaledContent(factory) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = menge, onValueChange = { menge = it }, label = { Text("Menge (frei, z. B. 2 Pakete)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = quelle, onValueChange = { quelle = it }, label = { Text("Quelle") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            ScaledContent(factory) {
                TextButton(onClick = {
                    if (name.isBlank()) { error = "Bitte einen Namen eingeben."; return@TextButton }
                    onAdd(name.trim(), menge.trim(), quelle.trim())
                }) { Text("Hinzufügen") }
            }
        },
        dismissButton = { ScaledContent(factory) { TextButton(onClick = onDismiss) { Text("Abbrechen") } } }
    )
}
