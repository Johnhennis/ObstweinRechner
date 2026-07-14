package com.example.fruchtweinrechner.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fruchtwein-Rechner") },
                actions = {
                    androidx.compose.material3.TextButton(onClick = onOpenRecipes) {
                        Text("Rezepte")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FruitDropdown(
                recipes = uiState.recipes,
                selected = uiState.selectedRecipe,
                onSelected = viewModel::onRecipeSelected
            )

            OutlinedTextField(
                value = uiState.zielLiterText,
                onValueChange = viewModel::onZielLiterChanged,
                label = { Text("Ziel-Menge Wein (Liter)") },
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.selectedRecipe != null && uiState.result != null) {
                ResultCard(recipeName = uiState.selectedRecipe!!.name, result = uiState.result!!)
            } else {
                Text(
                    "Bitte Frucht auswählen und eine gültige Literanzahl eingeben.",
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

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Frucht") },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenuDefaults.let {
            androidx.compose.material3.ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                recipes.forEach { recipe ->
                    androidx.compose.material3.DropdownMenuItem(
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
}

@Composable
private fun ResultCard(recipeName: String, result: CalculationResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Rezept für $recipeName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            ResultRow("Saftmenge", "${fmt(result.saftLiter)} L")
            ResultRow("Fruchtmenge", "${fmt(result.fruchtKg)} kg")
            ResultRow("Wassermenge", "${fmt(result.wasserLiter)} L")
            ResultRow("Zucker", "${fmt(result.zuckerKg)} kg")
            ResultRow("Hefe", "${fmt(result.hefeGramm)} g")
            ResultRow("Hefenährsalz", "${fmt(result.naehrsalzGramm)} g")
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun fmt(value: Double): String = String.format(Locale.GERMANY, "%.2f", value)
