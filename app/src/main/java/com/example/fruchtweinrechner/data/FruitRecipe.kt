package com.example.fruchtweinrechner.data

data class FruitRecipe(
    val id: String = "",
    val name: String = "",
    val saftAusbeute: Double = 0.0,
    val saftAnteilImWein: Double = 0.0,
    val zuckerProLiter: Double = 0.0,
    val hefeProLiter: Double = 0.0,
    val naehrsalzProLiter: Double = 0.0
)
