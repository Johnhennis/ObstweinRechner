package com.example.fruchtweinrechner

import android.app.Application
import com.example.fruchtweinrechner.data.FruitRecipeRepository
import com.example.fruchtweinrechner.data.InventoryRepository
import com.example.fruchtweinrechner.data.SchmalzRecipeRepository
import com.example.fruchtweinrechner.data.SettingsRepository
import com.example.fruchtweinrechner.data.ShoppingListRepository
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
    val inventoryRepository: InventoryRepository by lazy { InventoryRepository(FirebaseFirestore.getInstance()) }
    val shoppingListRepository: ShoppingListRepository by lazy { ShoppingListRepository(FirebaseFirestore.getInstance()) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            if (FirebaseAuth.getInstance().currentUser == null) {
                FirebaseAuth.getInstance().signInAnonymously().await()
            }
            repository.seedIfEmpty()
            schmalzRepository.seedIfEmpty()
        }
    }
}
