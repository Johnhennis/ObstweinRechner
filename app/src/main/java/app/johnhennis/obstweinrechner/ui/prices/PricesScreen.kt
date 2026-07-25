package app.johnhennis.obstweinrechner.ui.prices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.johnhennis.obstweinrechner.data.FruitPrice
import app.johnhennis.obstweinrechner.ui.AppViewModelFactory
import app.johnhennis.obstweinrechner.ui.common.ScaledContent
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricesScreen(
    factory: AppViewModelFactory,
    onOpenMenu: () -> Unit,
    onOpenTrash: () -> Unit
) {
    val viewModel: PricesViewModel = viewModel(factory = factory)
    val yearGroups by viewModel.yearGroups.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var confirmDeleteYear by remember { mutableStateOf<Int?>(null) }
    val expanded = remember { mutableStateMapOf<Int, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preise Obst") },
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
                Icon(Icons.Filled.Add, contentDescription = "Preis hinzufügen")
            }
        }
    ) { padding ->
        if (yearGroups.isEmpty()) {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text("Noch keine Preise erfasst. Mit dem + unten rechts einen Preis hinzufügen.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(horizontal = 12.dp).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                yearGroups.forEach { group ->
                    val isExpanded = expanded[group.jahr] ?: (group.jahr == viewModel.currentYear)
                    item(key = "header_${group.jahr}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded[group.jahr] = !isExpanded }
                                .padding(top = 10.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null
                                )
                                Text(
                                    "${group.jahr} (${group.rows.size})",
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
                    if (isExpanded) {
                        items(group.rows, key = { it.price.id }) { row ->
                            PriceRow(
                                row = row,
                                onUpdate = { viewModel.updateEntry(it) },
                                onDelete = { viewModel.deleteEntry(row.price) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddPriceDialog(
            factory = factory,
            defaultYear = viewModel.currentYear,
            onDismiss = { showAdd = false },
            onAdd = { j, f, d, p, q ->
                viewModel.addEntry(j, f, d, p, q)
                expanded[j] = true
                showAdd = false
            }
        )
    }

    confirmDeleteYear?.let { jahr ->
        AlertDialog(
            onDismissRequest = { confirmDeleteYear = null },
            title = { ScaledContent(factory) { Text("Jahr $jahr in den Papierkorb?") } },
            text = { ScaledContent(factory) { Text("Alle Preise aus $jahr wandern in den Papierkorb und können dort wiederhergestellt werden.") } },
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
}

@Composable
private fun PriceRow(
    row: PriceRow,
    onUpdate: (FruitPrice) -> Unit,
    onDelete: () -> Unit
) {
    val price = row.price
    var fruchtart by remember(price.id) { mutableStateOf(price.fruchtart) }
    var datum by remember(price.id) { mutableStateOf(price.datum) }
    var preisText by remember(price.id) { mutableStateOf(if (price.preis == 0.0) "" else price.preis.toString()) }
    var quelle by remember(price.id) { mutableStateOf(price.quelle) }
    var userEdited by remember(price.id) { mutableStateOf(false) }

    LaunchedEffect(fruchtart, datum, preisText, quelle) {
        if (userEdited) {
            delay(500)
            onUpdate(
                price.copy(
                    fruchtart = fruchtart,
                    datum = datum,
                    preis = preisText.replace(',', '.').toDoubleOrNull() ?: price.preis,
                    quelle = quelle
                )
            )
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactField(fruchtart, { fruchtart = it; userEdited = true }, "Frucht", Modifier.weight(1.3f))
            CompactField(datum, { datum = it; userEdited = true }, "Datum", Modifier.width(58.dp))
            CompactField(
                preisText,
                { new -> if (new.isEmpty() || new.matches(Regex("^[0-9]*[.,]?[0-9]*$"))) { preisText = new; userEdited = true } },
                "€",
                Modifier.width(54.dp),
                KeyboardType.Decimal
            )
            CompactField(quelle, { quelle = it; userEdited = true }, "Quelle", Modifier.weight(1f))
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Entfernen", modifier = Modifier.size(16.dp))
            }
        }
        if (row.vorjahresPreis != null) {
            Text(
                "Vorjahr: ${fmt(row.vorjahresPreis)} €",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun CompactField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
        textStyle = MaterialTheme.typography.bodySmall,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier
    )
}

@Composable
private fun AddPriceDialog(
    factory: AppViewModelFactory,
    defaultYear: Int,
    onDismiss: () -> Unit,
    onAdd: (Int, String, String, Double, String) -> Unit
) {
    var jahr by remember { mutableStateOf(defaultYear.toString()) }
    var fruchtart by remember { mutableStateOf("") }
    var datum by remember { mutableStateOf("") }
    var preis by remember { mutableStateOf("") }
    var quelle by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { ScaledContent(factory) { Text("Neuer Preis") } },
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
                    OutlinedTextField(value = fruchtart, onValueChange = { fruchtart = it }, label = { Text("Fruchtart") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = datum, onValueChange = { datum = it }, label = { Text("Datum (z. B. 12.9.)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = preis,
                        onValueChange = { new -> if (new.isEmpty() || new.matches(Regex("^[0-9]*[.,]?[0-9]*$"))) preis = new },
                        label = { Text("Preis") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                    val jahrValue = jahr.toIntOrNull()
                    if (jahrValue == null || jahrValue < 2000) { error = "Bitte ein gültiges Jahr eingeben."; return@TextButton }
                    if (fruchtart.isBlank()) { error = "Bitte eine Fruchtart eingeben."; return@TextButton }
                    val preisValue = preis.replace(',', '.').toDoubleOrNull()
                    if (preisValue == null) { error = "Bitte einen gültigen Preis eingeben."; return@TextButton }
                    onAdd(jahrValue, fruchtart.trim(), datum.trim(), preisValue, quelle.trim())
                }) { Text("Hinzufügen") }
            }
        },
        dismissButton = { ScaledContent(factory) { TextButton(onClick = onDismiss) { Text("Abbrechen") } } }
    )
}

private fun fmt(value: Double): String = String.format(Locale.GERMANY, "%.2f", value)
