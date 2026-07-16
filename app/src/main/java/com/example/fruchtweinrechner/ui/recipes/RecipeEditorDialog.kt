package com.example.fruchtweinrechner.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fruchtweinrechner.data.ExtraIngredient
import com.example.fruchtweinrechner.data.FruitRecipe
import com.example.fruchtweinrechner.ui.AppViewModelFactory

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
    var milchsaeure by remember { mutableStateOf(recipe?.milchsaeureProLiter?.toString() ?: "0.0") }
    var antigelKlein by remember { mutableStateOf(recipe?.antigelKleinProLiter?.toString() ?: "0.0") }
    var antigelGross by remember { mutableStateOf(recipe?.antigelGrossProLiter?.toString() ?: "0.0") }
    var hefe by remember { mutableStateOf(recipe?.hefeProLiter?.toString() ?: "0.4") }
    var hefeSorte by remember { mutableStateOf(recipe?.hefeSorte ?: "") }
    var naehrsalz by remember { mutableStateOf(recipe?.naehrsalzProLiter?.toString() ?: "0.4") }
    var error by remember { mutableStateOf<String?>(null) }

    val extraIngredients = remember {
        mutableStateListOf<Pair<String, String>>().apply {
            recipe?.zusatzZutaten?.forEach { add(it.name to it.mengeProLiter.toString()) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (recipe == null) "Neue Frucht hinzufügen" else "Frucht bearbeiten") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                NumberField("Saftausbeute (L Saft / kg Frucht)", saftAusbeute) { saftAusbeute = it }
                NumberField("Saftanteil im Wein (0.0 – 1.0)", saftAnteil) { saftAnteil = it }
                NumberField("Zucker (g pro Liter Wein)", zucker) { zucker = it }
                NumberField("Milchsäure (g pro Liter Wein)", milchsaeure) { milchsaeure = it }
                NumberField("Antigel klein (g pro Liter Wein)", antigelKlein) { antigelKlein = it }
                NumberField("Antigel groß (g pro Liter Wein)", antigelGross) { antigelGross = it }
                NumberField("Hefe (g pro Liter Wein)", hefe) { hefe = it }
                OutlinedTextField(value = hefeSorte, onValueChange = { hefeSorte = it }, label = { Text("Hefesorte / Test") }, modifier = Modifier.fillMaxWidth())
                NumberField("Hefenährsalz (g pro Liter Wein)", naehrsalz) { naehrsalz = it }

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
                            modifier = Modifier.width(170.dp)
                        )
                        OutlinedTextField(
                            value = ingMenge,
                            onValueChange = { new ->
                                if (new.isEmpty() || new.matches(Regex("^[0-9]*[.,]?[0-9]*$"))) {
                                    extraIngredients[index] = ingName to new
                                }
                            },
                            label = { Text("g/L") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(100.dp)
                        )
                        IconButton(onClick = { extraIngredients.removeAt(index) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Entfernen")
                        }
                    }
                }

                TextButton(onClick = { extraIngredients.add("" to "0.0") }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Zutat hinzufügen")
                }

                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = parseValues(name, saftAusbeute, saftAnteil, zucker, milchsaeure, antigelKlein, antigelGross, hefe, naehrsalz)
                if (parsed == null) {
                    error = "Bitte alle Felder korrekt ausfüllen (Name nicht leer, Zahlen gültig)."
                    return@TextButton
                }
                val zusatz = extraIngredients.mapNotNull { (n, m) ->
                    val menge = m.replace(',', '.').toDoubleOrNull()
                    if (n.isNotBlank() && menge != null) ExtraIngredient(n.trim(), menge) else null
                }
                val toSave = FruitRecipe(
                    id = recipe?.id ?: "",
                    name = name.trim(),
                    saftAusbeute = parsed.saftAusbeute,
                    saftAnteilImWein = parsed.saftAnteilImWein,
                    zuckerProLiter = parsed.zuckerProLiter,
                    milchsaeureProLiter = parsed.milchsaeureProLiter,
                    antigelKleinProLiter = parsed.antigelKleinProLiter,
                    antigelGrossProLiter = parsed.antigelGrossProLiter,
                    hefeProLiter = parsed.hefeProLiter,
                    hefeSorte = hefeSorte.trim(),
                    naehrsalzProLiter = parsed.naehrsalzProLiter,
                    zusatzZutaten = zusatz
                )
                viewModel.save(toSave) { onDismiss() }
            }) { Text("Speichern") }
        },
        dismissButton = {
            Row {
                if (recipe != null) {
                    TextButton(onClick = { viewModel.delete(recipe) { onDismiss() } }) {
                        Text("Löschen", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Abbrechen") }
            }
        }
    )
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            if (new.isEmpty() || new.matches(Regex("^[0-9]*[.,]?[0-9]*$"))) onValueChange(new)
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
    val milchsaeureProLiter: Double,
    val antigelKleinProLiter: Double,
    val antigelGrossProLiter: Double,
    val hefeProLiter: Double,
    val naehrsalzProLiter: Double
)

private fun parseValues(
    name: String,
    saftAusbeute: String,
    saftAnteil: String,
    zucker: String,
    milchsaeure: String,
    antigelKlein: String,
    antigelGross: String,
    hefe: String,
    naehrsalz: String
): ParsedValues? {
    if (name.isBlank()) return null
    fun parse(s: String) = s.replace(',', '.').toDoubleOrNull()
    val sa = parse(saftAusbeute) ?: return null
    val sw = parse(saftAnteil) ?: return null
    val z = parse(zucker) ?: return null
    val m = parse(milchsaeure) ?: return null
    val ak = parse(antigelKlein) ?: return null
    val ag = parse(antigelGross) ?: return null
    val h = parse(hefe) ?: return null
    val n = parse(naehrsalz) ?: return null
    if (sa <= 0 || sw !in 0.0..1.0) return null
    return ParsedValues(sa, sw, z, m, ak, ag, h, n)
}
