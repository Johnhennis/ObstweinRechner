package app.johnhennis.obstweinrechner.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.johnhennis.obstweinrechner.ui.AppViewModelFactory
import app.johnhennis.obstweinrechner.ui.calculator.CalculatorScreen
import app.johnhennis.obstweinrechner.ui.common.InAppUpdateChecker
import app.johnhennis.obstweinrechner.ui.prices.PricesScreen
import app.johnhennis.obstweinrechner.ui.prices.PricesTrashScreen
import app.johnhennis.obstweinrechner.ui.recipes.RecipeListScreen
import app.johnhennis.obstweinrechner.ui.recipes.RecipeTrashScreen
import app.johnhennis.obstweinrechner.ui.schmalz.SchmalzScreen
import app.johnhennis.obstweinrechner.ui.settings.SettingsScreen
import app.johnhennis.obstweinrechner.ui.shopping.ShoppingListScreen
import app.johnhennis.obstweinrechner.ui.stock.StockScreen
import app.johnhennis.obstweinrechner.ui.stock.StockTrashScreen
import app.johnhennis.obstweinrechner.ui.winestock.WineStockScreen
import app.johnhennis.obstweinrechner.ui.winestock.WineStockTrashScreen
import kotlinx.coroutines.launch

private object Routes {
    const val WEIN = "wein"
    const val WEIN_RECIPES = "wein_recipes"
    const val WEIN_TRASH = "wein_trash"
    const val SCHMALZ = "schmalz"
    const val SHOPPING = "shopping"
    const val PRICES = "prices"
    const val PRICES_TRASH = "prices_trash"
    const val STOCK = "stock"
    const val STOCK_TRASH = "stock_trash"
    const val WINE_STOCK = "wine_stock"
    const val WINE_STOCK_TRASH = "wine_stock_trash"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(factory: AppViewModelFactory) {
    InAppUpdateChecker()

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    fun navigateToTopLevel(route: String) {
        scope.launch { drawerState.close() }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text("Menü", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))
                NavigationDrawerItem(
                    label = { Text("Wein-Rechner") },
                    icon = { Icon(Icons.Filled.WineBar, contentDescription = null) },
                    selected = false,
                    onClick = { navigateToTopLevel(Routes.WEIN) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Schmalz-Rechner") },
                    icon = { Icon(Icons.Filled.Kitchen, contentDescription = null) },
                    selected = false,
                    onClick = { navigateToTopLevel(Routes.SCHMALZ) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Weinbestand") },
                    icon = { Icon(Icons.Filled.Inventory2, contentDescription = null) },
                    selected = false,
                    onClick = { navigateToTopLevel(Routes.WINE_STOCK) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Bestandsliste") },
                    icon = { Icon(Icons.Filled.Inventory, contentDescription = null) },
                    selected = false,
                    onClick = { navigateToTopLevel(Routes.STOCK) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Einkaufsliste") },
                    icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
                    selected = false,
                    onClick = { navigateToTopLevel(Routes.SHOPPING) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Preise Obst") },
                    icon = { Icon(Icons.Filled.AttachMoney, contentDescription = null) },
                    selected = false,
                    onClick = { navigateToTopLevel(Routes.PRICES) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Einstellungen") },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    selected = false,
                    onClick = { navigateToTopLevel(Routes.SETTINGS) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        NavHost(navController = navController, startDestination = Routes.WEIN) {
            composable(Routes.WEIN) {
                CalculatorScreen(
                    factory = factory,
                    onOpenRecipes = { navController.navigate(Routes.WEIN_RECIPES) },
                    onOpenMenu = { scope.launch { drawerState.open() } }
                )
            }
            composable(Routes.WEIN_RECIPES) {
                RecipeListScreen(
                    factory = factory,
                    onBack = { navController.popBackStack() },
                    onOpenTrash = { navController.navigate(Routes.WEIN_TRASH) }
                )
            }
            composable(Routes.WEIN_TRASH) {
                RecipeTrashScreen(factory = factory, onBack = { navController.popBackStack() })
            }
            composable(Routes.SCHMALZ) {
                SchmalzScreen(factory = factory, onOpenMenu = { scope.launch { drawerState.open() } })
            }
            composable(Routes.SHOPPING) {
                ShoppingListScreen(factory = factory, onOpenMenu = { scope.launch { drawerState.open() } })
            }
            composable(Routes.PRICES) {
                PricesScreen(
                    factory = factory,
                    onOpenMenu = { scope.launch { drawerState.open() } },
                    onOpenTrash = { navController.navigate(Routes.PRICES_TRASH) }
                )
            }
            composable(Routes.PRICES_TRASH) {
                PricesTrashScreen(factory = factory, onBack = { navController.popBackStack() })
            }
            composable(Routes.STOCK) {
                StockScreen(
                    factory = factory,
                    onOpenMenu = { scope.launch { drawerState.open() } },
                    onOpenTrash = { navController.navigate(Routes.STOCK_TRASH) }
                )
            }
            composable(Routes.STOCK_TRASH) {
                StockTrashScreen(factory = factory, onBack = { navController.popBackStack() })
            }
            composable(Routes.WINE_STOCK) {
                WineStockScreen(
                    factory = factory,
                    onOpenMenu = { scope.launch { drawerState.open() } },
                    onOpenTrash = { navController.navigate(Routes.WINE_STOCK_TRASH) }
                )
            }
            composable(Routes.WINE_STOCK_TRASH) {
                WineStockTrashScreen(factory = factory, onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(factory = factory, onOpenMenu = { scope.launch { drawerState.open() } })
            }
        }
    }
}
