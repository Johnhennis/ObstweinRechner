package com.example.fruchtweinrechner.data

/**
 * Ergebnis der Rezeptberechnung für eine bestimmte Ziel-Literanzahl.
 */
data class CalculationResult(
    val saftLiter: Double,
    val fruchtKg: Double,
    val wasserLiter: Double,
    val zuckerKg: Double,
    val hefeGramm: Double,
    val naehrsalzGramm: Double
)

/**
 * Reine Berechnungsfunktion, unabhängig von UI/ViewModel testbar.
 */
fun FruitRecipe.calculate(zielLiter: Double): CalculationResult {
    val saftLiter = zielLiter * saftAnteilImWein
    val fruchtKg = if (saftAusbeute > 0) saftLiter / saftAusbeute else 0.0
    val wasserLiter = zielLiter * (1 - saftAnteilImWein)
    val zuckerKg = (zielLiter * zuckerProLiter) / 1000.0
    val hefeGramm = zielLiter * hefeProLiter
    val naehrsalzGramm = zielLiter * naehrsalzProLiter

    return CalculationResult(
        saftLiter = saftLiter,
        fruchtKg = fruchtKg,
        wasserLiter = wasserLiter,
        zuckerKg = zuckerKg,
        hefeGramm = hefeGramm,
        naehrsalzGramm = naehrsalzGramm
    )
}
