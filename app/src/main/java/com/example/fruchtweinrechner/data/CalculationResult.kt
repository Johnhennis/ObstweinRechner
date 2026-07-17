package com.example.fruchtweinrechner.data

private const val BASIS_LITER = 10.0

data class CalculationResult(
    val zielLiter: Double,
    val fruchtKg: Double,
    val saftLiter: Double,
    val wasserLiter: Double,
    val zuckerKg: Double,
    val milchsaeureGramm: Double,
    val antigelKleinMl: Double,
    val antigelGrossMl: Double,
    val hefeSorte: String,
    val zusatzMengen: List<Pair<String, Double>>
)

fun FruitRecipe.calculateFromZielLiter(zielLiter: Double): CalculationResult {
    val faktor = zielLiter / BASIS_LITER
    return buildResult(faktor)
}

fun FruitRecipe.calculateFromFruchtKg(fruchtKgInput: Double): CalculationResult {
    val faktor = if (fruchtKg > 0) fruchtKgInput / fruchtKg else 0.0
    return buildResult(faktor)
}

private fun FruitRecipe.buildResult(faktor: Double): CalculationResult {
    return CalculationResult(
        zielLiter = BASIS_LITER * faktor,
        fruchtKg = fruchtKg * faktor,
        saftLiter = saftLiter * faktor,
        wasserLiter = wasserLiter * faktor,
        zuckerKg = zuckerKg * faktor,
        milchsaeureGramm = milchsaeureGramm * faktor,
        antigelKleinMl = antigelKleinMl * faktor,
        antigelGrossMl = antigelGrossMl * faktor,
        hefeSorte = hefeSorte,
        zusatzMengen = zusatzZutaten.map { it.name to (it.menge * faktor) }
    )
}
