package com.example.fruchtweinrechner.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fruchtweinrechner.ui.AppViewModelFactory
import com.example.fruchtweinrechner.ui.calculator.CalculatorScreen
import com.example.fruchtweinrechner.ui.inventory.InventoryScreen
import com.example.fruchtweinrechner.ui.inventory.InventoryTrashScreen
import com.example.fruchtweinrechner.ui.recipes.RecipeListScreen
import com.example.fruchtweinrechner.ui.recipes.RecipeTrashScreen
import com.example.fruchtweinrechner.ui.schmalz.SchmalzScreen
import com.example.fruchtweinrechner.ui.settings.SettingsScreen
import com.example.fruchtweinrechner.ui.shopping.ShoppingListScreen
import kotlinx.coroutines.launch

private object Routes {
    const val WEIN = "wein"
    const val WEIN_RECIPES = "wein_recipes"
    const val WEIN_TRASH = "wein_trash"
    const val SCHMALZ = "schmalz"
    const val INVENTORY = "inventory"
    const val INVENTORY_TRASH = "inventory_trash"
    const val SHOPPING = "shopping"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(factory: AppViewModelFactory) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
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
                    onClick = { scope.launch { drawerState.close() }; navController.navigate(Routes.WEIN) { launchSingleTop = true } },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Schmalz-Rechner") },
                    icon = { Icon(Icons.Filled.Kitchen, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; navController.navigate(Routes.SCHMALZ) { launchSingleTop = true } },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Bestandsaufnahme") },
                    icon = { Icon(Icons.Filled.Inventory2, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; navController.navigate(Routes.INVENTORY) { launchSingleTop = true } },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Einkaufsliste") },
                    icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; navController.navigate(Routes.SHOPPING) { launchSingleTop = true } },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Einstellungen") },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
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
            composable(Routes.INVENTORY) {
                InventoryScreen(
                    factory = factory,
                    onOpenMenu = { scope.launch { drawerState.open() } },
                    onOpenTrash = { navController.navigate(Routes.INVENTORY_TRASH) }
                )
            }
            composable(Routes.INVENTORY_TRASH) {
                InventoryTrashScreen(factory = factory, onBack = { navController.popBackStack() })
            }
            composable(Routes.SHOPPING) {
                ShoppingListScreen(factory = factory, onOpenMenu = { scope.launch { drawerState.open() } })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(factory = factory, onOpenMenu = { scope.launch { drawerState.open() } })
            }
        }
    }
}
