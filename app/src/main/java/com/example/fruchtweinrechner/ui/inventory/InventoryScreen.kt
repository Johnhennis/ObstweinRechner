package com.example.fruchtweinrechner.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fruchtweinrechner.data.InventoryItem
import com.example.fruchtweinrechner.ui.AppViewModelFactory
import com.example.fruchtweinrechner.ui.common.ScaledContent

private fun formatPlain(value: Double): String = if (value == 0.0) "" else value.toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    factory: AppViewModelFactory,
    onOpenMenu: () -> Unit
) {
    val viewModel: InventoryViewModel = viewModel(factory = factory)
    val items by viewModel.items.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bestandsaufnahme") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Position hinzufügen")
            }
        }
    ) { padding ->
        if (items.isEmpty()) {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text(
                    "Noch keine Positionen erfasst. Mit dem + unten rechts eine neue Position hinzufügen.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    InventoryRow(
                        item = item,
                        onUpdate = { viewModel.updateItem(it) },
                        onDelete = { viewModel.deleteItem(item) }
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddItemDialog(
            factory = factory,
            onDismiss = { showAdd = false },
            onAdd = { viewModel.addItem(it); showAdd = false }
        )
    }
}

@Composable
private fun InventoryRow(
    item: InventoryItem,
    onUpdate: (InventoryItem) -> Unit,
    onDelete: () -> Unit
) {
    var sollText by remember(item.id) { mutableStateOf(formatPlain(item.soll)) }
    var istText by remember(item.id) { mutableStateOf(formatPlain(item.ist)) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (item.einheit.isBlank()) item.name else "${item.name} (${item.einheit})",
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Entfernen")
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = sollText,
                    onValueChange = { new ->
                        if (new.isEmpty() || new.matches(Regex("^[0-9]*[.,]?[0-9]*$"))) {
                            sollText = new
                            onUpdate(item.copy(soll = new.replace(',', '.').toDoubleOrNull() ?: 0.0))
                        }
                    },
                    label = { Text("Soll") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.width(120.dp)
                )
                OutlinedTextField(
                    value = istText,
                    onValueChange = { new ->
                        if (new.isEmpty() || new.matches(Regex("^[0-9]*[.,]?[0-9]*$"))) {
                            istText = new
                            onUpdate(item.copy(ist = new.replace(',', '.').toDoubleOrNull() ?: 0.0))
                        }
                    },
                    label = { Text("Ist") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.width(120.dp)
                )
            }
        }
    }
}

@Composable
private fun AddItemDialog(
    factory: AppViewModelFactory,
    onDismiss: () -> Unit,
    onAdd: (InventoryItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var einheit by remember { mutableStateOf("") }
    var soll by remember { mutableStateOf("") }
    var ist by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { ScaledContent(factory) { Text("Neue Position") } },
        text = {
            ScaledContent(factory) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = einheit, onValueChange = { einheit = it }, label = { Text("Einheit (z. B. Flaschen, L)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = soll,
                        onValueChange = { new -> if (new.isEmpty() || new.matches(Regex("^[0-9]*[.,]?[0-9]*$"))) soll = new },
                        label = { Text("Soll") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ist,
                        onValueChange = { new -> if (new.isEmpty() || new.matches(Regex("^[0-9]*[.,]?[0-9]*$"))) ist = new },
                        label = { Text("Ist") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            ScaledContent(factory) {
                TextButton(onClick = {
                    if (name.isBlank()) { error = "Bitte einen Namen eingeben."; return@TextButton }
                    val sollValue = if (soll.isBlank()) 0.0 else soll.replace(',', '.').toDoubleOrNull()
                    val istValue = if (ist.isBlank()) 0.0 else ist.replace(',', '.').toDoubleOrNull()
                    if (sollValue == null || istValue == null) { error = "Bitte gültige Zahlen eingeben."; return@TextButton }
                    onAdd(InventoryItem(name = name.trim(), einheit = einheit.trim(), soll = sollValue, ist = istValue))
                }) { Text("Hinzufügen") }
            }
        },
        dismissButton = {
            ScaledContent(factory) {
                TextButton(onClick = onDismiss) { Text("Abbrechen") }
            }
        }
    )
}
