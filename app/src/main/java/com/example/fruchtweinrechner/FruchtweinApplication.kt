package com.example.fruchtweinrechner

import android.app.Application
import com.example.fruchtweinrechner.data.FruitRecipeRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FruchtweinApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob())

    val repository: FruitRecipeRepository by lazy {
        FruitRecipeRepository(FirebaseFirestore.getInstance())
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            if (FirebaseAuth.getInstance().currentUser == null) {
                FirebaseAuth.getInstance().signInAnonymously().await()
            }
            repository.seedIfEmpty()
        }
    }
}
