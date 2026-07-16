package com.example.fruchtweinrechner.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruchtweinrechner.data.CalculationResult
import com.example.fruchtweinrechner.data.FruitRecipe
import com.example.fruchtweinrechner.data.FruitRecipeRepository
import com.example.fruchtweinrechner.data.calculate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CalculatorUiState(
    val recipes: List<FruitRecipe> = emptyList(),
    val selectedRecipe: FruitRecipe? = null,
    val zielLiterText: String = "10",
    val result: CalculationResult? = null
)

class CalculatorViewModel(
    private val repository: FruitRecipeRepository
) : ViewModel() {

    private val selectedRecipeId = MutableStateFlow<String?>(null)
    private val zielLiterText = MutableStateFlow("10")

    val uiState: StateFlow<CalculatorUiState> = combine(
        repository.allRecipes,
        selectedRecipeId,
        zielLiterText
    ) { recipes, selectedId, literText ->
        val selected = recipes.firstOrNull { it.id == selectedId } ?: recipes.firstOrNull()
        val liter = literText.replace(',', '.').toDoubleOrNull()
        val result = if (selected != null && liter != null && liter > 0) {
            selected.calculate(liter)
        } else null

        CalculatorUiState(
            recipes = recipes,
            selectedRecipe = selected,
            zielLiterText = literText,
            result = result
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalculatorUiState()
    )

    fun onRecipeSelected(recipe: FruitRecipe) {
        selectedRecipeId.value = recipe.id
    }

    fun onZielLiterChanged(text: String) {
        // Nur sinnvolle Zeicheneingabe zulassen (Ziffern, Komma, Punkt)
        if (text.isEmpty() || text.matches(Regex("^[0-9]*[.,]?[0-9]*$"))) {
            zielLiterText.value = text
        }
    }
}
