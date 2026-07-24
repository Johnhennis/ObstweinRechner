package app.johnhennis.obstweinrechner.ui.schmalz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.johnhennis.obstweinrechner.data.SchmalzCalculationResult
import app.johnhennis.obstweinrechner.data.SchmalzRecipe
import app.johnhennis.obstweinrechner.data.SchmalzRecipeRepository
import app.johnhennis.obstweinrechner.data.calculate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SchmalzUiState(
    val recipe: SchmalzRecipe? = null,
    val literText: String = "10",
    val result: SchmalzCalculationResult? = null
)

class SchmalzViewModel(
    private val repository: SchmalzRecipeRepository
) : ViewModel() {

    private val literText = MutableStateFlow("10")

    val uiState: StateFlow<SchmalzUiState> = combine(
        repository.recipe,
        literText
    ) { recipe, text ->
        val liter = text.replace(',', '.').toDoubleOrNull()
        val result = if (recipe != null && liter != null && liter > 0) recipe.calculate(liter) else null
        SchmalzUiState(recipe = recipe, literText = text, result = result)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SchmalzUiState()
    )

    fun onLiterChanged(text: String) {
        if (text.isEmpty() || text.matches(Regex("^[0-9]*[.,]?[0-9]*$"))) {
            literText.value = text
        }
    }

    fun save(recipe: SchmalzRecipe, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.save(recipe)
            onSaved()
        }
    }
}
