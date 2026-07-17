package com.example.fruchtweinrechner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.fruchtweinrechner.FruchtweinApplication
import com.example.fruchtweinrechner.data.FruitRecipeRepository
import com.example.fruchtweinrechner.data.SchmalzRecipeRepository
import com.example.fruchtweinrechner.data.SettingsRepository
import com.example.fruchtweinrechner.ui.calculator.CalculatorViewModel
import com.example.fruchtweinrechner.ui.recipes.RecipeEditorViewModel
import com.example.fruchtweinrechner.ui.recipes.RecipeListViewModel
import com.example.fruchtweinrechner.ui.schmalz.SchmalzViewModel
import com.example.fruchtweinrechner.ui.settings.SettingsViewModel

class AppViewModelFactory(
    private val repository: FruitRecipeRepository,
    private val schmalzRepository: SchmalzRecipeRepository,
    private val settingsRepository: SettingsRepository
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

            modelClass.isAssignableFrom(SchmalzViewModel::class.java) ->
                SchmalzViewModel(schmalzRepository) as T

            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(settingsRepository) as T

            else -> throw IllegalArgumentException("Unbekannte ViewModel-Klasse: ${modelClass.name}")
        }
    }

    companion object {
        fun from(application: FruchtweinApplication) = AppViewModelFactory(
            application.repository,
            application.schmalzRepository,
            application.settingsRepository
        )
    }
}
