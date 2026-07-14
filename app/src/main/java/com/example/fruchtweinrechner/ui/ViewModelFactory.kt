package com.example.fruchtweinrechner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.fruchtweinrechner.FruchtweinApplication
import com.example.fruchtweinrechner.data.FruitRecipeRepository
import com.example.fruchtweinrechner.ui.calculator.CalculatorViewModel
import com.example.fruchtweinrechner.ui.recipes.RecipeEditorViewModel
import com.example.fruchtweinrechner.ui.recipes.RecipeListViewModel

/**
 * Einfache, zentrale ViewModel-Factory ohne DI-Framework (Hilt o.ä.).
 * Liest das Repository aus der Application-Klasse.
 */
class AppViewModelFactory(
    private val repository: FruitRecipeRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when {
            modelClass.isAssignableFrom(CalculatorViewModel::class.java) ->
                CalculatorViewModel(repository) as T

            modelClass.isAssignableFrom(RecipeListViewModel::class.java) ->
                RecipeListViewModel(repository) as T

            modelClass.isAssignableFrom(RecipeEditorViewModel::class.java) ->
                RecipeEditorViewModel(repository) as T

            else -> throw IllegalArgumentException("Unbekannte ViewModel-Klasse: ${modelClass.name}")
        }
    }

    companion object {
        fun from(application: FruchtweinApplication) = AppViewModelFactory(application.repository)
    }
}
