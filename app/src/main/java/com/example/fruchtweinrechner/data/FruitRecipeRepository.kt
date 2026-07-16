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

    private fun defaultRecipes(): List<FruitRecipe> = listOf(
        FruitRecipe(name = "Apfel", saftAusbeute = 0.70, saftAnteilImWein = 0.80, zuckerProLiter = 150.0, hefeProLiter = 0.4, naehrsalzProLiter = 0.4),
        FruitRecipe(name = "Kirsche (Sauerkirsch)", saftAusbeute = 0.60, saftAnteilImWein = 0.70, zuckerProLiter = 200.0, hefeProLiter = 0.4, naehrsalzProLiter = 0.4),
        FruitRecipe(name = "Johannisbeere", saftAusbeute = 0.55, saftAnteilImWein = 0.50, zuckerProLiter = 220.0, hefeProLiter = 0.4, naehrsalzProLiter = 0.5)
    )
}
