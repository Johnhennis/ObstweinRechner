package com.example.fruchtweinrechner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [FruitRecipe::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun fruitRecipeDao(): FruitRecipeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fruchtwein_database"
                )
                    .addCallback(SeedCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Befüllt die Datenbank beim allerersten Erstellen mit drei Standard-Rezepten.
     */
    private class SeedCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.fruitRecipeDao()
                    dao.insertAll(defaultRecipes())
                }
            }
        }

        private fun defaultRecipes(): List<FruitRecipe> = listOf(
            FruitRecipe(
                name = "Apfel",
                saftAusbeute = 0.70,
                saftAnteilImWein = 0.80,
                zuckerProLiter = 150.0,
                hefeProLiter = 0.4,
                naehrsalzProLiter = 0.4
            ),
            FruitRecipe(
                name = "Kirsche (Sauerkirsch)",
                saftAusbeute = 0.60,
                saftAnteilImWein = 0.70,
                zuckerProLiter = 200.0,
                hefeProLiter = 0.4,
                naehrsalzProLiter = 0.4
            ),
            FruitRecipe(
                name = "Johannisbeere",
                saftAusbeute = 0.55,
                saftAnteilImWein = 0.50,
                zuckerProLiter = 220.0,
                hefeProLiter = 0.4,
                naehrsalzProLiter = 0.5
            )
        )
    }
}
