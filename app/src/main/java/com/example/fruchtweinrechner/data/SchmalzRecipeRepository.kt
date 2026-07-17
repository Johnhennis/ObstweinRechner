package com.example.fruchtweinrechner.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SchmalzRecipeRepository(private val firestore: FirebaseFirestore) {

    private val collection = firestore.collection("schmalzRecipes")

    val recipe: Flow<SchmalzRecipe?> = callbackFlow {
        val listener = collection.limit(1).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            val doc = snapshot?.documents?.firstOrNull()
            trySend(doc?.toObject(SchmalzRecipe::class.java)?.copy(id = doc.id))
        }
        awaitClose { listener.remove() }
    }

    suspend fun save(recipe: SchmalzRecipe) {
        if (recipe.id.isEmpty()) {
            collection.add(recipe.copy(id = "")).await()
        } else {
            collection.document(recipe.id).set(recipe).await()
        }
    }

    suspend fun seedIfEmpty() {
        val snapshot = collection.limit(1).get().await()
        if (snapshot.isEmpty) {
            collection.add(defaultRecipe()).await()
        }
    }

    // Originalrezept für 24 Liter
    private fun defaultRecipe() = SchmalzRecipe(
        schmalzStueck = 24.0,
        ruckenfettKg = 6.0,
        aepfelStueck = 8.0,
        zwiebelnGramm = 1000.0,
        salzGramm = 36.0,
        majoranGramm = 36.0,
        thymianGramm = 36.0
    )
}
