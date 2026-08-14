package app.johnhennis.obstweinrechner.ui.wineorder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import app.johnhennis.obstweinrechner.data.WineOrder
import app.johnhennis.obstweinrechner.ui.AppViewModelFactory
import app.johnhennis.obstweinrechner.ui.common.ScaledContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private fun fmtNum(v: Double): String = if (v == 0.0) "" else v.toLong().toString()

private fun displayDate(iso: String): String = if (iso.isBlank()) "" else try {
    java.time.LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
} catch (e: Exception) {
    iso
}

private sealed class WineOrderListEntry {
    data class YearHeader(val jahr: Int, val count: Int) : WineOrderListEntry()
    data class Row(val order: WineOrder) : WineOrderListEntry()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WineOrderScreen(
    factory: AppViewModelFactory,
    onOpenMenu: () -> Unit,
    onOpenTrash: () -> Unit
) {
    val viewModel: WineOrderViewModel = viewModel(factory = factory)
    val context = LocalContext.current
    val yearGroups by viewModel.yearGroups.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var confirmDeleteYear by remember { mutableStateOf<Int?>(null) }
    var confirmDeleteOrder by remember { mutableStateOf<WineOrder?>(null) }
    var expandedYear by rememberSaveable { mutableStateOf<Int?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    LaunchedEffect(yearGroups) {
        if (yearGroups.isNotEmpty()) viewModel.rescheduleAllPending(context)
    }

    val flatList = remember(yearGroups, expandedYear) {
        buildList {
            yearGroups.forEach { group ->
                add(WineOrderListEntry.YearHeader(group.jahr, group.orders.size))
                if (group.jahr == expandedYear) {
                    group.orders.forEach { add(WineOrderListEntry.Row(it)) }
                }
            }
        }
    }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weinvorbestellung") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                actions = {
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Vorbestellung hinzufügen")
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
                Text("Noch keine Vorbestellungen erfasst. Oben rechts mit + eine neue hinzufügen.", style = MaterialTheme.typography.bodyMedium)
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
                            is WineOrderListEntry.YearHeader -> "header_${entry.jahr}"
                            is WineOrderListEntry.Row -> entry.order.id
                        }
                    }
                ) { index, entry ->
                    when (entry) {
                        is WineOrderListEntry.YearHeader -> {
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
                                HorizontalDivider(modifier = Modifier.padding(bottom = 6.dp))
                            }
                        }
                        is WineOrderListEntry.Row -> {
                            WineOrderRow(
                                order = entry.order,
                                onUpdate = { viewModel.updateOrder(context, it) },
                                onDelete = { confirmDeleteOrder = entry.order },
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
        AddWineOrderDialog(
            factory = factory,
            defaultYear = viewModel.currentYear,
            onDismiss = { showAdd = false },
            onAdd = { jahr, wer, sorte, menge, wann ->
                viewModel.addOrder(context, jahr, wer, sorte, menge, wann)
                expandedYear = jahr
                showAdd = false
            }
        )
    }

    confirmDeleteYear?.let { jahr ->
        AlertDialog(
            onDismissRequest = { confirmDeleteYear = null },
            title = { ScaledContent(factory) { Text("Jahr $jahr in den Papierkorb?") } },
            text = { ScaledContent(factory) { Text("Alle Vorbestellungen aus $jahr wandern in den Papierkorb, geplante Erinnerungen werden storniert.") } },
            confirmButton = {
                ScaledContent(factory) {
                    TextButton(onClick = { viewModel.deleteYear(context, jahr); confirmDeleteYear = null }) {
                        Text("In den Papierkorb", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = { ScaledContent(factory) { TextButton(onClick = { confirmDeleteYear = null }) { Text("Abbrechen") } } }
        )
    }

    confirmDeleteOrder?.let { order ->
        AlertDialog(
            onDismissRequest = { confirmDeleteOrder = null },
            title = { ScaledContent(factory) { Text("In den Papierkorb verschieben?") } },
            text = { ScaledContent(factory) { Text("\"${order.wer} – ${order.sorte}\" wandert in den Papierkorb, eine geplante Erinnerung wird storniert.") } },
            confirmButton = {
                ScaledContent(factory) {
                    TextButton(onClick = { viewModel.deleteOrder(context, order); confirmDeleteOrder = null }) {
                        Text("In den Papierkorb", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = { ScaledContent(factory) { TextButton(onClick = { confirmDeleteOrder = null }) { Text("Abbrechen") } } }
        )
    }
}

@Composable
private fun WineOrderRow(
    order: WineOrder,
    onUpdate: (WineOrder) -> Unit,
    onDelete: () -> Unit,
    onFocusGained: () -> Unit
) {
    var wer by remember(order.id) { mutableStateOf(order.wer) }
    var sorte by remember(order.id) { mutableStateOf(order.sorte) }
    var mengeText by remember(order.id) { mutableStateOf(fmtNum(order.menge)) }
    var userEdited by remember(order.id) { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(wer, sorte, mengeText) {
        if (userEdited) {
            delay(500)
            onUpdate(order.copy(wer = wer, sorte = sorte, menge = mengeText.toIntOrNull()?.toDouble() ?: order.menge))
        }
    }

    fun currentOrder() = order.copy(wer = wer, sorte = sorte, menge = mengeText.toIntOrNull()?.toDouble() ?: order.menge)

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
        Row(
            modifier = Modifier.padding(top = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RowField(
                mengeText,
                { new -> if (new.isEmpty() || new.matches(Regex("^[0-9]*$"))) { mengeText = new; userEdited = true } },
                "Menge", Modifier.weight(1f), keyboardType = KeyboardType.Number, onFocus = onFocusGained
            )
            Row(
                modifier = Modifier.weight(1f).clickable { showDatePicker = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    if (order.wannDatum.isBlank()) "Datum wählen" else displayDate(order.wannDatum),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Row(modifier = Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = order.abgefuellt, onCheckedChange = { onUpdate(currentOrder().copy(abgefuellt = it)) })
            Text("Abgefüllt", style = MaterialTheme.typography.bodyMedium)
            Checkbox(checked = order.abgeholt, onCheckedChange = { onUpdate(currentOrder().copy(abgeholt = it)) })
            Text("Abgeholt", style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (showDatePicker) {
        WineOrderDatePicker(
            onDismiss = { showDatePicker = false },
            onConfirm = { iso -> onUpdate(currentOrder().copy(wannDatum = iso)); showDatePicker = false }
        )
    }
}

@Composable
private fun RowField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onFocus: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        textStyle = MaterialTheme.typography.bodySmall,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.onFocusChanged { if (it.isFocused) onFocus() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WineOrderDatePicker(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis ?: return@TextButton
                val iso = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE)
                onConfirm(iso)
            }) { Text("Übernehmen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWineOrderDialog(
    factory: AppViewModelFactory,
    defaultYear: Int,
    onDismiss: () -> Unit,
    onAdd: (Int, String, String, Double, String) -> Unit
) {
    var jahr by remember { mutableStateOf(defaultYear.toString()) }
    var wer by remember { mutableStateOf("") }
    var sorte by remember { mutableStateOf("") }
    var menge by remember { mutableStateOf("") }
    var wannDatum by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { ScaledContent(factory) { Text("Neue Vorbestellung") } },
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
                    OutlinedTextField(value = wer, onValueChange = { wer = it }, label = { Text("Wer") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sorte, onValueChange = { sorte = it }, label = { Text("Sorte") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = menge,
                        onValueChange = { new -> if (new.isEmpty() || new.matches(Regex("^[0-9]*$"))) menge = new },
                        label = { Text("Menge") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.EventNote, contentDescription = null)
                        Text(if (wannDatum.isBlank()) "Wann? Datum wählen (optional)" else displayDate(wannDatum))
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            ScaledContent(factory) {
                TextButton(onClick = {
                    val jahrValue = jahr.toIntOrNull()
                    if (jahrValue == null || jahrValue < 2000) { error = "Bitte ein gültiges Jahr eingeben."; return@TextButton }
                    if (wer.isBlank()) { error = "Bitte angeben, wer bestellt hat."; return@TextButton }
                    val mengeValue = if (menge.isBlank()) 0.0 else menge.toIntOrNull()?.toDouble()
                    if (mengeValue == null) { error = "Bitte eine gültige Menge eingeben."; return@TextButton }
                    onAdd(jahrValue, wer.trim(), sorte.trim(), mengeValue, wannDatum)
                }) { Text("Hinzufügen") }
            }
        },
        dismissButton = { ScaledContent(factory) { TextButton(onClick = onDismiss) { Text("Abbrechen") } } }
    )

    if (showDatePicker) {
        WineOrderDatePicker(
            onDismiss = { showDatePicker = false },
            onConfirm = { iso -> wannDatum = iso; showDatePicker = false }
        )
    }
}
