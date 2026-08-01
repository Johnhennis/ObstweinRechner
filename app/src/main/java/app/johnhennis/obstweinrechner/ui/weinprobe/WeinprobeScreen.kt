package app.johnhennis.obstweinrechner.ui.weinprobe

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.johnhennis.obstweinrechner.data.WeinprobeEntry
import app.johnhennis.obstweinrechner.ui.AppViewModelFactory
import app.johnhennis.obstweinrechner.ui.common.ScaledContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DELETE_WIDTH = 28.dp

private fun displayDate(iso: String): String = try {
    java.time.LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
} catch (e: Exception) {
    iso
}

private sealed class WeinprobeListEntry {
    data class DateHeader(val group: WeinprobeDateGroup) : WeinprobeListEntry()
    data class Row(val entry: WeinprobeEntry) : WeinprobeListEntry()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeinprobeScreen(
    factory: AppViewModelFactory,
    onOpenMenu: () -> Unit,
    onOpenTrash: () -> Unit
) {
    val viewModel: WeinprobeViewModel = viewModel(factory = factory)
    val dateGroups by viewModel.dateGroups.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var confirmDeleteDatum by remember { mutableStateOf<String?>(null) }
    var confirmDeleteEntry by remember { mutableStateOf<WeinprobeEntry?>(null) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var expandedDatum by remember { mutableStateOf<String?>(null) }

    val flatList = remember(dateGroups, expandedDatum) {
        buildList {
            dateGroups.forEach { group ->
                add(WeinprobeListEntry.DateHeader(group))
                if (group.datum == expandedDatum) {
                    group.entries.forEach { add(WeinprobeListEntry.Row(it)) }
                }
            }
        }
    }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weinprobe") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                actions = {
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Weinprobe anlegen")
                    }
                    IconButton(onClick = onOpenTrash) {
                        Icon(Icons.Filled.Delete, contentDescription = "Papierkorb öffnen")
                    }
                }
            )
        }
    ) { padding ->
        if (dateGroups.isEmpty()) {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text("Noch keine Weinprobe angelegt. Oben rechts mit + eine neue anlegen - übernimmt automatisch alle Sorten mit Ist-Bestand aus dem Weinbestand.", style = MaterialTheme.typography.bodyMedium)
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
                            is WeinprobeListEntry.DateHeader -> "header_${entry.group.datum}"
                            is WeinprobeListEntry.Row -> entry.entry.id
                        }
                    }
                ) { index, entry ->
                    when (entry) {
                        is WeinprobeListEntry.DateHeader -> {
                            val isExpanded = entry.group.datum == expandedDatum
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedDatum = if (isExpanded) null else entry.group.datum }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = null
                                        )
                                        Text(
                                            "${displayDate(entry.group.datum)} (${entry.group.entries.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(onClick = { confirmDeleteDatum = entry.group.datum }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Diese Weinprobe in den Papierkorb", modifier = Modifier.size(18.dp))
                                    }
                                }
                                if (isExpanded) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        ColumnLabel("Sorte", Modifier.weight(1f))
                                        ColumnLabel("Bemerkung", Modifier.weight(1.8f))
                                        Spacer(Modifier.size(DELETE_WIDTH))
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                        is WeinprobeListEntry.Row -> {
                            WeinprobeRow(
                                entry = entry.entry,
                                onUpdate = { viewModel.updateEntry(it) },
                                onDelete = { confirmDeleteEntry = entry.entry },
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
        AddWeinprobeDialog(
            factory = factory,
            onDismiss = { showAdd = false },
            onConfirm = { datum ->
                showAdd = false
                viewModel.createWeinprobe(datum) { _, message ->
                    resultMessage = message
                    expandedDatum = datum
                }
            }
        )
    }

    confirmDeleteDatum?.let { datum ->
        AlertDialog(
            onDismissRequest = { confirmDeleteDatum = null },
            title = { ScaledContent(factory) { Text("Weinprobe in den Papierkorb?") } },
            text = { ScaledContent(factory) { Text("Alle Einträge vom ${displayDate(datum)} wandern in den Papierkorb und können dort wiederhergestellt werden.") } },
            confirmButton = {
                ScaledContent(factory) {
                    TextButton(onClick = { viewModel.deleteDatum(datum); confirmDeleteDatum = null }) {
                        Text("In den Papierkorb", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = { ScaledContent(factory) { TextButton(onClick = { confirmDeleteDatum = null }) { Text("Abbrechen") } } }
        )
    }

    confirmDeleteEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmDeleteEntry = null },
            title = { ScaledContent(factory) { Text("In den Papierkorb verschieben?") } },
            text = { ScaledContent(factory) { Text("\"${entry.sorte}\" wandert in den Papierkorb und kann dort wiederhergestellt werden.") } },
            confirmButton = {
                ScaledContent(factory) {
                    TextButton(onClick = { viewModel.deleteEntry(entry); confirmDeleteEntry = null }) {
                        Text("In den Papierkorb", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = { ScaledContent(factory) { TextButton(onClick = { confirmDeleteEntry = null }) { Text("Abbrechen") } } }
        )
    }

    resultMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { resultMessage = null },
            title = { ScaledContent(factory) { Text("Weinprobe") } },
            text = { ScaledContent(factory) { Text(msg) } },
            confirmButton = { ScaledContent(factory) { TextButton(onClick = { resultMessage = null }) { Text("OK") } } }
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
private fun WeinprobeRow(
    entry: WeinprobeEntry,
    onUpdate: (WeinprobeEntry) -> Unit,
    onDelete: () -> Unit,
    onFocusGained: () -> Unit
) {
    var sorte by remember(entry.id) { mutableStateOf(entry.sorte) }
    var bemerkung by remember(entry.id) { mutableStateOf(entry.bemerkung) }
    var userEdited by remember(entry.id) { mutableStateOf(false) }

    LaunchedEffect(sorte, bemerkung) {
        if (userEdited) {
            delay(500)
            onUpdate(entry.copy(sorte = sorte, bemerkung = bemerkung))
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RowField(sorte, { sorte = it; userEdited = true }, Modifier.weight(1f), onFocus = onFocusGained)
            RowField(bemerkung, { bemerkung = it; userEdited = true }, Modifier.weight(1.8f), onFocus = onFocusGained)
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
    onFocus: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.bodyMedium,
        singleLine = true,
        modifier = modifier.onFocusChanged { if (it.isFocused) onFocus() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWeinprobeDialog(
    factory: AppViewModelFactory,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            ScaledContent(factory) {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis ?: return@TextButton
                    val iso = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        .format(DateTimeFormatter.ISO_LOCAL_DATE)
                    onConfirm(iso)
                }) { Text("Anlegen") }
            }
        },
        dismissButton = { ScaledContent(factory) { TextButton(onClick = onDismiss) { Text("Abbrechen") } } }
    ) {
        ScaledContent(factory) {
            DatePicker(state = datePickerState)
        }
    }
}
