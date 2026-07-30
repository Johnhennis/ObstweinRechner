package app.johnhennis.obstweinrechner.ui.winestock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.johnhennis.obstweinrechner.data.WineStockItem
import app.johnhennis.obstweinrechner.ui.AppViewModelFactory
import app.johnhennis.obstweinrechner.ui.common.ScaledContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val MENGE_WIDTH = 72.dp
private val DELETE_WIDTH = 28.dp

private fun fmtWhole(v: Double): String = v.toLong().toString()
private fun formatFieldValue(v: Double): String = if (v == 0.0) "" else v.toLong().toString()

private sealed class WineStockListEntry {
    data class YearHeader(val group: WineStockYearGroup) : WineStockListEntry()
    data class Row(val item: WineStockItem) : WineStockListEntry()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WineStockScreen(
    factory: AppViewModelFactory,
    onOpenMenu: () -> Unit,
    onOpenTrash: () -> Unit
) {
    val viewModel: WineStockViewModel = viewModel(factory = factory)
    val yearGroups by viewModel.yearGroups.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var confirmDeleteYear by remember { mutableStateOf<Int?>(null) }
    var confirmDeleteItem by remember { mutableStateOf<WineStockItem?>(null) }
    var expandedYear by remember { mutableStateOf<Int?>(null) }

    val flatList = remember(yearGroups, expandedYear) {
        buildList {
            yearGroups.forEach { group ->
                add(WineStockListEntry.YearHeader(group))
                if (group.jahr == expandedYear) {
                    group.items.forEach { add(WineStockListEntry.Row(it)) }
                }
            }
        }
    }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weinbestand") },
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
        if (yearGroups.isEmpty()) {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text("Noch keine Positionen erfasst. Mit dem + unten rechts eine neue Position hinzufügen.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(padding).padding(horizontal = 12.dp).fillMaxWidth().imePadding(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(
                    flatList,
                    key = { _, entry ->
                        when (entry) {
                            is WineStockListEntry.YearHeader -> "header_${entry.group.jahr}"
                            is WineStockListEntry.Row -> entry.item.id
                        }
                    }
                ) { index, entry ->
                    when (entry) {
                        is WineStockListEntry.YearHeader -> {
                            val isExpanded = entry.group.jahr == expandedYear
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedYear = if (isExpanded) null else entry.group.jahr }
                                        .padding(top = 10.dp, bottom = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = null
                                        )
                                        Text(
                                            "${entry.group.jahr} (${entry.group.items.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(onClick = { confirmDeleteYear = entry.group.jahr }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Jahr ${entry.group.jahr} in den Papierkorb", modifier = Modifier.size(18.dp))
                                    }
                                }
                                Text(
                                    "Soll gesamt: ${fmtWhole(entry.group.sollSumme)} L    Aktuell gesamt: ${fmtWhole(entry.group.aktuelleSumme)} L",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                if (isExpanded) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        ColumnLabel("Sorte", Modifier.weight(1.5f))
                                        ColumnLabel("Soll (L)", Modifier.width(MENGE_WIDTH))
                                        ColumnLabel("Aktuell (L)", Modifier.width(MENGE_WIDTH))
                                        Spacer(Modifier.size(DELETE_WIDTH))
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                        is WineStockListEntry.Row -> {
                            WineStockRow(
                                item = entry.item,
                                onUpdate = { viewModel.updateItem(it) },
                                onDelete = { confirmDeleteItem = entry.item },
                                onFocusGained = {
                                    coroutineScope.launch { listState.animateScrollToItem(index) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddWineStockDialog(
            factory = factory,
            defaultYear = viewModel.currentYear,
            onDismiss = { showAdd = false },
            onAdd = { jahr, sorte, soll, aktuell ->
                viewModel.addItem(jahr, sorte, soll, aktuell)
                expandedYear = jahr
                showAdd = false
            }
        )
    }

    confirmDeleteYear?.let { jahr ->
        AlertDialog(
            onDismissRequest = { confirmDeleteYear = null },
            title = { ScaledContent(factory) { Text("Jahr $jahr in den Papierkorb?") } },
            text = { ScaledContent(factory) { Text("Alle Positionen aus $jahr wandern in den Papierkorb und können dort wiederhergestellt werden.") } },
            confirmButton = {
                ScaledContent(factory) {
                    TextButton(onClick = { viewModel.deleteYear(jahr); confirmDeleteYear = null }) {
                        Text("In den Papierkorb", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = { ScaledContent(factory) { TextButton(onClick = { confirmDeleteYear = null }) { Text("Abbrechen") } } }
        )
    }

    confirmDeleteItem?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmDeleteItem = null },
            title = { ScaledContent(factory) { Text("In den Papierkorb verschieben?") } },
            text = { ScaledContent(factory) { Text("\"${item.sorte}\" wandert in den Papierkorb und kann dort wiederhergestellt werden.") } },
            confirmButton = {
                ScaledContent(factory) {
                    TextButton(onClick = { viewModel.deleteItem(item); confirmDeleteItem = null }) {
                        Text("In den Papierkorb", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = { ScaledContent(factory) { TextButton(onClick = { confirmDeleteItem = null }) { Text("Abbrechen") } } }
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
private fun WineStockRow(
    item: WineStockItem,
    onUpdate: (WineStockItem) -> Unit,
    onDelete: () -> Unit,
    onFocusGained: () -> Unit
) {
    var sorte by remember(item.id) { mutableStateOf(item.sorte) }
    var sollText by remember(item.id) { mutableStateOf(formatFieldValue(item.sollmenge)) }
    var aktuellText by remember(item.id) { mutableStateOf(formatFieldValue(item.aktuelleMenge)) }
    var userEdited by remember(item.id) { mutableStateOf(false) }

    LaunchedEffect(sorte, sollText, aktuellText) {
        if (userEdited) {
            delay(500)
            onUpdate(
                item.copy(
                    sorte = sorte,
                    sollmenge = sollText.toIntOrNull()?.toDouble() ?: item.sollmenge,
                    aktuelleMenge = aktuellText.toIntOrNull()?.toDouble() ?: item.aktuelleMenge
                )
            )
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RowField(sorte, { sorte = it; userEdited = true }, Modifier.weight(1.5f), onFocus = onFocusGained)
            RowField(
                value = sollText,
                onValueChange = { new -> if (new.isEmpty() || new.matches(Regex("^[0-9]*$"))) { sollText = new; userEdited = true } },
                modifier = Modifier.width(MENGE_WIDTH),
                keyboardType = KeyboardType.Number,
                onFocus = onFocusGained
            )
            RowField(
                value = aktuellText,
                onValueChange = { new -> if (new.isEmpty() || new.matches(Regex("^[0-9]*$"))) { aktuellText = new; userEdited = true } },
                modifier = Modifier.width(MENGE_WIDTH),
                keyboardType = KeyboardType.Number,
                onFocus = onFocusGained
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(DELETE_WIDTH)) {
                Icon(Icons.Filled.Delete, contentDescription = "Entfernen", modifier = Modifier.size(18.dp))
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun RowField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onFocus: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.bodyMedium,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.onFocusChanged { if (it.isFocused) onFocus() }
    )
}

@Composable
private fun AddWineStockDialog(
    factory: AppViewModelFactory,
    defaultYear: Int,
    onDismiss: () -> Unit,
    onAdd: (Int, String, Double, Double) -> Unit
) {
    var jahr by remember { mutableStateOf(defaultYear.toString()) }
    var sorte by remember { mutableStateOf("") }
    var soll by remember { mutableStateOf("") }
    var aktuell by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { ScaledContent(factory) { Text("Neue Position") } },
        text = {
            ScaledContent(factory) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = jahr,
                        onValueChange = { new -> if (new.isEmpty() || new.matches(Regex("^[0-9]{0,4}$"))) jahr = new },
                        label = { Text("Jahr") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(value = sorte, onValueChange = { sorte = it }, label = { Text("Sorte") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = soll,
                        onValueChange = { new -> if (new.isEmpty() || new.matches(Regex("^[0-9]*$"))) soll = new },
                        label = { Text("Sollmenge (L)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = aktuell,
                        onValueChange = { new -> if (new.isEmpty() || new.matches(Regex("^[0-9]*$"))) aktuell = new },
                        label = { Text("Aktuelle Menge (L)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    val jahrValue = jahr.toIntOrNull()
                    if (jahrValue == null || jahrValue < 2000) { error = "Bitte ein gültiges Jahr eingeben."; return@TextButton }
                    if (sorte.isBlank()) { error = "Bitte eine Sorte eingeben."; return@TextButton }
                    val sollValue = if (soll.isBlank()) 0.0 else soll.toIntOrNull()?.toDouble()
                    val aktuellValue = if (aktuell.isBlank()) 0.0 else aktuell.toIntOrNull()?.toDouble()
                    if (sollValue == null || aktuellValue == null) { error = "Bitte gültige Zahlen eingeben."; return@TextButton }
                    onAdd(jahrValue, sorte.trim(), sollValue, aktuellValue)
                }) { Text("Hinzufügen") }
            }
        },
        dismissButton = { ScaledContent(factory) { TextButton(onClick = onDismiss) { Text("Abbrechen") } } }
    )
}
