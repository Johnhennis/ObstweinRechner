package com.example.fruchtweinrechner.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruchtweinrechner.data.FruitRecipe
import com.example.fruchtweinrechner.data.FruitRecipeRepository
import kotlinx.coroutines.launch

class RecipeEditorViewModel(
    private val repository: FruitRecipeRepository
) : ViewModel() {

    /**
     * Speichert ein neues oder bestehendes Rezept.
     * Ist [recipe.id] == 0, wird ein neuer Datensatz angelegt, ansonsten aktualisiert.
     */
    fun save(recipe: FruitRecipe, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            if (recipe.id == 0L) {
                repository.insert(recipe)
            } else {
                repository.update(recipe)
            }
            onSaved()
        }
    }

    fun delete(recipe: FruitRecipe, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.delete(recipe)
            onDeleted()
        }
    }
}
