package com.example.fruchtweinrechner.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fruchtweinrechner.data.ExtraIngredient
import com.example.fruchtweinrechner.data.FruitRecipe
import com.example.fruchtweinrechner.ui.AppViewModelFactory

private fun displayValue(value: Double?): String =
    if (value == null || value == 0.0) "" else value.toString()

// Fängt den Enter-Tastendruck der Tastatur direkt ab und springt zum nächsten Feld.
// Zuverlässiger als sich auf die IME-Aktion zu verlassen, die bei Zahlentastaturen
// von manchen Android-Tastaturen ignoriert wird.
private fun Modifier.moveFocusOnEnter(focusManager: FocusManager) = this.onPreviewKeyEvent { event ->
    if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
        focusManager.moveFocus(FocusDirection.Down)
        true
    } else {
        false
    }
}

private fun Modifier.clearFocusOnEnter(focusManager: FocusManager) = this.onPreviewKeyEvent { event ->
    if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
        focusManager.clearFocus()
        true
    } else {
        false
    }
}

@Composable
fun RecipeEditorDialog(
    factory: AppViewModelFactory,
    recipe: FruitRecipe?,
    onDismiss: () -> Unit
) {
    val viewModel: RecipeEditorViewModel = viewModel(factory = factory)
    val focusManager = LocalFocusManager.current

    var name by remember { mutableStateOf(recipe?.name ?: "") }
    var fruchtKg by remember { mutableStateOf(displayValue(recipe?.fruchtKg)) }
    var saftLiter by remember { mutableStateOf(displayValue(recipe?.saftLiter)) }
    var wasserLiter by remember { mutableStateOf(displayValue(recipe?.wasserLiter)) }
    var zuckerKg by remember { mutableStateOf(displayValue(recipe?.zuckerKg)) }
    var milchsaeure by remember { mutableStateOf(displayValue(recipe?.milchsaeureGramm)) }
    var antigelKlein by remember { mutableStateOf(displayValue(recipe?.antigelKleinMl)) }
    var antigelGross by remember { mutableStateOf(displayValue(recipe?.antigelGrossMl)) }
    var hefeSorte by remember { mutableStateOf(recipe?.hefeSorte ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    val extraIngredients = remember {
        mutableStateListOf<Pair<String, String>>().apply {
            recipe?.zusatzZutaten?.forEach { add(it.name to displayValue(it.menge)) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (recipe == null) "Neue Frucht hinzufügen" else "Frucht bearbeiten") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Alle Mengen beziehen sich auf 10 Liter fertigen Wein.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().moveFocusOnEnter(focusManager)
                )
                NumberField("Frucht (kg)", fruchtKg, focusManager) { fruchtKg = it }
                NumberField("Ausbeute Saft (L)", saftLiter, focusManager) { saftLiter = it }
                NumberField("Wasser (L)", wasserLiter, focusManager) { wasserLiter = it }
                NumberField("Zucker (kg)", zuckerKg, focusManager) { zuckerKg = it }
                NumberField("Milchsäure (g)", milchsaeure, focusManager) { milchsaeure = it }
                NumberField("Antigel klein (ml)", antigelKlein, focusManager) { antigelKlein = it }
                NumberField("Antigel groß (ml)", antigelGross, focusManager) { antigelGross = it }
                OutlinedTextField(
                    value = hefeSorte,
                    onValueChange = { hefeSorte = it },
                    label = { Text("Hefe") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().clearFocusOnEnter(focusManager)
                )

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
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            singleLine = true,
                            modifier = Modifier.width(170.dp).moveFocusOnEnter(focusManager)
                        )
                        OutlinedTextField(
                            value = ingMenge,
                            onValueChange = { new ->
                                if (new.isEmpty() || new.matches(Regex("^[0-9]*[.,]?[0-9]*$"))) {
                                    extraIngredients[index] = ingName to new
                                }
                            },
                            label = { Text("Menge") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            singleLine = true,
                            modifier = Modifier.width(100.dp).moveFocusOnEnter(focusManager)
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
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    error = "Bitte einen Namen eingeben."
                    return@TextButton
                }
                fun parse(s: String) = if (s.isBlank()) 0.0 else s.replace(',', '.').toDoubleOrNull()

                val fk = parse(fruchtKg)
                val sl = parse(saftLiter)
                val wl = parse(wasserLiter)
                val zk = parse(zuckerKg)
                val ms = parse(milchsaeure)
                val ak = parse(antigelKlein)
                val ag = parse(antigelGross)

                if (fk == null || sl == null || wl == null || zk == null || ms == null || ak == null || ag == null) {
                    error = "Bitte nur gültige Zahlen eingeben."
                    return@TextButton
                }

                val zusatz = extraIngredients.mapNotNull { (n, m) ->
                    val menge = if (m.isBlank()) 0.0 else m.replace(',', '.').toDoubleOrNull()
                    if (n.isNotBlank() && menge != null) ExtraIngredient(n.trim(), menge) else null
                }

                val toSave = FruitRecipe(
                    id = recipe?.id ?: "",
                    name = name.trim(),
                    fruchtKg = fk,
                    saftLiter = sl,
                    wasserLiter = wl,
                    zuckerKg = zk,
                    milchsaeureGramm = ms,
                    antigelKleinMl = ak,
                    antigelGrossMl = ag,
                    hefeSorte = hefeSorte.trim(),
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
private fun NumberField(
    label: String,
    value: String,
    focusManager: FocusManager,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            if (new.isEmpty() || new.matches(Regex("^[0-9]*[.,]?[0-9]*$"))) onValueChange(new)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().moveFocusOnEnter(focusManager)
    )
}
