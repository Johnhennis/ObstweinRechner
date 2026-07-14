package com.example.fruchtweinrechner.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fruchtweinrechner.data.FruitRecipe
import com.example.fruchtweinrechner.ui.AppViewModelFactory

/**
 * Dialog zum Anlegen (recipe == null) oder Bearbeiten (recipe != null) eines Frucht-Rezepts.
 * Alle Faktoren sind hier frei editierbar und werden über das ViewModel persistiert.
 */
@Composable
fun RecipeEditorDialog(
    factory: AppViewModelFactory,
    recipe: FruitRecipe?,
    onDismiss: () -> Unit
) {
    val viewModel: RecipeEditorViewModel = viewModel(factory = factory)

    var name by remember { mutableStateOf(recipe?.name ?: "") }
    var saftAusbeute by remember { mutableStateOf(recipe?.saftAusbeute?.toString() ?: "0.70") }
    var saftAnteil by remember { mutableStateOf(recipe?.saftAnteilImWein?.toString() ?: "0.80") }
    var zucker by remember { mutableStateOf(recipe?.zuckerProLiter?.toString() ?: "150.0") }
    var hefe by remember { mutableStateOf(recipe?.hefeProLiter?.toString() ?: "0.4") }
    var naehrsalz by remember { mutableStateOf(recipe?.naehrsalzProLiter?.toString() ?: "0.4") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (recipe == null) "Neue Frucht hinzufügen" else "Frucht bearbeiten") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                NumberField("Saftausbeute (L Saft / kg Frucht)", saftAusbeute) { saftAusbeute = it }
                NumberField("Saftanteil im Wein (0.0 – 1.0)", saftAnteil) { saftAnteil = it }
                NumberField("Zucker (g pro Liter Wein)", zucker) { zucker = it }
                NumberField("Hefe (g pro Liter Wein)", hefe) { hefe = it }
                NumberField("Hefenährsalz (g pro Liter Wein)", naehrsalz) { naehrsalz = it }

                error?.let {
                    Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = parseValues(
                    name, saftAusbeute, saftAnteil, zucker, hefe, naehrsalz
                )
                if (parsed == null) {
                    error = "Bitte alle Felder korrekt ausfüllen (Name nicht leer, Zahlen gültig)."
                    return@TextButton
                }
                val (sa, sw, z, h, n) = parsed
                val toSave = FruitRecipe(
                    id = recipe?.id ?: 0L,
                    name = name.trim(),
                    saftAusbeute = sa,
                    saftAnteilImWein = sw,
                    zuckerProLiter = z,
                    hefeProLiter = h,
                    naehrsalzProLiter = n
                )
                viewModel.save(toSave) { onDismiss() }
            }) {
                Text("Speichern")
            }
        },
        dismissButton = {
            Row {
                if (recipe != null) {
                    TextButton(onClick = {
                        viewModel.delete(recipe) { onDismiss() }
                    }) {
                        Text("Löschen", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Abbrechen")
                }
            }
        }
    )
}

@Composable
private fun Row(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    androidx.compose.foundation.layout.Row(content = content)
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            if (new.isEmpty() || new.matches(Regex("^[0-9]*[.,]?[0-9]*$"))) {
                onValueChange(new)
            }
        },
        label = { Text(label) },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

private data class ParsedValues(
    val saftAusbeute: Double,
    val saftAnteilImWein: Double,
    val zuckerProLiter: Double,
    val hefeProLiter: Double,
    val naehrsalzProLiter: Double
)

private fun parseValues(
    name: String,
    saftAusbeute: String,
    saftAnteil: String,
    zucker: String,
    hefe: String,
    naehrsalz: String
): ParsedValues? {
    if (name.isBlank()) return null
    fun parse(s: String) = s.replace(',', '.').toDoubleOrNull()
    val sa = parse(saftAusbeute) ?: return null
    val sw = parse(saftAnteil) ?: return null
    val z = parse(zucker) ?: return null
    val h = parse(hefe) ?: return null
    val n = parse(naehrsalz) ?: return null
    if (sa <= 0 || sw !in 0.0..1.0) return null
    return ParsedValues(sa, sw, z, h, n)
}
