package app.johnhennis.obstweinrechner.data

// Merkt sich pro Rezept die zuletzt benutzte Eingabe (Modus, Liter/kg-Text,
// Notiz), damit sie nach Sortenwechsel oder App-Neustart wieder da ist.
// Dokument-ID in Firestore = recipeId.
data class RecipeSession(
    val recipeId: String = "",
    val mode: String = "LITER",
    val literText: String = "10",
    val fruchtKgText: String = "10",
    val notiz: String = ""
)
