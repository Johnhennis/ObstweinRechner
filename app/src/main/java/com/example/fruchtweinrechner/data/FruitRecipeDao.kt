package com.example.fruchtweinrechner.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FruitRecipeDao {

    @Query("SELECT * FROM fruit_recipes ORDER BY name ASC")
    fun getAll(): Flow<List<FruitRecipe>>

    @Query("SELECT * FROM fruit_recipes WHERE id = :id")
    suspend fun getById(id: Long): FruitRecipe?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: FruitRecipe): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(recipes: List<FruitRecipe>)

    @Update
    suspend fun update(recipe: FruitRecipe)

    @Delete
    suspend fun delete(recipe: FruitRecipe)

    @Query("SELECT COUNT(*) FROM fruit_recipes")
    suspend fun count(): Int
}
