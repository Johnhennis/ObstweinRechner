package com.example.fruchtweinrechner.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FruitRecipeRepository(private val firestore: FirebaseFirestore) {

    private val recipesCollection = firestore.collection("recipes")

    val allRecipes: Flow<List<FruitRecipe>> = callbackFlow {
        val listener = recipesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val recipes = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(FruitRecipe::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(recipes.sortedBy { it.name })
        }
        awaitClose { listener.remove() }
    }

    suspend fun getById(id: String): FruitRecipe? {
        val doc = recipesCollection.document(id).get().await()
        return doc.toObject(FruitRecipe::class.java)?.copy(id = doc.id)
    }

    suspend fun insert(recipe: FruitRecipe) {
        recipesCollection.add(recipe.copy(id = "")).await()
    }

    suspend fun update(recipe: FruitRecipe) {
        recipesCollection.document(recipe.id).set(recipe).await()
    }

    suspend fun delete(recipe: FruitRecipe) {
        recipesCollection.document(recipe.id).delete().await()
    }

    suspend fun seedIfEmpty() {
        val snapshot = recipesCollection.limit(1).get().await()
        if (snapshot.isEmpty) {
            defaultRecipes().forEach { recipesCollection.add(it).await() }
        }
    }

    // Werte pro 10 Liter fertigem Wein
    private fun defaultRecipes(): List<FruitRecipe> = listOf(
        FruitRecipe(
            name = "Äpfel reif",
            fruchtKg = 14.0,
            saftLiter = 10.0,
            hefeSorte = "Steinberg"
        ),
        FruitRecipe(
            name = "Sauerkirschen",
            fruchtKg = 7.0,
            saftLiter = 5.0,
            wasserLiter = 3.5,
            zuckerKg = 2.5,
            antigelKleinMl = 10.0,
            antigelGrossMl = 1.5,
            hefeSorte = "Portwein"
        ),
        FruitRecipe(
            name = "Johannisbeeren sw",
            fruchtKg = 4.0,
            saftLiter = 3.0,
            wasserLiter = 5.5,
            zuckerKg = 2.5,
            antigelKleinMl = 15.0,
            antigelGrossMl = 1.5,
            hefeSorte = "Portwein"
        )
    )
}
