package app.johnhennis.obstweinrechner.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.johnhennis.obstweinrechner.data.InventoryItem
import app.johnhennis.obstweinrechner.ui.AppViewModelFactory
import app.johnhennis.obstweinrechner.ui.common.ScaledContent
import kotlinx.coroutines.delay

private val SOLL_IST_WIDTH = 56.dp
private val DELETE_WIDTH = 28.dp

private enum class SortColumn { NAME, QUELLE }
private enum class SortDirection { ASC, DESC }

private fun formatPlain(value: Double): String = if (value == 0.0) "" else value.toInt().toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    factory: AppViewModelFactory,
    onOpenMenu: () -> Unit,
    onOpenTrash: () -> Unit
) {
    val viewModel: InventoryViewModel = viewModel(factory = factory)
    val items by viewModel.items.collectAsState()
    val statusMap by viewModel.statusMap.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var sortColumn by remember { mutableStateOf<SortColumn?>(null) }
    var sortDirection by remember { mutableStateOf(SortDirection.ASC) }

    fun onSortClick(column: SortColumn) {
        if (sortColumn == column) {
            sortDirection = if (sortDirection == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
        } else {
            sortColumn = column
            sortDirection = SortDirection.ASC
        }
    }

    val sortedItems = remember(items, statusMap, sortColumn, sortDirection) {
        val base = when (sortColumn) {
            SortColumn.NAME -> items.sortedBy { it.name.lowercase() }
            SortColumn.QUELLE -> items.sortedBy { (statusMap[it.id]?.quelle ?: "").lowercase() }
            null -> items
        }
        if (sortDirection == SortDirection.DESC) base.reversed() else base
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bestandsaufnahme") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenTrash) {
                        Icon(Icons.Filled.Delete, contentDescription = "Papierkorb öffnen")
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
                modifier = Modifier.padding(padding).padding(horizontal = 12.dp).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item(key = "columns") {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SortableColumnLabel(
                                "Name",
                                active = sortColumn == SortColumn.NAME,
                                direction = sortDirection,
                                onClick = { onSortClick(SortColumn.NAME) },
                                modifier = Modifier.weight(1.3f)
                            )
                            ColumnLabel("Soll", Modifier.width(SOLL_IST_WIDTH))
                            ColumnLabel("Ist", Modifier.width(SOLL_IST_WIDTH))
                            SortableColumnLabel(
                                "Quelle",
                                active = sortColumn == SortColumn.QUELLE,
                                direction = sortDirection,
                                onClick = { onSortClick(SortColumn.QUELLE) },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.size(DELETE_WIDTH))
                        }
                        HorizontalDivider()
                    }
                }
                items(sortedItems, key = { it.id }) { item ->
                    InventoryRow(
                        item = item,
                        quelle = statusMap[item.id]?.quelle ?: "",
                        onUpdate = { viewModel.updateItem(it) },
                        onQuelleChange = { viewModel.updateQuelle(item.id, it) },
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
            onAdd = { item, quelle -> viewModel.addItem(item, quelle); showAdd = false }
        )
    }
}

@Composable
private fun ColumnLabel(text: String, modifier: Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun SortableColumnLabel(
    text: String,
    active: Boolean,
    direction: SortDirection,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (active) {
            Icon(
                if (direction == SortDirection.ASC) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun InventoryRow(
    item: InventoryItem,
    quelle: String,
    onUpdate: (InventoryItem) -> Unit,
    onQuelleChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    var sollText by remember(item.id) { mutableStateOf(formatPlain(item.soll)) }
    var istText by remember(item.id) { mutableStateOf(formatPlain(item.ist)) }
    var quelleText by remember(item.id) { mutableStateOf(quelle) }
    var quelleUserEdited by remember(item.id) { mutableStateOf(false) }

    LaunchedEffect(quelle) {
        if (!quelleUserEdited) quelleText = quelle
    }

    LaunchedEffect(quelleText) {
        if (quelleUserEdited) {
            delay(500)
            onQuelleChange(quelleText)
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (item.einheit.isBlank()) item.name else "${item.name} (${item.einheit})",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1.3f)
            )
            CompactField(
                value = sollText,
                onValueChange = { new ->
                    if (new.isEmpty() || new.matches(Regex("^[0-9]*$"))) {
                        sollText = new
                        onUpdate(item.copy(soll = new.toIntOrNull()?.toDouble() ?: 0.0))
                    }
                },
                modifier = Modifier.width(SOLL_IST_WIDTH),
                keyboardType = KeyboardType.Number
            )
            CompactField(
                value = istText,
                onValueChange = { new ->
                    if (new.isEmpty() || new.matches(Regex("^[0-9]*$"))) {
                        istText = new
                        onUpdate(item.copy(ist = new.toIntOrNull()?.toDouble() ?: 0.0))
                    }
                },
                modifier = Modifier.width(SOLL_IST_WIDTH),
                keyboardType = KeyboardType.Number
            )
            CompactField(
                value = quelleText,
                onValueChange = { quelleText = it; quelleUserEdited = true },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(DELETE_WIDTH)) {
                Icon(Icons.Filled.Delete, contentDescription = "Entfernen", modifier = Modifier.size(16.dp))
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun CompactField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.bodySmall,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier
    )
}

@Composable
private fun AddItemDialog(
    factory: AppViewModelFactory,
    onDismiss: () -> Unit,
    onAdd: (InventoryItem, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var einheit by remember { mutableStateOf("") }
    var soll by remember { mutableStateOf("") }
    var ist by remember { mutableStateOf("") }
    var quelle by remember { mutableStateOf("") }
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
                        onValueChange = { new -> if (new.isEmpty() || new.matches(Regex("^[0-9]*$"))) soll = new },
                        label = { Text("Soll") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ist,
                        onValueChange = { new -> if (new.isEmpty() || new.matches(Regex("^[0-9]*$"))) ist = new },
                        label = { Text("Ist") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(value = quelle, onValueChange = { quelle = it }, label = { Text("Quelle") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            ScaledContent(factory) {
                TextButton(onClick = {
                    if (name.isBlank()) { error = "Bitte einen Namen eingeben."; return@TextButton }
                    val sollValue = if (soll.isBlank()) 0.0 else soll.toIntOrNull()?.toDouble()
                    val istValue = if (ist.isBlank()) 0.0 else ist.toIntOrNull()?.toDouble()
                    if (sollValue == null || istValue == null) { error = "Bitte gültige Zahlen eingeben."; return@TextButton }
                    onAdd(InventoryItem(name = name.trim(), einheit = einheit.trim(), soll = sollValue, ist = istValue), quelle.trim())
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
