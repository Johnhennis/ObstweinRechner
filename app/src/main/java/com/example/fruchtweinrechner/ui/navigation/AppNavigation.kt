package com.example.fruchtweinrechner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fruchtweinrechner.ui.AppViewModelFactory
import com.example.fruchtweinrechner.ui.calculator.CalculatorScreen
import com.example.fruchtweinrechner.ui.recipes.RecipeListScreen

private object Routes {
    const val CALCULATOR = "calculator"
    const val RECIPES = "recipes"
}

@Composable
fun AppNavigation(factory: AppViewModelFactory) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.CALCULATOR) {
        composable(Routes.CALCULATOR) {
            CalculatorScreen(
                factory = factory,
                onOpenRecipes = { navController.navigate(Routes.RECIPES) }
            )
        }
        composable(Routes.RECIPES) {
            RecipeListScreen(
                factory = factory,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
