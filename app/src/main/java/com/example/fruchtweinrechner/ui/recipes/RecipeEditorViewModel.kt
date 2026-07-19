package com.example.fruchtweinrechner.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruchtweinrechner.data.FruitRecipe
import com.example.fruchtweinrechner.data.FruitRecipeRepository
import kotlinx.coroutines.launch

class RecipeEditorViewModel(
    private val repository: FruitRecipeRepository
) : ViewModel() {

    fun save(recipe: FruitRecipe, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            if (recipe.id.isEmpty()) {
                repository.insert(recipe)
            } else {
                repository.update(recipe)
            }
            onSaved()
        }
    }

    // Verschiebt in den Papierkorb, statt sofort endgültig zu löschen.
    fun delete(recipe: FruitRecipe, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.moveToTrash(recipe)
            onDeleted()
        }
    }
}
