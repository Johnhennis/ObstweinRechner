package com.example.fruchtweinrechner.data

// Alle Mengen beziehen sich auf das Originalrezept für 24 Liter Schmalz.
data class SchmalzRecipe(
    val id: String = "",
    val ruckenfettKg: Double = 0.0,
    val aepfelStueck: Double = 0.0,
    val zwiebelnGramm: Double = 0.0,
    val salzGramm: Double = 0.0,
    val majoranGramm: Double = 0.0,
    val thymianGramm: Double = 0.0
)
