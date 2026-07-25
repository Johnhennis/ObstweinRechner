package app.johnhennis.obstweinrechner.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.johnhennis.obstweinrechner.data.CalculationResult
import app.johnhennis.obstweinrechner.data.FruitRecipe
import app.johnhennis.obstweinrechner.data.FruitRecipeRepository
import app.johnhennis.obstweinrechner.data.RecipeSession
import app.johnhennis.obstweinrechner.data.RecipeSessionRepository
import app.johnhennis.obstweinrechner.data.calculateFromFruchtKg
import app.johnhennis.obstweinrechner.data.calculateFromZielLiter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class InputMode { LITER, FRUCHT_KG }

data class CalculatorUiState(
    val recipes: List<FruitRecipe> = emptyList(),
    val selectedRecipe: FruitRecipe? = null,
    val mode: InputMode = InputMode.LITER,
    val literText: String = "10",
    val fruchtKgText: String = "10",
    val notiz: String = "",
    val result: CalculationResult? = null
)

class CalculatorViewModel(
    private val repository: FruitRecipeRepository,
    private val sessionRepository: RecipeSessionRepository
) : ViewModel() {

    private val selectedRecipeId = MutableStateFlow<String?>(null)
    private val mode = MutableStateFlow(InputMode.LITER)
    private val literText = MutableStateFlow("10")
    private val fruchtKgText = MutableStateFlow("10")
    private val notiz = MutableStateFlow("")

    // Verhindert, dass das Nachladen einer Session beim Sortenwechsel
    // sofort wieder unverändert zurückgespeichert wird.
    private var applyingSession = false

    val uiState: StateFlow<CalculatorUiState> = combine(
        repository.allRecipes,
        selectedRecipeId,
        mode,
        literText,
        fruchtKgText,
        notiz
    ) { values ->
        val recipes = values[0] as List<FruitRecipe>
        val selectedId = values[1] as String?
        val currentMode = values[2] as InputMode
        val lText = values[3] as String
        val kgText = values[4] as String
        val notizText = values[5] as String

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
            notiz = notizText,
            result = result
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalculatorUiState()
    )

    init {
        viewModelScope.launch {
            repository.allRecipes.collect { recipes ->
                if (selectedRecipeId.value == null) {
                    recipes.firstOrNull()?.let { onRecipeSelected(it) }
                }
            }
        }

        viewModelScope.launch {
            combine(literText, fruchtKgText, mode, notiz) { _, _, _, _ -> Unit }
                .debounce(500)
                .collect {
                    if (!applyingSession) saveCurrentSession()
                }
        }
    }

    fun onRecipeSelected(recipe: FruitRecipe) {
        selectedRecipeId.value = recipe.id
        viewModelScope.launch {
            applyingSession = true
            val session = sessionRepository.getOnce(recipe.id)
            mode.value = if (session.mode == "FRUCHT_KG") InputMode.FRUCHT_KG else InputMode.LITER
            literText.value = session.literText
            fruchtKgText.value = session.fruchtKgText
            notiz.value = session.notiz
            delay(600) // länger als der Debounce oben, damit das Nachladen nicht sofort zurückgeschrieben wird
            applyingSession = false
        }
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

    fun onNotizChanged(text: String) {
        notiz.value = text
    }

    private fun saveCurrentSession() {
        val recipeId = selectedRecipeId.value ?: return
        viewModelScope.launch {
            sessionRepository.save(
                RecipeSession(
                    recipeId = recipeId,
                    mode = mode.value.name,
                    literText = literText.value,
                    fruchtKgText = fruchtKgText.value,
                    notiz = notiz.value
                )
            )
        }
    }
}
