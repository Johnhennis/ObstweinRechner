package com.example.fruchtweinrechner.data

data class CalculationResult(
    val zielLiter: Double,
    val saftLiter: Double,
    val fruchtKg: Double,
    val wasserLiter: Double,
    val zuckerKg: Double,
    val milchsaeureGramm: Double,
    val antigelKleinGramm: Double,
    val antigelGrossGramm: Double,
    val hefeGramm: Double,
    val hefeSorte: String,
    val naehrsalzGramm: Double,
    val zusatzMengen: List<Pair<String, Double>>
)

fun FruitRecipe.calculateFromZielLiter(zielLiter: Double): CalculationResult {
    val saftLiter = zielLiter * saftAnteilImWein
    val fruchtKg = if (saftAusbeute > 0) saftLiter / saftAusbeute else 0.0
    val wasserLiter = zielLiter * (1 - saftAnteilImWein)
    return buildResult(zielLiter, fruchtKg, saftLiter, wasserLiter)
}

fun FruitRecipe.calculateFromFruchtKg(fruchtKg: Double): CalculationResult {
    val saftLiter = fruchtKg * saftAusbeute
    val zielLiter = if (saftAnteilImWein > 0) saftLiter / saftAnteilImWein else 0.0
    val wasserLiter = zielLiter - saftLiter
    return buildResult(zielLiter, fruchtKg, saftLiter, wasserLiter)
}

private fun FruitRecipe.buildResult(
    zielLiter: Double,
    fruchtKg: Double,
    saftLiter: Double,
    wasserLiter: Double
): CalculationResult {
    return CalculationResult(
        zielLiter = zielLiter,
        saftLiter = saftLiter,
        fruchtKg = fruchtKg,
        wasserLiter = wasserLiter,
        zuckerKg = (zielLiter * zuckerProLiter) / 1000.0,
        milchsaeureGramm = zielLiter * milchsaeureProLiter,
        antigelKleinGramm = zielLiter * antigelKleinProLiter,
        antigelGrossGramm = zielLiter * antigelGrossProLiter,
        hefeGramm = zielLiter * hefeProLiter,
        hefeSorte = hefeSorte,
        naehrsalzGramm = zielLiter * naehrsalzProLiter,
        zusatzMengen = zusatzZutaten.map { it.name to (zielLiter * it.mengeProLiter) }
    )
}
