package com.example.fruchtweinrechner

import android.app.Application
import com.example.fruchtweinrechner.data.AppDatabase
import com.example.fruchtweinrechner.data.FruitRecipeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class FruchtweinApplication : Application() {

    // Anwendungsweiter CoroutineScope, u.a. für das Datenbank-Seeding
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository: FruitRecipeRepository by lazy { FruitRecipeRepository(database.fruitRecipeDao()) }
}
