package com.example.fruchtweinrechner.data

data class ExtraIngredient(
    val name: String = "",
    val menge: Double = 0.0
)

// Alle Mengen beziehen sich auf ein Basis-Rezept für 10 Liter fertigen Wein.
data class FruitRecipe(
    val id: String = "",
    val name: String = "",
    val fruchtKg: Double = 0.0,
    val saftLiter: Double = 0.0,
    val wasserLiter: Double = 0.0,
    val zuckerKg: Double = 0.0,
    val milchsaeureGramm: Double = 0.0,
    val antigelKleinMl: Double = 0.0,
    val antigelGrossMl: Double = 0.0,
    val hefeSorte: String = "",
    val zusatzZutaten: List<ExtraIngredient> = emptyList()
)
