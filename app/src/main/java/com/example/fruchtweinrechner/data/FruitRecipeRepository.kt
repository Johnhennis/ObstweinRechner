package com.example.fruchtweinrechner.data

import kotlinx.coroutines.flow.Flow

class FruitRecipeRepository(private val dao: FruitRecipeDao) {

    val allRecipes: Flow<List<FruitRecipe>> = dao.getAll()

    suspend fun getById(id: Long): FruitRecipe? = dao.getById(id)

    suspend fun insert(recipe: FruitRecipe): Long = dao.insert(recipe)

    suspend fun update(recipe: FruitRecipe) = dao.update(recipe)

    suspend fun delete(recipe: FruitRecipe) = dao.delete(recipe)
}
