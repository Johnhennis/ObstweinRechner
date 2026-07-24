package app.johnhennis.obstweinrechner.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.johnhennis.obstweinrechner.data.CalculationResult
import app.johnhennis.obstweinrechner.data.FruitRecipe
import app.johnhennis.obstweinrechner.data.FruitRecipeRepository
import app.johnhennis.obstweinrechner.data.calculateFromFruchtKg
import app.johnhennis.obstweinrechner.data.calculateFromZielLiter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class InputMode { LITER, FRUCHT_KG }

data class CalculatorUiState(
    val recipes: List<FruitRecipe> = emptyList(),
    val selectedRecipe: FruitRecipe? = null,
    val mode: InputMode = InputMode.LITER,
    val literText: String = "10",
    val fruchtKgText: String = "10",
    val result: CalculationResult? = null
)

class CalculatorViewModel(
    private val repository: FruitRecipeRepository
) : ViewModel() {

    private val selectedRecipeId = MutableStateFlow<String?>(null)
    private val mode = MutableStateFlow(InputMode.LITER)
    private val literText = MutableStateFlow("10")
    private val fruchtKgText = MutableStateFlow("10")

    val uiState: StateFlow<CalculatorUiState> = combine(
        repository.allRecipes,
        selectedRecipeId,
        mode,
        literText,
        fruchtKgText
    ) { values ->
        val recipes = values[0] as List<FruitRecipe>
        val selectedId = values[1] as String?
        val currentMode = values[2] as InputMode
        val lText = values[3] as String
        val kgText = values[4] as String

        val selected = recipes.firstOrNull { it.id == selectedId } ?: recipes.firstOrNull()

        val result = selected?.let { recipe ->
            when (currentMode) {
                InputMode.LITER -> lText.replace(',', '.').toDoubleOrNull()
                    ?.takeIf { it > 0 }
                    ?.let { recipe.calculateFromZielLiter(it) }

                InputMode.FRUCHT_KG -> kgText.replace(',', '.').toDoubleOrNull()
                    ?.takeIf { it > 0 }
                    ?.let { recipe.calculateFromFruchtKg(it) }
            }
        }

        CalculatorUiState(
            recipes = recipes,
            selectedRecipe = selected,
            mode = currentMode,
            literText = lText,
            fruchtKgText = kgText,
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

    fun onModeChanged(newMode: InputMode) {
        mode.value = newMode
    }

    fun onLiterChanged(text: String) {
        if (text.isEmpty() || text.matches(Regex("^[0-9]*[.,]?[0-9]*$"))) {
            literText.value = text
        }
    }

    fun onFruchtKgChanged(text: String) {
        if (text.isEmpty() || text.matches(Regex("^[0-9]*[.,]?[0-9]*$"))) {
            fruchtKgText.value = text
        }
    }
}
