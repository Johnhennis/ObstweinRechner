package com.example.fruchtweinrechner.data

private const val BASIS_LITER = 24.0

data class SchmalzCalculationResult(
    val zielLiter: Double,
    val schmalzStueck: Double,
    val ruckenfettKg: Double,
    val aepfelStueck: Double,
    val zwiebelnGramm: Double,
    val salzGramm: Double,
    val majoranGramm: Double,
    val thymianGramm: Double,
    val zusatzMengen: List<Pair<String, Double>>
)

fun SchmalzRecipe.calculate(zielLiter: Double): SchmalzCalculationResult {
    val faktor = zielLiter / BASIS_LITER
    return SchmalzCalculationResult(
        zielLiter = zielLiter,
        schmalzStueck = schmalzStueck * faktor,
        ruckenfettKg = ruckenfettKg * faktor,
        aepfelStueck = aepfelStueck * faktor,
        zwiebelnGramm = zwiebelnGramm * faktor,
        salzGramm = salzGramm * faktor,
        majoranGramm = majoranGramm * faktor,
        thymianGramm = thymianGramm * faktor,
        zusatzMengen = zusatzZutaten.map { it.name to (it.menge * faktor) }
    )
}
