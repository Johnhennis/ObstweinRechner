package app.johnhennis.obstweinrechner.data

// datum als ISO-Text (JJJJ-MM-TT) gespeichert, damit die Gruppierung wie
// bei den anderen Reitern zuverlässig neueste-zuerst sortiert werden kann
// (deutsche TT.MM.JJJJ-Schreibweise sortiert als reiner Text nicht korrekt).
data class WeinprobeEntry(
    val id: String = "",
    val datum: String = "",
    val sorte: String = "",
    val bemerkung: String = "",
    val geloescht: Boolean = false
)
