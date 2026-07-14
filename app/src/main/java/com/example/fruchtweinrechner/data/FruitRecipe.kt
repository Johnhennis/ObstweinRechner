package com.example.fruchtweinrechner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Repräsentiert ein editierbares Frucht-Rezept / Verhältnis-Set.
 *
 * saftAusbeute:       Liter Saft pro kg Frucht (z.B. 0.70 => 1 kg Frucht -> 0.7 L Saft)
 * saftAnteilImWein:   Anteil des Safts am fertigen Wein, Rest = Wasser (z.B. 0.80 => 80 %)
 * zuckerProLiter:     Gramm Zucker pro Liter fertigem Wein
 * hefeProLiter:        Gramm Hefe pro Liter fertigem Wein
 * naehrsalzProLiter:  Gramm Hefenährsalz pro Liter fertigem Wein
 */
@Entity(tableName = "fruit_recipes")
data class FruitRecipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val saftAusbeute: Double,
    val saftAnteilImWein: Double,
    val zuckerProLiter: Double,
    val hefeProLiter: Double,
    val naehrsalzProLiter: Double
)
