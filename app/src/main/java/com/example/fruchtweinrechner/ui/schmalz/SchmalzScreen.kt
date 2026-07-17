package com.example.fruchtweinrechner.ui.schmalz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fruchtweinrechner.data.SchmalzCalculationResult
import com.example.fruchtweinrechner.data.SchmalzRecipe
import com.example.fruchtweinrechner.ui.AppViewModelFactory
import java.util.Locale

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
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
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
            ResultRow("Rückenfett", "${fmt(result.ruckenfettKg)} kg")
            ResultRow("Äpfel", "${fmt(result.aepfelStueck)} Stück")
            ResultRow("Zwiebeln", "${fmt(result.zwiebelnGramm)} g")
            ResultRow("Salz", "${fmt(result.salzGramm)} g")
            ResultRow("Majoran", "${fmt(result.majoranGramm)} g")
            ResultRow("Thymian", "${fmt(result.thymianGramm)} g")
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
    recipe: SchmalzRecipe,
    onDismiss: () -> Unit,
    onSave: (SchmalzRecipe) -> Unit
) {
    var ruckenfett by remember { mutableStateOf(recipe.ruckenfettKg.toString()) }
    var aepfel by remember { mutableStateOf(recipe.aepfelStueck.toString()) }
    var zwiebeln by remember { mutableStateOf(recipe.zwiebelnGramm.toString()) }
    var salz by remember { mutableStateOf(recipe.salzGramm.toString()) }
    var majoran by remember { mutableStateOf(recipe.majoranGramm.toString()) }
    var thymian by remember { mutableStateOf(recipe.thymianGramm.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Grundrezept bearbeiten") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Alle Mengen beziehen sich auf 24 Liter (Originalrezept).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SimpleNumberField("Rückenfett (kg)", ruckenfett) { ruckenfett = it }
                SimpleNumberField("Äpfel (Stück)", aepfel) { aepfel = it }
                SimpleNumberField("Zwiebeln (g)", zwiebeln) { zwiebeln = it }
                SimpleNumberField("Salz (g)", salz) { salz = it }
                SimpleNumberField("Majoran (g)", majoran) { majoran = it }
                SimpleNumberField("Thymian (g)", thymian) { thymian = it }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                fun parse(s: String) = if (s.isBlank()) 0.0 else s.replace(',', '.').toDoubleOrNull()
                val rf = parse(ruckenfett)
                val ae = parse(aepfel)
                val zw = parse(zwiebeln)
                val sa = parse(salz)
                val ma = parse(majoran)
                val th = parse(thymian)
                if (rf == null || ae == null || zw == null || sa == null || ma == null || th == null) {
                    error = "Bitte nur gültige Zahlen eingeben."
                    return@TextButton
                }
                onSave(recipe.copy(ruckenfettKg = rf, aepfelStueck = ae, zwiebelnGramm = zw, salzGramm = sa, majoranGramm = ma, thymianGramm = th))
            }) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
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
