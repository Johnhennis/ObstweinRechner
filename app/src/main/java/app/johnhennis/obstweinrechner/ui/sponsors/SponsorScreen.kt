package app.johnhennis.obstweinrechner.ui.sponsors

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.johnhennis.obstweinrechner.data.FruitSponsor
import app.johnhennis.obstweinrechner.ui.AppViewModelFactory
import app.johnhennis.obstweinrechner.ui.common.ScaledContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed class SponsorListEntry {
    data class YearHeader(val jahr: Int, val count: Int) : SponsorListEntry()
    data class Row(val sponsor: FruitSponsor) : SponsorListEntry()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SponsorScreen(
    factory: AppViewModelFactory,
    onOpenMenu: () -> Unit,
    onOpenTrash: () -> Unit
) {
    val viewModel: SponsorViewModel = viewModel(factory = factory)
    val yearGroups by viewModel.yearGroups.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var confirmDeleteYear by remember { mutableStateOf<Int?>(null) }
    var confirmDeleteSponsor by remember { mutableStateOf<FruitSponsor?>(null) }
    var expandedYear by rememberSaveable { mutableStateOf<Int?>(null) }

    val flatList = remember(yearGroups, expandedYear) {
        buildList {
            yearGroups.forEach { group ->
                add(SponsorListEntry.YearHeader(group.jahr, group.sponsors.size))
                if (group.jahr == expandedYear) {
                    group.sponsors.forEach { add(SponsorListEntry.Row(it)) }
                }
            }
        }
    }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Obstsponsoren") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                actions = {
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Sponsor hinzufügen")
                    }
                    IconButton(onClick = onOpenTrash) {
                        Icon(Icons.Filled.Delete, contentDescription = "Papierkorb öffnen")
                    }
                }
            )
        }
    ) { padding ->
        if (yearGroups.isEmpty()) {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text("Noch keine Sponsoren erfasst. Oben rechts mit + einen neuen hinzufügen.", style = MaterialTheme.typography.bodyMedium)
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
                            is SponsorListEntry.YearHeader -> "header_${entry.jahr}"
                            is SponsorListEntry.Row -> entry.sponsor.id
                        }
                    }
                ) { index, entry ->
                    when (entry) {
                        is SponsorListEntry.YearHeader -> {
                            val isExpanded = entry.jahr == expandedYear
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedYear = if (isExpanded) null else entry.jahr }
                                        .padding(top = 8.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = null
                                        )
                                        Text(
                                            "${entry.jahr} (${entry.count})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(onClick = { confirmDeleteYear = entry.jahr }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Jahr ${entry.jahr} in den Papierkorb", modifier = Modifier.size(18.dp))
                                    }
                                }
                                if (isExpanded) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        ColumnLabel("Wer", Modifier.weight(1f))
                                        ColumnLabel("Sorte", Modifier.weight(1f))
                                        Spacer(Modifier.size(28.dp))
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(bottom = 6.dp))
                            }
                        }
                        is SponsorListEntry.Row -> {
                            SponsorRow(
                                sponsor = entry.sponsor,
                                onUpdate = { viewModel.updateSponsor(it) },
                                onDelete = { confirmDeleteSponsor = entry.sponsor },
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
        AddSponsorDialog(
            factory = factory,
            defaultYear = viewModel.currentYear,
            onDismiss = { showAdd = false },
            onAdd = { jahr, wer, sorte ->
                viewModel.addSponsor(jahr, wer, sorte)
                expandedYear = jahr
                showAdd = false
            }
        )
    }

    confirmDeleteYear?.let { jahr ->
        AlertDialog(
            onDismissRequest = { confirmDeleteYear = null },
            title = { ScaledContent(factory) { Text("Jahr $jahr in den Papierkorb?") } },
            text = { ScaledContent(factory) { Text("Alle Sponsoren aus $jahr wandern in den Papierkorb und können dort wiederhergestellt werden.") } },
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

    confirmDeleteSponsor?.let { sponsor ->
        AlertDialog(
            onDismissRequest = { confirmDeleteSponsor = null },
            title = { ScaledContent(factory) { Text("In den Papierkorb verschieben?") } },
            text = { ScaledContent(factory) { Text("\"${sponsor.wer} – ${sponsor.sorte}\" wandert in den Papierkorb und kann dort wiederhergestellt werden.") } },
            confirmButton = {
                ScaledContent(factory) {
                    TextButton(onClick = { viewModel.deleteSponsor(sponsor); confirmDeleteSponsor = null }) {
                        Text("In den Papierkorb", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = { ScaledContent(factory) { TextButton(onClick = { confirmDeleteSponsor = null }) { Text("Abbrechen") } } }
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
private fun SponsorRow(
    sponsor: FruitSponsor,
    onUpdate: (FruitSponsor) -> Unit,
    onDelete: () -> Unit,
    onFocusGained: () -> Unit
) {
    var wer by remember(sponsor.id) { mutableStateOf(sponsor.wer) }
    var sorte by remember(sponsor.id) { mutableStateOf(sponsor.sorte) }
    var userEdited by remember(sponsor.id) { mutableStateOf(false) }

    LaunchedEffect(wer, sorte) {
        if (userEdited) {
            delay(500)
            onUpdate(sponsor.copy(wer = wer, sorte = sorte))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            RowField(wer, { wer = it; userEdited = true }, "Wer", Modifier.weight(1f), onFocus = onFocusGained)
            RowField(sorte, { sorte = it; userEdited = true }, "Sorte", Modifier.weight(1f), onFocus = onFocusGained)
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Entfernen", modifier = Modifier.size(18.dp))
            }
        }
        Row(modifier = Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = sponsor.geschenkt,
                onCheckedChange = { onUpdate(sponsor.copy(wer = wer, sorte = sorte, geschenkt = it)) }
            )
            Text("Geschenkt", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RowField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        textStyle = MaterialTheme.typography.bodySmall,
        singleLine = true,
        modifier = modifier.onFocusChanged { if (it.isFocused) onFocus() }
    )
}

@Composable
private fun AddSponsorDialog(
    factory: AppViewModelFactory,
    defaultYear: Int,
    onDismiss: () -> Unit,
    onAdd: (Int, String, String) -> Unit
) {
    var jahr by remember { mutableStateOf(defaultYear.toString()) }
    var wer by remember { mutableStateOf("") }
    var sorte by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { ScaledContent(factory) { Text("Neuer Sponsor") } },
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
                    OutlinedTextField(value = wer, onValueChange = { wer = it }, label = { Text("Wer") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sorte, onValueChange = { sorte = it }, label = { Text("Sorte") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            ScaledContent(factory) {
                TextButton(onClick = {
                    val jahrValue = jahr.toIntOrNull()
                    if (jahrValue == null || jahrValue < 2000) { error = "Bitte ein gültiges Jahr eingeben."; return@TextButton }
                    if (wer.isBlank()) { error = "Bitte angeben, wer gespendet hat."; return@TextButton }
                    onAdd(jahrValue, wer.trim(), sorte.trim())
                }) { Text("Hinzufügen") }
            }
        },
        dismissButton = { ScaledContent(factory) { TextButton(onClick = onDismiss) { Text("Abbrechen") } } }
    )
}
