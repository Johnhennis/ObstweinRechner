package app.johnhennis.obstweinrechner.ui.schmalz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.johnhennis.obstweinrechner.data.ExtraIngredient
import app.johnhennis.obstweinrechner.data.SchmalzCalculationResult
import app.johnhennis.obstweinrechner.data.SchmalzRecipe
import app.johnhennis.obstweinrechner.ui.AppViewModelFactory
import app.johnhennis.obstweinrechner.ui.common.ScaledContent
import java.util.Locale

private const val EPS = 0.0001

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchmalzScreen(
    factory: AppViewModelFactory,
    onOpenMenu: () -> Unit
) {
    val viewModel: SchmalzViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showEditor by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schmalz-Rechner") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditor = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Grundrezept bearbeiten")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.literText,
                onValueChange = viewModel::onLiterChanged,
                label = { Text("Ziel-Menge Schmalz (Liter)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); keyboardController?.hide() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.result != null) {
                ResultCard(uiState.result!!)
            } else {
                Text("Bitte eine gültige Literanzahl eingeben.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (showEditor && uiState.recipe != null) {
        SchmalzEditorDialog(
            factory = factory,
            recipe = uiState.recipe!!,
            onDismiss = { showEditor = false },
            onSave = { updated -> viewModel.save(updated) { showEditor = false } }
        )
    }
}

@Composable
private fun ResultCard(result: SchmalzCalculationResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Zutaten für ${fmt(result.zielLiter)} L Schmalz", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (result.schmalzStueck > EPS) ResultRow("Schmalz", "${fmt(result.schmalzStueck)} Stück")
            if (result.ruckenfettKg > EPS) ResultRow("Rückenfett", "${fmt(result.ruckenfettKg)} kg")
            if (result.aepfelStueck > EPS) ResultRow("Äpfel", "${fmt(result.aepfelStueck)} Stück")
            if (result.zwiebelnGramm > EPS) ResultRow("Zwiebeln", "${fmt(result.zwiebelnGramm)} g")
            if (result.salzGramm > EPS) ResultRow("Salz", "${fmt(result.salzGramm)} g")
            if (result.majoranGramm > EPS) ResultRow("Majoran", "${fmt(result.majoranGramm)} g")
            if (result.thymianGramm > EPS) ResultRow("Thymian", "${fmt(result.thymianGramm)} g")
            result.zusatzMengen.forEach { (name, menge) ->
                if (menge > EPS) ResultRow(name, fmt(menge))
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SchmalzEditorDialog(
    factory: AppViewModelFactory,
    recipe: SchmalzRecipe,
    onDismiss: () -> Unit,
    onSave: (SchmalzRecipe) -> Unit
) {
    var schmalz by remember { mutableStateOf(if (recipe.schmalzStueck == 0.0) "" else recipe.schmalzStueck.toString()) }
    var ruckenfett by remember { mutableStateOf(if (recipe.ruckenfettKg == 0.0) "" else recipe.ruckenfettKg.toString()) }
    var aepfel by remember { mutableStateOf(if (recipe.aepfelStueck == 0.0) "" else recipe.aepfelStueck.toString()) }
    var zwiebeln by remember { mutableStateOf(if (recipe.zwiebelnGramm == 0.0) "" else recipe.zwiebelnGramm.toString()) }
    var salz by remember { mutableStateOf(if (recipe.salzGramm == 0.0) "" else recipe.salzGramm.toString()) }
    var majoran by remember { mutableStateOf(if (recipe.majoranGramm == 0.0) "" else recipe.majoranGramm.toString()) }
    var thymian by remember { mutableStateOf(if (recipe.thymianGramm == 0.0) "" else recipe.thymianGramm.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    val extraIngredients = remember {
        mutableStateListOf<Pair<String, String>>().apply {
            recipe.zusatzZutaten.forEach { add(it.name to (if (it.menge == 0.0) "" else it.menge.toString())) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { ScaledContent(factory) { Text("Grundrezept bearbeiten") } },
        text = {
            ScaledContent(factory) {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Alle Mengen beziehen sich auf 24 Liter (Originalrezept).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SimpleNumberField("Schmalz (Stück)", schmalz) { schmalz = it }
                    SimpleNumberField("Rückenfett (kg)", ruckenfett) { ruckenfett = it }
                    SimpleNumberField("Äpfel (Stück)", aepfel) { aepfel = it }
                    SimpleNumberField("Zwiebeln (g)", zwiebeln) { zwiebeln = it }
                    SimpleNumberField("Salz (g)", salz) { salz = it }
                    SimpleNumberField("Majoran (g)", majoran) { majoran = it }
                    SimpleNumberField("Thymian (g)", thymian) { thymian = it }

                    Divider()
                    Text("Zusätzliche Zutaten", style = MaterialTheme.typography.titleSmall)

                    extraIngredients.forEachIndexed { index, (ingName, ingMenge) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = ingName,
                                onValueChange = { extraIngredients[index] = it to ingMenge },
                                label = { Text("Name") },
                                singleLine = true,
                                modifier = Modifier.width(170.dp)
                            )
                            OutlinedTextField(
                                value = ingMenge,
                                onValueChange = { new ->
                                    if (new.isEmpty() || new.matches(Regex("^[0-9]*[.,]?[0-9]*$"))) {
                                        extraIngredients[index] = ingName to new
                                    }
                                },
                                label = { Text("Menge") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.width(100.dp)
                            )
                            IconButton(onClick = { extraIngredients.removeAt(index) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Entfernen")
                            }
                        }
                    }

                    TextButton(onClick = { extraIngredients.add("" to "") }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text("Zutat hinzufügen")
                    }

                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            ScaledContent(factory) {
                TextButton(onClick = {
                    fun parse(s: String) = if (s.isBlank()) 0.0 else s.replace(',', '.').toDoubleOrNull()
                    val sm = parse(schmalz)
                    val rf = parse(ruckenfett)
                    val ae = parse(aepfel)
                    val zw = parse(zwiebeln)
                    val sa = parse(salz)
                    val ma = parse(majoran)
                    val th = parse(thymian)
                    if (sm == null || rf == null || ae == null || zw == null || sa == null || ma == null || th == null) {
                        error = "Bitte nur gültige Zahlen eingeben."
                        return@TextButton
                    }
                    val zusatz = extraIngredients.mapNotNull { (n, m) ->
                        val menge = if (m.isBlank()) 0.0 else m.replace(',', '.').toDoubleOrNull()
                        if (n.isNotBlank() && menge != null) ExtraIngredient(n.trim(), menge) else null
                    }
                    onSave(
                        recipe.copy(
                            schmalzStueck = sm, ruckenfettKg = rf, aepfelStueck = ae, zwiebelnGramm = zw,
                            salzGramm = sa, majoranGramm = ma, thymianGramm = th, zusatzZutaten = zusatz
                        )
                    )
                }) { Text("Speichern") }
            }
        },
        dismissButton = {
            ScaledContent(factory) {
                TextButton(onClick = onDismiss) { Text("Abbrechen") }
            }
        }
    )
}

@Composable
private fun SimpleNumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            if (new.isEmpty() || new.matches(Regex("^[0-9]*[.,]?[0-9]*$"))) onValueChange(new)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun fmt(value: Double): String = String.format(Locale.GERMANY, "%.2f", value)
