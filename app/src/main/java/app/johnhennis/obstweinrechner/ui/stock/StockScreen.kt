package app.johnhennis.obstweinrechner.ui.stock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.johnhennis.obstweinrechner.data.StockItem
import app.johnhennis.obstweinrechner.ui.AppViewModelFactory
import app.johnhennis.obstweinrechner.ui.common.ScaledContent
import kotlinx.coroutines.delay

private fun tryParse(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

private fun fmtNum(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    factory: AppViewModelFactory,
    onOpenMenu: () -> Unit,
    onOpenTrash: () -> Unit
) {
    val viewModel: StockViewModel = viewModel(factory = factory)
    val yearGroups by viewModel.yearGroups.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var confirmDeleteYear by remember { mutableStateOf<Int?>(null) }
    var confirmDeleteItem by remember { mutableStateOf<StockItem?>(null) }
    var confirmNewYear by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var expandedYear by remember { mutableStateOf<Int?>(viewModel.currentYear) }

    val latestYear = yearGroups.maxOfOrNull { it.jahr }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bestandsliste") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                actions = {
                    if (latestYear != null) {
                        IconButton(onClick = { confirmNewYear = true }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Neues Jahr anlegen")
                        }
                    }
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
                modifier = Modifier.padding(padding).padding(horizontal = 12.dp).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                yearGroups.forEach { group ->
                    val isExpanded = group.jahr == expandedYear
                    item(key = "header_${group.jahr}") {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedYear = if (isExpanded) null else group.jahr }
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
                                        "${group.jahr} (${group.items.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(onClick = { confirmDeleteYear = group.jahr }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Jahr ${group.jahr} in den Papierkorb", modifier = Modifier.size(18.dp))
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                    if (isExpanded) {
                        items(group.items, key = { it.id }) { item ->
                            StockRow(
                                item = item,
                                onUpdate = { viewModel.updateItem(it) },
                                onDelete = { confirmDeleteItem = item }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddStockDialog(
            factory = factory,
            defaultYear = viewModel.currentYear,
            onDismiss = { showAdd = false },
            onAdd = { jahr, art, quelle, einheit, bv, ek, rest, ff, bem ->
                viewModel.addItem(jahr, art, quelle, einheit, bv, ek, rest, ff, bem)
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
            text = { ScaledContent(factory) { Text("\"${item.art}\" wandert in den Papierkorb und kann dort wiederhergestellt werden.") } },
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

    if (confirmNewYear && latestYear != null) {
        AlertDialog(
            onDismissRequest = { confirmNewYear = false },
            title = { ScaledContent(factory) { Text("Jahr ${latestYear + 1} anlegen?") } },
            text = {
                ScaledContent(factory) {
                    Text(
                        "Alle Positionen aus $latestYear werden übernommen: Vorjahr-Bestand = bisheriger Rest, " +
                            "Einkauf = bisher \"für Folgejahr\". Rest bleibt offen, bis du ihn einträgst."
                    )
                }
            },
            confirmButton = {
                ScaledContent(factory) {
                    TextButton(onClick = {
                        confirmNewYear = false
                        viewModel.createNextYear { _, message ->
                            resultMessage = message
                            expandedYear = latestYear + 1
                        }
                    }) { Text("Anlegen") }
                }
            },
            dismissButton = { ScaledContent(factory) { TextButton(onClick = { confirmNewYear = false }) { Text("Abbrechen") } } }
        )
    }

    resultMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { resultMessage = null },
            title = { ScaledContent(factory) { Text("Bestandsliste") } },
            text = { ScaledContent(factory) { Text(msg) } },
            confirmButton = { ScaledContent(factory) { TextButton(onClick = { resultMessage = null }) { Text("OK") } } }
        )
    }
}

@Composable
private fun StockRow(
    item: StockItem,
    onUpdate: (StockItem) -> Unit,
    onDelete: () -> Unit
) {
    var art by remember(item.id) { mutableStateOf(item.art) }
    var quelle by remember(item.id) { mutableStateOf(item.quelle) }
    var einheit by remember(item.id) { mutableStateOf(item.einheit) }
    var bestandVorjahr by remember(item.id) { mutableStateOf(item.bestandVorjahr) }
    var einkauf by remember(item.id) { mutableStateOf(item.einkauf) }
    var rest by remember(item.id) { mutableStateOf(item.rest) }
    var fuerFolgejahr by remember(item.id) { mutableStateOf(item.fuerFolgejahr) }
    var bemerkung by remember(item.id) { mutableStateOf(item.bemerkung) }
    var userEdited by remember(item.id) { mutableStateOf(false) }

    LaunchedEffect(art, quelle, einheit, bestandVorjahr, einkauf, rest, fuerFolgejahr, bemerkung) {
        if (userEdited) {
            delay(500)
            onUpdate(
                item.copy(
                    art = art, quelle = quelle, einheit = einheit,
                    bestandVorjahr = bestandVorjahr, einkauf = einkauf, rest = rest,
                    fuerFolgejahr = fuerFolgejahr, bemerkung = bemerkung
                )
            )
        }
    }

    val bedarf = tryParse(bestandVorjahr)?.let { bv -> tryParse(einkauf)?.let { ek -> bv + ek } }
    val verbraucht = bedarf?.let { bd -> tryParse(rest)?.let { r -> bd - r } }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            CompactField(art, { art = it; userEdited = true }, "Art", Modifier.weight(1.4f))
            CompactField(quelle, { quelle = it; userEdited = true }, "Quelle", Modifier.weight(1f))
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Entfernen", modifier = Modifier.size(18.dp))
            }
        }
        Row(
            modifier = Modifier.padding(top = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactField(einheit, { einheit = it; userEdited = true }, "Einheit", Modifier.weight(1f))
            CompactField(bestandVorjahr, { bestandVorjahr = it; userEdited = true }, "Vorjahr", Modifier.weight(0.8f))
            CompactField(einkauf, { einkauf = it; userEdited = true }, "Einkauf", Modifier.weight(0.8f))
            CompactField(rest, { rest = it; userEdited = true }, "Rest", Modifier.weight(0.8f))
            CompactField(fuerFolgejahr, { fuerFolgejahr = it; userEdited = true }, "Folgejahr", Modifier.weight(0.9f))
        }
        if (bedarf != null || verbraucht != null) {
            Text(
                listOfNotNull(
                    bedarf?.let { "Bedarf: ${fmtNum(it)}" },
                    verbraucht?.let { "Verbraucht: ${fmtNum(it)}" }
                ).joinToString("   "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        CompactField(
            bemerkung, { bemerkung = it; userEdited = true }, "Bemerkung",
            Modifier.fillMaxWidth().padding(top = 3.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun CompactField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        textStyle = MaterialTheme.typography.bodySmall,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier
    )
}

@Composable
private fun AddStockDialog(
    factory: AppViewModelFactory,
    defaultYear: Int,
    onDismiss: () -> Unit,
    onAdd: (Int, String, String, String, String, String, String, String, String) -> Unit
) {
    var jahr by remember { mutableStateOf(defaultYear.toString()) }
    var art by remember { mutableStateOf("") }
    var quelle by remember { mutableStateOf("") }
    var einheit by remember { mutableStateOf("") }
    var bestandVorjahr by remember { mutableStateOf("") }
    var einkauf by remember { mutableStateOf("") }
    var rest by remember { mutableStateOf("") }
    var fuerFolgejahr by remember { mutableStateOf("") }
    var bemerkung by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { ScaledContent(factory) { Text("Neue Position") } },
        text = {
            ScaledContent(factory) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = jahr,
                        onValueChange = { new -> if (new.isEmpty() || new.matches(Regex("^[0-9]{0,4}$"))) jahr = new },
                        label = { Text("Jahr") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(value = art, onValueChange = { art = it }, label = { Text("Art") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = quelle, onValueChange = { quelle = it }, label = { Text("Quelle") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = einheit, onValueChange = { einheit = it }, label = { Text("Einheit") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = bestandVorjahr, onValueChange = { bestandVorjahr = it }, label = { Text("Vorjahr") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = einkauf, onValueChange = { einkauf = it }, label = { Text("Einkauf") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = rest, onValueChange = { rest = it }, label = { Text("Rest") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = fuerFolgejahr, onValueChange = { fuerFolgejahr = it }, label = { Text("Folgejahr") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = bemerkung, onValueChange = { bemerkung = it }, label = { Text("Bemerkung") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            ScaledContent(factory) {
                TextButton(onClick = {
                    val jahrValue = jahr.toIntOrNull()
                    if (jahrValue == null || jahrValue < 2000) { error = "Bitte ein gültiges Jahr eingeben."; return@TextButton }
                    if (art.isBlank()) { error = "Bitte eine Art eingeben."; return@TextButton }
                    onAdd(jahrValue, art.trim(), quelle.trim(), einheit.trim(), bestandVorjahr.trim(), einkauf.trim(), rest.trim(), fuerFolgejahr.trim(), bemerkung.trim())
                }) { Text("Hinzufügen") }
            }
        },
        dismissButton = { ScaledContent(factory) { TextButton(onClick = onDismiss) { Text("Abbrechen") } } }
    )
}
