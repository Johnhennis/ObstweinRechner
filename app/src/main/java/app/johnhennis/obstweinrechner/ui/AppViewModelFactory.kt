package app.johnhennis.obstweinrechner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import app.johnhennis.obstweinrechner.FruchtweinApplication
import app.johnhennis.obstweinrechner.data.FruitRecipeRepository
import app.johnhennis.obstweinrechner.data.InventoryRepository
import app.johnhennis.obstweinrechner.data.SchmalzRecipeRepository
import app.johnhennis.obstweinrechner.data.SettingsRepository
import app.johnhennis.obstweinrechner.data.ShoppingListRepository
import app.johnhennis.obstweinrechner.ui.calculator.CalculatorViewModel
import app.johnhennis.obstweinrechner.ui.inventory.InventoryViewModel
import app.johnhennis.obstweinrechner.ui.recipes.RecipeEditorViewModel
import app.johnhennis.obstweinrechner.ui.recipes.RecipeListViewModel
import app.johnhennis.obstweinrechner.ui.schmalz.SchmalzViewModel
import app.johnhennis.obstweinrechner.ui.settings.SettingsViewModel
import app.johnhennis.obstweinrechner.ui.shopping.ShoppingListViewModel

class AppViewModelFactory(
    private val repository: FruitRecipeRepository,
    private val schmalzRepository: SchmalzRecipeRepository,
    private val settingsRepository: SettingsRepository,
    private val inventoryRepository: InventoryRepository,
    private val shoppingListRepository: ShoppingListRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when {
            modelClass.isAssignableFrom(CalculatorViewModel::class.java) -> CalculatorViewModel(repository) as T
            modelClass.isAssignableFrom(RecipeListViewModel::class.java) -> RecipeListViewModel(repository) as T
            modelClass.isAssignableFrom(RecipeEditorViewModel::class.java) -> RecipeEditorViewModel(repository) as T
            modelClass.isAssignableFrom(SchmalzViewModel::class.java) -> SchmalzViewModel(schmalzRepository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(settingsRepository) as T
            modelClass.isAssignableFrom(InventoryViewModel::class.java) -> InventoryViewModel(inventoryRepository) as T
            modelClass.isAssignableFrom(ShoppingListViewModel::class.java) -> ShoppingListViewModel(inventoryRepository, shoppingListRepository) as T
            else -> throw IllegalArgumentException("Unbekannte ViewModel-Klasse: ${modelClass.name}")
        }
    }

    companion object {
        fun from(application: FruchtweinApplication) = AppViewModelFactory(
            application.repository,
            application.schmalzRepository,
            application.settingsRepository,
            application.inventoryRepository,
            application.shoppingListRepository
        )
    }
}
