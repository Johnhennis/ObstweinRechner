package com.example.fruchtweinrechner.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruchtweinrechner.data.FruitRecipe
import com.example.fruchtweinrechner.data.FruitRecipeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecipeListViewModel(
    private val repository: FruitRecipeRepository
) : ViewModel() {

    val recipes: StateFlow<List<FruitRecipe>> = repository.allRecipes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val trashedRecipes: StateFlow<List<FruitRecipe>> = repository.trashedRecipes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun deleteRecipe(recipe: FruitRecipe) {
        viewModelScope.launch { repository.moveToTrash(recipe) }
    }

    fun restore(recipe: FruitRecipe) {
        viewModelScope.launch { repository.restore(recipe) }
    }

    fun deletePermanently(recipe: FruitRecipe) {
        viewModelScope.launch { repository.deletePermanently(recipe) }
    }
}
