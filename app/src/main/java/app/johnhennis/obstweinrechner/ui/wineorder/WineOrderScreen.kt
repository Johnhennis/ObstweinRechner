package app.johnhennis.obstweinrechner.ui.wineorder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import app.johnhennis.obstweinrechner.data.WineOrder
import app.johnhennis.obstweinrechner.data.WineOrderItem
import app.johnhennis.obstweinrechner.notifications.parseWannZeitpunkt
import app.johnhennis.obstweinrechner.ui.AppViewModelFactory
import app.johnhennis.obstweinrechner.ui.common.ScaledContent
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

private fun fmtNum(v: Double): String = if (v == 0.0) "" else v.toLong().toString()

private fun displayZeitpunkt(iso: String): String {
    val dt = parseWannZeitpunkt(iso) ?: return ""
    return dt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
}

private fun displayErinnerungen(stunden: List<Int>): String {
    if (stunden.isEmpty()) return "Keine Erinnerung"
    return stunden.sortedDescending().joinToString(", ") { "${it}h" }
}

private fun sortenSummary(order: WineOrder): String =
    order.positionen.joinToString(", ") { it.sorte }.ifBlank { "–" }

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
                                factory = factory,
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
            onAdd = { jahr, wer, positionen, wann, erinnerungen ->
                viewModel.addOrder(context, jahr, wer, positionen, wann, erinnerungen)
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
            text = { ScaledContent(factory) { Text("\"${order.wer} – ${sortenSummary(order)}\" wandert in den Papierkorb, geplante Erinnerungen werden storniert.") } },
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
    factory: AppViewModelFactory,
    order: WineOrder,
    onUpdate: (WineOrder) -> Unit,
    onDelete: () -> Unit,
    onFocusGained: () -> Unit
) {
    var wer by remember(order.id) { mutableStateOf(order.wer) }
    val positionen = remember(order.id) {
        androidx.compose.runtime.mutableStateListOf<Pair<String, String>>().apply {
            if (order.positionen.isEmpty()) add("" to "")
            else order.positionen.forEach { add(it.sorte to fmtNum(it.menge)) }
        }
    }
    var userEdited by remember(order.id) { mutableStateOf(false) }
    var showWannPicker by remember { mutableStateOf(false) }
    var showReminders by remember { mutableStateOf(false) }

    fun buildPositionen(): List<WineOrderItem> = positionen.mapNotNull { (sorte, mengeText) ->
        if (sorte.isBlank() && mengeText.isBlank()) null
        else WineOrderItem(sorte = sorte, menge = mengeText.toIntOrNull()?.toDouble() ?: 0.0)
    }

    LaunchedEffect(wer, positionen.toList()) {
        if (userEdited) {
            kotlinx.coroutines.delay(500)
            onUpdate(order.copy(wer = wer, positionen = buildPositionen()))
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
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Bestellung entfernen", modifier = Modifier.size(18.dp))
            }
        }

        positionen.forEachIndexed { index, (sorte, mengeText) ->
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RowField(sorte, { positionen[index] = it to mengeText; userEdited = true }, "Sorte", Modifier.weight(1.3f), onFocus = onFocusGained)
                RowField(
                    mengeText,
                    { new -> if (new.isEmpty() || new.matches(Regex("^[0-9]*$"))) { positionen[index] = sorte to new; userEdited = true } },
                    "Menge", Modifier.weight(0.8f),
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number, onFocus = onFocusGained
                )
                IconButton(
                    onClick = { if (positionen.size > 1) { positionen.removeAt(index); userEdited = true } },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Sorte entfernen", modifier = Modifier.size(16.dp))
                }
            }
        }
        TextButton(onClick = { positionen.add("" to ""); userEdited = true }, modifier = Modifier.padding(top = 2.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("Sorte hinzufügen", style = MaterialTheme.typography.bodySmall)
        }

        Row(
            modifier = Modifier.padding(top = 4.dp).fillMaxWidth().clickable { showWannPicker = true },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                if (order.wannZeitpunkt.isBlank()) "Termin wählen" else displayZeitpunkt(order.wannZeitpunkt),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(
            modifier = Modifier.padding(top = 4.dp).fillMaxWidth().clickable {
                if (order.wannZeitpunkt.isNotBlank()) showReminders = true
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                if (order.wannZeitpunkt.isBlank()) "Erst Termin wählen" else displayErinnerungen(order.erinnerungenStunden),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = order.abgefuellt, onCheckedChange = { onUpdate(order.copy(wer = wer, positionen = buildPositionen(), abgefuellt = it)) })
            Text("Abgefüllt", style = MaterialTheme.typography.bodyMedium)
            Checkbox(checked = order.abgeholt, onCheckedChange = { onUpdate(order.copy(wer = wer, positionen = buildPositionen(), abgeholt = it)) })
            Text("Abgeholt", style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (showWannPicker) {
        WannPicker(
            initial = order.wannZeitpunkt,
            onDismiss = { showWannPicker = false },
            onConfirm = { iso -> onUpdate(order.copy(wer = wer, positionen = buildPositionen(), wannZeitpunkt = iso)); showWannPicker = false }
        )
    }

    if (showReminders) {
        RemindersDialog(
            factory = factory,
            current = order.erinnerungenStunden,
            onSave = { list -> onUpdate(order.copy(wer = wer, positionen = buildPositionen(), erinnerungenStunden = list)); showReminders = false },
            onDismiss = { showReminders = false }
        )
    }
}

@Composable
private fun RowField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    onFocus: () -> Unit = {}
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        textStyle = MaterialTheme.typography.bodySmall,
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.onFocusChanged { if (it.isFocused) onFocus() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WannPicker(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val initialParsed = parseWannZeitpunkt(initial)
    var step by remember { mutableStateOf(1) }
    var selectedDate by remember { mutableStateOf<java.time.LocalDate?>(null) }

    val dateState = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = initialParsed
            ?.toLocalDate()
            ?.atStartOfDay(java.time.ZoneOffset.UTC)
            ?.toInstant()
            ?.toEpochMilli()
            ?: System.currentTimeMillis()
    )
    val timeState = androidx.compose.material3.rememberTimePickerState(
        initialHour = initialParsed?.hour ?: 12,
        initialMinute = initialParsed?.minute ?: 0,
        is24Hour = true
    )

    if (step == 1) {
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val millis = dateState.selectedDateMillis ?: return@TextButton
                    selectedDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    step = 2
                }) { Text("Weiter") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
        ) {
            androidx.compose.material3.DatePicker(state = dateState)
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Uhrzeit wählen") },
            text = { androidx.compose.material3.TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val date = selectedDate ?: return@TextButton
                    val dateTime = java.time.LocalDateTime.of(date, java.time.LocalTime.of(timeState.hour, timeState.minute))
                    onConfirm(dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")))
                }) { Text("Übernehmen") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
        )
    }
}

@Composable
private fun RemindersDialog(
    factory: AppViewModelFactory,
    current: List<Int>,
    onSave: (List<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    var reminders by remember { mutableStateOf(current.sortedDescending()) }
    var newHours by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { ScaledContent(factory) { Text("Erinnerungen") } },
        text = {
            ScaledContent(factory) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Wie viele Stunden vor dem Termin erinnert werden soll. Mehrere Erinnerungen möglich.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (reminders.isEmpty()) {
                        Text("Noch keine Erinnerung angelegt.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        reminders.forEach { hours ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (hours == 1) "1 Stunde vorher" else "$hours Stunden vorher")
                                IconButton(onClick = { reminders = reminders - hours }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Entfernen")
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.OutlinedTextField(
                            value = newHours,
                            onValueChange = { new -> if (new.isEmpty() || new.matches(Regex("^[0-9]{0,2}$"))) newHours = new },
                            label = { Text("Std. vorher (1-24)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            val h = newHours.toIntOrNull()
                            when {
                                h == null || h !in 1..24 -> error = "Bitte eine Zahl von 1 bis 24 eingeben."
                                h in reminders -> error = "Diese Erinnerung gibt es schon."
                                else -> { reminders = (reminders + h).sortedDescending(); newHours = ""; error = null }
                            }
                        }) { Text("Hinzufügen") }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            }
        },
        confirmButton = { ScaledContent(factory) { TextButton(onClick = { onSave(reminders) }) { Text("Speichern") } } },
        dismissButton = { ScaledContent(factory) { TextButton(onClick = onDismiss) { Text("Abbrechen") } } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWineOrderDialog(
    factory: AppViewModelFactory,
    defaultYear: Int,
    onDismiss: () -> Unit,
    onAdd: (Int, String, List<WineOrderItem>, String, List<Int>) -> Unit
) {
    var jahr by remember { mutableStateOf(defaultYear.toString()) }
    var wer by remember { mutableStateOf("") }
    val positionen = remember { androidx.compose.runtime.mutableStateListOf("" to "") }
    var wannZeitpunkt by remember { mutableStateOf("") }
    var erinnerungen by remember { mutableStateOf(listOf<Int>()) }
    var showWannPicker by remember { mutableStateOf(false) }
    var showReminders by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { ScaledContent(factory) { Text("Neue Vorbestellung") } },
        text = {
            ScaledContent(factory) {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = jahr,
                        onValueChange = { new -> if (new.isEmpty() || new.matches(Regex("^[0-9]{0,4}$"))) jahr = new },
                        label = { Text("Jahr") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    androidx.compose.material3.OutlinedTextField(value = wer, onValueChange = { wer = it }, label = { Text("Wer") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    positionen.forEachIndexed { index, (sorte, mengeText) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.OutlinedTextField(
                                value = sorte, onValueChange = { positionen[index] = it to mengeText },
                                label = { Text("Sorte") }, singleLine = true, modifier = Modifier.weight(1.3f)
                            )
                            androidx.compose.material3.OutlinedTextField(
                                value = mengeText,
                                onValueChange = { new -> if (new.isEmpty() || new.matches(Regex("^[0-9]*$"))) positionen[index] = sorte to new },
                                label = { Text("Menge") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                singleLine = true, modifier = Modifier.weight(0.8f)
                            )
                            IconButton(onClick = { if (positionen.size > 1) positionen.removeAt(index) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Sorte entfernen", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    TextButton(onClick = { positionen.add("" to "") }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Sorte hinzufügen", style = MaterialTheme.typography.bodySmall)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showWannPicker = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                        Text(if (wannZeitpunkt.isBlank()) "Termin wählen (optional)" else displayZeitpunkt(wannZeitpunkt))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { if (wannZeitpunkt.isNotBlank()) showReminders = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Notifications, contentDescription = null)
                        Text(
                            if (wannZeitpunkt.isBlank()) "Erst Termin wählen" else displayErinnerungen(erinnerungen),
                            style = MaterialTheme.typography.bodySmall
                        )
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
                    val items = positionen.mapNotNull { (sorte, mengeText) ->
                        if (sorte.isBlank() && mengeText.isBlank()) null
                        else WineOrderItem(sorte = sorte.trim(), menge = mengeText.toIntOrNull()?.toDouble() ?: 0.0)
                    }
                    onAdd(jahrValue, wer.trim(), items, wannZeitpunkt, erinnerungen)
                }) { Text("Hinzufügen") }
            }
        },
        dismissButton = { ScaledContent(factory) { TextButton(onClick = onDismiss) { Text("Abbrechen") } } }
    )

    if (showWannPicker) {
        WannPicker(
            initial = wannZeitpunkt,
            onDismiss = { showWannPicker = false },
            onConfirm = { iso -> wannZeitpunkt = iso; showWannPicker = false }
        )
    }

    if (showReminders) {
        RemindersDialog(
            factory = factory,
            current = erinnerungen,
            onSave = { list -> erinnerungen = list; showReminders = false },
            onDismiss = { showReminders = false }
        )
    }
}
