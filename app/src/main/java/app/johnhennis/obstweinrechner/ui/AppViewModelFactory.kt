package app.johnhennis.obstweinrechner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import app.johnhennis.obstweinrechner.FruchtweinApplication
import app.johnhennis.obstweinrechner.data.FruitPriceRepository
import app.johnhennis.obstweinrechner.data.FruitRecipeRepository
import app.johnhennis.obstweinrechner.data.InfoEntryRepository
import app.johnhennis.obstweinrechner.data.ManualShoppingItemRepository
import app.johnhennis.obstweinrechner.data.RecipeSessionRepository
import app.johnhennis.obstweinrechner.data.SchmalzRecipeRepository
import app.johnhennis.obstweinrechner.data.SettingsRepository
import app.johnhennis.obstweinrechner.data.ShoppingListRepository
import app.johnhennis.obstweinrechner.data.StockItemRepository
import app.johnhennis.obstweinrechner.data.ThemeRepository
import app.johnhennis.obstweinrechner.data.WeinprobeRepository
import app.johnhennis.obstweinrechner.data.WineOrderRepository
import app.johnhennis.obstweinrechner.data.WineStockItemRepository
import app.johnhennis.obstweinrechner.ui.calculator.CalculatorViewModel
import app.johnhennis.obstweinrechner.ui.prices.PricesViewModel
import app.johnhennis.obstweinrechner.ui.recipes.RecipeEditorViewModel
import app.johnhennis.obstweinrechner.ui.recipes.RecipeListViewModel
import app.johnhennis.obstweinrechner.ui.schmalz.SchmalzViewModel
import app.johnhennis.obstweinrechner.ui.settings.SettingsViewModel
import app.johnhennis.obstweinrechner.ui.shopping.ShoppingListViewModel
import app.johnhennis.obstweinrechner.ui.stock.StockViewModel
import app.johnhennis.obstweinrechner.ui.weinprobe.WeinprobeViewModel
import app.johnhennis.obstweinrechner.ui.wineorder.WineOrderViewModel
import app.johnhennis.obstweinrechner.ui.winestock.WineStockViewModel

class AppViewModelFactory(
    private val repository: FruitRecipeRepository,
    private val schmalzRepository: SchmalzRecipeRepository,
    private val settingsRepository: SettingsRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val fruitPriceRepository: FruitPriceRepository,
    private val infoEntryRepository: InfoEntryRepository,
    private val recipeSessionRepository: RecipeSessionRepository,
    private val manualShoppingItemRepository: ManualShoppingItemRepository,
    private val stockItemRepository: StockItemRepository,
    private val themeRepository: ThemeRepository,
    private val wineStockItemRepository: WineStockItemRepository,
    private val weinprobeRepository: WeinprobeRepository,
    private val wineOrderRepository: WineOrderRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when {
            modelClass.isAssignableFrom(CalculatorViewModel::class.java) -> CalculatorViewModel(repository, recipeSessionRepository) as T
            modelClass.isAssignableFrom(RecipeListViewModel::class.java) -> RecipeListViewModel(repository) as T
            modelClass.isAssignableFrom(RecipeEditorViewModel::class.java) -> RecipeEditorViewModel(repository) as T
            modelClass.isAssignableFrom(SchmalzViewModel::class.java) -> SchmalzViewModel(schmalzRepository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(settingsRepository, infoEntryRepository, themeRepository) as T
            modelClass.isAssignableFrom(ShoppingListViewModel::class.java) -> ShoppingListViewModel(stockItemRepository, shoppingListRepository, manualShoppingItemRepository) as T
            modelClass.isAssignableFrom(PricesViewModel::class.java) -> PricesViewModel(fruitPriceRepository) as T
            modelClass.isAssignableFrom(StockViewModel::class.java) -> StockViewModel(stockItemRepository) as T
            modelClass.isAssignableFrom(WineStockViewModel::class.java) -> WineStockViewModel(wineStockItemRepository) as T
            modelClass.isAssignableFrom(WeinprobeViewModel::class.java) -> WeinprobeViewModel(weinprobeRepository, wineStockItemRepository) as T
            modelClass.isAssignableFrom(WineOrderViewModel::class.java) -> WineOrderViewModel(wineOrderRepository) as T
            else -> throw IllegalArgumentException("Unbekannte ViewModel-Klasse: ${modelClass.name}")
        }
    }

    companion object {
        fun from(application: FruchtweinApplication) = AppViewModelFactory(
            application.repository,
            application.schmalzRepository,
            application.settingsRepository,
            application.shoppingListRepository,
            application.fruitPriceRepository,
            application.infoEntryRepository,
            application.recipeSessionRepository,
            application.manualShoppingItemRepository,
            application.stockItemRepository,
            application.themeRepository,
            application.wineStockItemRepository,
            application.weinprobeRepository,
            application.wineOrderRepository
        )
    }
}
