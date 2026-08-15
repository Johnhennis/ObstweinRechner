package app.johnhennis.obstweinrechner

import android.app.Application
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FruchtweinApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob())

    val repository: FruitRecipeRepository by lazy { FruitRecipeRepository(FirebaseFirestore.getInstance()) }
    val schmalzRepository: SchmalzRecipeRepository by lazy { SchmalzRecipeRepository(FirebaseFirestore.getInstance()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val shoppingListRepository: ShoppingListRepository by lazy { ShoppingListRepository(FirebaseFirestore.getInstance()) }
    val fruitPriceRepository: FruitPriceRepository by lazy { FruitPriceRepository(FirebaseFirestore.getInstance()) }
    val infoEntryRepository: InfoEntryRepository by lazy { InfoEntryRepository(FirebaseFirestore.getInstance()) }
    val recipeSessionRepository: RecipeSessionRepository by lazy { RecipeSessionRepository(FirebaseFirestore.getInstance()) }
    val manualShoppingItemRepository: ManualShoppingItemRepository by lazy { ManualShoppingItemRepository(FirebaseFirestore.getInstance()) }
    val stockItemRepository: StockItemRepository by lazy { StockItemRepository(FirebaseFirestore.getInstance()) }
    val themeRepository: ThemeRepository by lazy { ThemeRepository(this) }
    val wineStockItemRepository: WineStockItemRepository by lazy { WineStockItemRepository(FirebaseFirestore.getInstance()) }
    val weinprobeRepository: WeinprobeRepository by lazy { WeinprobeRepository(FirebaseFirestore.getInstance()) }
    val wineOrderRepository: WineOrderRepository by lazy { WineOrderRepository(FirebaseFirestore.getInstance()) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            if (FirebaseAuth.getInstance().currentUser == null) {
                FirebaseAuth.getInstance().signInAnonymously().await()
            }
            repository.seedIfEmpty()
            schmalzRepository.seedIfEmpty()
            fruitPriceRepository.seedIfEmpty()
            stockItemRepository.seedIfEmpty()

            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            if (!prefs.getBoolean("stockBedarfMigrated_v1", false)) {
                stockItemRepository.migrateEinkaufToBedarf()
                prefs.edit().putBoolean("stockBedarfMigrated_v1", true).apply()
            }
            if (!prefs.getBoolean("stockDedup_v1", false)) {
                stockItemRepository.deduplicateItems()
                prefs.edit().putBoolean("stockDedup_v1", true).apply()
            }
            if (!prefs.getBoolean("priceDedup_v1", false)) {
                fruitPriceRepository.deduplicatePrices()
                prefs.edit().putBoolean("priceDedup_v1", true).apply()
            }
            if (!prefs.getBoolean("infoDedup_v1", false)) {
                infoEntryRepository.deduplicateEntries()
                prefs.edit().putBoolean("infoDedup_v1", true).apply()
            }
            if (!prefs.getBoolean("recipeDedup_v1", false)) {
                repository.deduplicateRecipes()
                schmalzRepository.deduplicate()
                prefs.edit().putBoolean("recipeDedup_v1", true).apply()
            }
            if (!prefs.getBoolean("wineOrderMigrated_v1", false)) {
                wineOrderRepository.migrateToPositionen()
                prefs.edit().putBoolean("wineOrderMigrated_v1", true).apply()
            }
        }
    }
}
