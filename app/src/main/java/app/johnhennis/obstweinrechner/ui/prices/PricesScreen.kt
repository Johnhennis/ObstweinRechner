package app.johnhennis.obstweinrechner.ui.prices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.johnhennis.obstweinrechner.data.FruitPrice
import app.johnhennis.obstweinrechner.ui.AppViewModelFactory
import app.johnhennis.obstweinrechner.ui.common.ScaledContent
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
                modifier = Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                yearGroups.forEach { group ->
                    item(key = "header_${group.jahr}") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${group.jahr}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { confirmDeleteYear = group.jahr }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Jahr ${group.jahr} in den Papierkorb")
                            }
                        }
                    }
                    items(group.rows, key = { it.price.id }) { row ->
                        PriceRowCard(
                            row = row,
                            onUpdate = { viewModel.updateEntry(it) },
                            onDelete = { viewModel.deleteEntry(row.price) }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddPriceDialog(
            factory = factory,
            onDismiss = { showAdd = false },
            onAdd = { f, d, p, q -> viewModel.addEntry(f, d, p, q); showAdd = false }
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
private fun PriceRowCard(
    row: PriceRow,
    onUpdate: (FruitPrice) -> Unit,
    onDelete: () -> Unit
) {
    val price = row.price
    var fruchtart by remember(price.id) { mutableStateOf(price.fruchtart) }
    var datum by remember(price.id) { mutableStateOf(price.datum) }
    var preisText by remember(price.id) { mutableStateOf(if (price.preis == 0.0) "" else price.preis.toString()) }
    var quelle by remember(price.id) { mutableStateOf(price.quelle) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = fruchtart,
                    onValueChange = { fruchtart = it; onUpdate(price.copy(fruchtart = it)) },
                    label = { Text("Fruchtart") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Entfernen")
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = datum,
                    onValueChange = { datum = it; onUpdate(price.copy(datum = it)) },
                    label = { Text("Datum") },
                    singleLine = true,
                    modifier = Modifier.width(100.dp)
                )
                OutlinedTextField(
                    value = preisText,
                    onValueChange = { new ->
                        if (new.isEmpty() || new.matches(Regex("^[0-9]*[.,]?[0-9]*$"))) {
                            preisText = new
                            onUpdate(price.copy(preis = new.replace(',', '.').toDoubleOrNull() ?: 0.0))
                        }
                    },
                    label = { Text("Preis") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.width(100.dp)
                )
                OutlinedTextField(
                    value = quelle,
                    onValueChange = { quelle = it; onUpdate(price.copy(quelle = it)) },
                    label = { Text("Quelle") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            row.vorjahresPreis?.let {
                Text(
                    "Vorjahr: ${fmt(it)} €",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddPriceDialog(
    factory: AppViewModelFactory,
    onDismiss: () -> Unit,
    onAdd: (String, String, Double, String) -> Unit
) {
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
                    if (fruchtart.isBlank()) { error = "Bitte eine Fruchtart eingeben."; return@TextButton }
                    val preisValue = preis.replace(',', '.').toDoubleOrNull()
                    if (preisValue == null) { error = "Bitte einen gültigen Preis eingeben."; return@TextButton }
                    onAdd(fruchtart.trim(), datum.trim(), preisValue, quelle.trim())
                }) { Text("Hinzufügen") }
            }
        },
        dismissButton = { ScaledContent(factory) { TextButton(onClick = onDismiss) { Text("Abbrechen") } } }
    )
}

private fun fmt(value: Double): String = String.format(Locale.GERMANY, "%.2f", value)
