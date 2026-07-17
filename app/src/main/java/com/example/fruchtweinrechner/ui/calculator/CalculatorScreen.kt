package com.example.fruchtweinrechner.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fruchtweinrechner.data.CalculationResult
import com.example.fruchtweinrechner.data.FruitRecipe
import com.example.fruchtweinrechner.ui.AppViewModelFactory
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    factory: AppViewModelFactory,
    onOpenRecipes: () -> Unit
) {
    val viewModel: CalculatorViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fruchtwein-Rechner") },
                actions = {
                    TextButton(onClick = onOpenRecipes) { Text("Rezepte") }
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
            FruitDropdown(
                recipes = uiState.recipes,
                selected = uiState.selectedRecipe,
                onSelected = viewModel::onRecipeSelected
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = uiState.mode == InputMode.LITER,
                    onClick = { viewModel.onModeChanged(InputMode.LITER) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Ziel-Liter") }
                SegmentedButton(
                    selected = uiState.mode == InputMode.FRUCHT_KG,
                    onClick = { viewModel.onModeChanged(InputMode.FRUCHT_KG) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("kg Frucht") }
            }

            when (uiState.mode) {
                InputMode.LITER -> OutlinedTextField(
                    value = uiState.literText,
                    onValueChange = viewModel::onLiterChanged,
                    label = { Text("Ziel-Menge Wein (Liter)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); keyboardController?.hide() }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                InputMode.FRUCHT_KG -> OutlinedTextField(
                    value = uiState.fruchtKgText,
                    onValueChange = viewModel::onFruchtKgChanged,
                    label = { Text("Verfügbare Frucht (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); keyboardController?.hide() }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.selectedRecipe != null && uiState.result != null) {
                ResultCard(recipeName = uiState.selectedRecipe!!.name, result = uiState.result!!)
            } else {
                Text(
                    "Bitte Frucht auswählen und eine gültige Menge eingeben.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FruitDropdown(
    recipes: List<FruitRecipe>,
    selected: FruitRecipe?,
    onSelected: (FruitRecipe) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Frucht") },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            recipes.forEach { recipe ->
                DropdownMenuItem(
                    text = { Text(recipe.name) },
                    onClick = {
                        onSelected(recipe)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ResultCard(recipeName: String, result: CalculationResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Rezept für $recipeName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ResultRow("Ziel-Menge Wein", "${fmt(result.zielLiter)} L")
            ResultRow("Fruchtmenge", "${fmt(result.fruchtKg)} kg")
            ResultRow("Ausbeute Saft", "${fmt(result.saftLiter)} L")
            ResultRow("Wasser", "${fmt(result.wasserLiter)} L")
            ResultRow("Zucker", "${fmt(result.zuckerKg)} kg")
            ResultRow("Milchsäure", "${fmt(result.milchsaeureGramm)} g")
            ResultRow("Antigel klein", "${fmt(result.antigelKleinMl)} ml")
            ResultRow("Antigel groß", "${fmt(result.antigelGrossMl)} ml")
            ResultRow("Hefe", result.hefeSorte.ifBlank { "–" })
            result.zusatzMengen.forEach { (name, menge) ->
                ResultRow(name, fmt(menge))
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

private fun fmt(value: Double): String = String.format(Locale.GERMANY, "%.2f", value)
