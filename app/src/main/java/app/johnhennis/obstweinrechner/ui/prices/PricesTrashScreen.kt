package app.johnhennis.obstweinrechner.ui.prices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.johnhennis.obstweinrechner.data.FruitPrice
import app.johnhennis.obstweinrechner.ui.AppViewModelFactory
import app.johnhennis.obstweinrechner.ui.common.ScaledContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricesTrashScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit
) {
    val viewModel: PricesViewModel = viewModel(factory = factory)
    val trashedByYear by viewModel.trashedByYear.collectAsState()
    var confirmDeleteEntry by remember { mutableStateOf<FruitPrice?>(null) }
    var confirmDeleteYear by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Papierkorb") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        if (trashedByYear.isEmpty()) {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text("Der Papierkorb ist leer.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                trashedByYear.forEach { group ->
                    item(key = "header_${group.jahr}") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${group.jahr} (${group.rows.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row {
                                TextButton(onClick = { viewModel.restoreYear(group.jahr) }) { Text("Jahr wiederherstellen") }
                                TextButton(onClick = { confirmDeleteYear = group.jahr }) {
                                    Text("Endgültig löschen", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    items(group.rows, key = { it.price.id }) { row ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${row.price.fruchtart} – ${row.price.datum} – ${row.price.preis} € (${row.price.quelle})", style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { viewModel.restoreEntry(row.price) }) { Text("Wiederherstellen") }
                                    TextButton(onClick = { confirmDeleteEntry = row.price }) {
                                        Text("Endgültig löschen", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    confirmDeleteEntry?.let { price ->
        AlertDialog(
            onDismissRequest = { confirmDeleteEntry = null },
            title = { ScaledContent(factory) { Text("Endgültig löschen?") } },
            text = { ScaledContent(factory) { Text("\"${price.fruchtart} ${price.jahr}\" wird unwiderruflich gelöscht.") } },
            confirmButton = {
                ScaledContent(factory) {
                    TextButton(onClick = { viewModel.deleteEntryPermanently(price); confirmDeleteEntry = null }) {
                        Text("Löschen", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = { ScaledContent(factory) { TextButton(onClick = { confirmDeleteEntry = null }) { Text("Abbrechen") } } }
        )
    }

    confirmDeleteYear?.let { jahr ->
        AlertDialog(
            onDismissRequest = { confirmDeleteYear = null },
            title = { ScaledContent(factory) { Text("Jahr $jahr endgültig löschen?") } },
            text = { ScaledContent(factory) { Text("Alle Preise aus $jahr werden unwiderruflich gelöscht.") } },
            confirmButton = {
                ScaledContent(factory) {
                    TextButton(onClick = { viewModel.deleteYearPermanently(jahr); confirmDeleteYear = null }) {
                        Text("Alle löschen", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = { ScaledContent(factory) { TextButton(onClick = { confirmDeleteYear = null }) { Text("Abbrechen") } } }
        )
    }
}
