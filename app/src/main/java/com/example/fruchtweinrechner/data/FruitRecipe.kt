package com.example.fruchtweinrechner.data

data class ExtraIngredient(
    val name: String = "",
    val mengeProLiter: Double = 0.0
)

data class FruitRecipe(
    val id: String = "",
    val name: String = "",
    val saftAusbeute: Double = 0.0,
    val saftAnteilImWein: Double = 0.0,
    val zuckerProLiter: Double = 0.0,
    val milchsaeureProLiter: Double = 0.0,
    val antigelKleinProLiter: Double = 0.0,
    val antigelGrossProLiter: Double = 0.0,
    val hefeProLiter: Double = 0.0,
    val hefeSorte: String = "",
    val naehrsalzProLiter: Double = 0.0,
    val zusatzZutaten: List<ExtraIngredient> = emptyList()
)
