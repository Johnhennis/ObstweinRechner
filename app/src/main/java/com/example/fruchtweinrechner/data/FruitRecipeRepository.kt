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
                trySend(emptyList())
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

    // Alle Werte pro 10 Liter fertigem Wein
    private fun defaultRecipes(): List<FruitRecipe> = listOf(
        FruitRecipe(name = "Äpfel reif", fruchtKg = 14.0, saftLiter = 10.0, hefeSorte = "Steinberg"),
        FruitRecipe(name = "Aprikose", fruchtKg = 5.0, wasserLiter = 5.5, zuckerKg = 2.5, milchsaeureGramm = 30.0, antigelKleinMl = 10.0, antigelGrossMl = 1.5, hefeSorte = "Bordeaux"),
        FruitRecipe(name = "Aronia", fruchtKg = 9.0, wasserLiter = 6.0, zuckerKg = 2.0, antigelKleinMl = 50.0, antigelGrossMl = 3.0, hefeSorte = "Bordeaux"),
        FruitRecipe(name = "Birnen", fruchtKg = 14.0, saftLiter = 10.0, milchsaeureGramm = 20.0, hefeSorte = "Steinberg"),
        FruitRecipe(name = "Bananen", fruchtKg = 4.0, saftLiter = 1.0, wasserLiter = 7.5, zuckerKg = 2.5, milchsaeureGramm = 60.0, antigelKleinMl = 10.0, antigelGrossMl = 1.5, hefeSorte = "Portwein"),
        FruitRecipe(name = "Brombeeren", fruchtKg = 6.0, saftLiter = 4.5, wasserLiter = 4.0, zuckerKg = 2.5, antigelKleinMl = 15.0, antigelGrossMl = 1.5, hefeSorte = "Bordeaux"),
        FruitRecipe(name = "Erdbeeren", fruchtKg = 6.0, saftLiter = 5.0, wasserLiter = 3.5, zuckerKg = 2.5, milchsaeureGramm = 30.0, antigelKleinMl = 10.0, antigelGrossMl = 1.5, hefeSorte = "Portwein"),
        FruitRecipe(name = "Hagebutten", fruchtKg = 3.0, wasserLiter = 8.5, zuckerKg = 2.8, milchsaeureGramm = 40.0, antigelKleinMl = 10.0, antigelGrossMl = 1.5, hefeSorte = "Portwein"),
        FruitRecipe(name = "Heidelbeeren", fruchtKg = 6.0, saftLiter = 4.5, wasserLiter = 4.0, zuckerKg = 2.5, milchsaeureGramm = 20.0, antigelKleinMl = 10.0, antigelGrossMl = 1.5, hefeSorte = "Bordeaux"),
        FruitRecipe(name = "Himbeeren", fruchtKg = 4.0, saftLiter = 3.5, wasserLiter = 5.0, zuckerKg = 2.5, milchsaeureGramm = 20.0, antigelKleinMl = 10.0, antigelGrossMl = 1.5, hefeSorte = "Portwein"),
        FruitRecipe(name = "Holunderbeeren", fruchtKg = 4.0, saftLiter = 3.0, wasserLiter = 5.5, zuckerKg = 2.5, milchsaeureGramm = 40.0, antigelKleinMl = 10.0, antigelGrossMl = 1.5, hefeSorte = "Portwein"),
        FruitRecipe(name = "Holunderblüte", fruchtKg = 0.5, wasserLiter = 8.5, zuckerKg = 2.5, milchsaeureGramm = 50.0, hefeSorte = "Portwein"),
        FruitRecipe(
            name = "Honig",
            fruchtKg = 3.0,
            wasserLiter = 7.0,
            milchsaeureGramm = 40.0,
            hefeSorte = "Portwein",
            zusatzZutaten = listOf(ExtraIngredient("A-Saft (L, statt Zucker)", 1.0))
        ),
        FruitRecipe(name = "Johannisbeeren r/w", fruchtKg = 4.0, saftLiter = 3.0, wasserLiter = 5.5, zuckerKg = 2.5, antigelKleinMl = 10.0, antigelGrossMl = 1.5, hefeSorte = "Portwein"),
        FruitRecipe(name = "Johannisbeeren sw", fruchtKg = 4.0, saftLiter = 3.0, wasserLiter = 5.5, zuckerKg = 2.5, antigelKleinMl = 15.0, antigelGrossMl = 1.5, hefeSorte = "Portwein"),
        FruitRecipe(name = "Mirabellen", fruchtKg = 6.0, saftLiter = 4.5, wasserLiter = 4.5, zuckerKg = 2.0, milchsaeureGramm = 50.0, antigelKleinMl = 15.0, antigelGrossMl = 1.5, hefeSorte = "Portwein"),
        FruitRecipe(name = "Möhren", fruchtKg = 6.0, wasserLiter = 7.0, zuckerKg = 2.0, milchsaeureGramm = 10.0, hefeSorte = "Portwein"),
        FruitRecipe(name = "Pfirsich", fruchtKg = 10.0, saftLiter = 6.0, wasserLiter = 2.5, zuckerKg = 2.5, milchsaeureGramm = 30.0, antigelKleinMl = 20.0, antigelGrossMl = 3.0, hefeSorte = "Portwein"),
        FruitRecipe(name = "Pflaumen", fruchtKg = 6.0, saftLiter = 4.0, wasserLiter = 5.0, zuckerKg = 2.0, milchsaeureGramm = 40.0, antigelKleinMl = 15.0, antigelGrossMl = 1.5, hefeSorte = "Portwein"),
        FruitRecipe(name = "Quitten", fruchtKg = 7.0, wasserLiter = 3.5, zuckerKg = 2.5, milchsaeureGramm = 25.0, antigelKleinMl = 10.0, antigelGrossMl = 1.5, hefeSorte = "Portwein"),
        FruitRecipe(
            name = "Rhabarber",
            fruchtKg = 7.0,
            saftLiter = 4.5,
            wasserLiter = 3.5,
            zuckerKg = 3.0,
            milchsaeureGramm = 40.0,
            hefeSorte = "Portwein",
            zusatzZutaten = listOf(ExtraIngredient("Weinkalk (nach Gärung)", 25.0))
        ),
        FruitRecipe(name = "Sauerkirschen", fruchtKg = 7.0, saftLiter = 5.0, wasserLiter = 3.5, zuckerKg = 2.5, antigelKleinMl = 10.0, antigelGrossMl = 1.5, hefeSorte = "Portwein"),
        FruitRecipe(name = "Süßkirschen", fruchtKg = 11.0, saftLiter = 7.0, wasserLiter = 2.0, zuckerKg = 2.0, milchsaeureGramm = 30.0, antigelKleinMl = 20.0, antigelGrossMl = 3.0, hefeSorte = "Portwein"),
        FruitRecipe(name = "Schlehen", fruchtKg = 6.0, saftLiter = 2.5, wasserLiter = 6.0, zuckerKg = 2.5, antigelKleinMl = 20.0, antigelGrossMl = 3.0, hefeSorte = "Portwein"),
        FruitRecipe(name = "Stachelbeeren", fruchtKg = 6.0, saftLiter = 4.5, wasserLiter = 4.0, zuckerKg = 2.5, antigelKleinMl = 20.0, antigelGrossMl = 3.0, hefeSorte = "Portwein"),
        FruitRecipe(name = "Trauben ws", fruchtKg = 14.0, saftLiter = 10.0, antigelKleinMl = 20.0, antigelGrossMl = 3.0, hefeSorte = "Steinberg"),
        FruitRecipe(name = "Trauben rot", fruchtKg = 14.0, saftLiter = 10.0, antigelKleinMl = 20.0, antigelGrossMl = 3.0, hefeSorte = "Bordeaux")
    )
}
