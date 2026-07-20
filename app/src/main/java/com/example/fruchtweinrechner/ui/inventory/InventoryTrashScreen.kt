package com.example.fruchtweinrechner.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fruchtweinrechner.data.InventoryItem
import com.example.fruchtweinrechner.ui.AppViewModelFactory
import com.example.fruchtweinrechner.ui.common.ScaledContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryTrashScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit
) {
    val viewModel: InventoryViewModel = viewModel(factory = factory)
    val trashed by viewModel.trashedItems.collectAsState()
    var confirmDelete by remember { mutableStateOf<InventoryItem?>(null) }
    var confirmEmpty by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Papierkorb") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    if (trashed.isNotEmpty()) {
                        IconButton(onClick = { confirmEmpty = true }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Papierkorb leeren")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (trashed.isEmpty()) {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text("Der Papierkorb ist leer.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trashed, key = { it.id }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { viewModel.restore(item) }) { Text("Wiederherstellen") }
                                TextButton(onClick = { confirmDelete = item }) {
                                    Text("Endgültig löschen", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    confirmDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { ScaledContent(factory) { Text("Endgültig löschen?") } },
            text = { ScaledContent(factory) { Text("\"${item.name}\" wird unwiderruflich gelöscht.") } },
            confirmButton = {
                ScaledContent(factory) {
                    TextButton(onClick = { viewModel.deletePermanently(item); confirmDelete = null }) {
                        Text("Löschen", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = { ScaledContent(factory) { TextButton(onClick = { confirmDelete = null }) { Text("Abbrechen") } } }
        )
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            title = { ScaledContent(factory) { Text("Papierkorb leeren?") } },
            text = { ScaledContent(factory) { Text("Alle ${trashed.size} Positionen im Papierkorb werden unwiderruflich gelöscht.") } },
            confirmButton = {
                ScaledContent(factory) {
                    TextButton(onClick = { viewModel.emptyTrash(); confirmEmpty = false }) {
                        Text("Alle löschen", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = { ScaledContent(factory) { TextButton(onClick = { confirmEmpty = false }) { Text("Abbrechen") } } }
        )
    }
}
