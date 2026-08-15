package app.johnhennis.obstweinrechner.ui.wineorder

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.johnhennis.obstweinrechner.data.WineOrder
import app.johnhennis.obstweinrechner.ui.AppViewModelFactory
import app.johnhennis.obstweinrechner.ui.common.ScaledContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WineOrderTrashScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit
) {
    val viewModel: WineOrderViewModel = viewModel(factory = factory)
    val context = LocalContext.current
    val trashedByYear by viewModel.trashedByYear.collectAsState()
    var confirmDeleteOrder by remember { mutableStateOf<WineOrder?>(null) }
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
                            Text("${group.jahr} (${group.orders.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row {
                                TextButton(onClick = { viewModel.restoreYear(context, group.jahr) }) { Text("Wiederherstellen") }
                                TextButton(onClick = { confirmDeleteYear = group.jahr }) {
                                    Text("Endgültig löschen", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    items(group.orders, key = { it.id }) { order ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${order.wer} – ${order.sorte}", style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { viewModel.restoreOrder(context, order) }) { Text("Wiederherstellen") }
                                    TextButton(onClick = { confirmDeleteOrder = order }) {
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

    confirmDeleteOrder?.let { order ->
        AlertDialog(
            onDismissRequest = { confirmDeleteOrder = null },
            title = { ScaledContent(factory) { Text("Endgültig löschen?") } },
            text = { ScaledContent(factory) { Text("\"${order.wer} – ${order.sorte}\" wird unwiderruflich gelöscht.") } },
            confirmButton = {
                ScaledContent(factory) {
                    TextButton(onClick = { viewModel.deleteOrderPermanently(context, order); confirmDeleteOrder = null }) {
                        Text("Löschen", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = { ScaledContent(factory) { TextButton(onClick = { confirmDeleteOrder = null }) { Text("Abbrechen") } } }
        )
    }

    confirmDeleteYear?.let { jahr ->
        AlertDialog(
            onDismissRequest = { confirmDeleteYear = null },
            title = { ScaledContent(factory) { Text("Jahr $jahr endgültig löschen?") } },
            text = { ScaledContent(factory) { Text("Alle Vorbestellungen aus $jahr werden unwiderruflich gelöscht.") } },
            confirmButton = {
                ScaledContent(factory) {
                    TextButton(onClick = { viewModel.deleteYearPermanently(context, jahr); confirmDeleteYear = null }) {
                        Text("Alle löschen", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = { ScaledContent(factory) { TextButton(onClick = { confirmDeleteYear = null }) { Text("Abbrechen") } } }
        )
    }
}
