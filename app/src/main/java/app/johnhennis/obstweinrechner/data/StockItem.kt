package app.johnhennis.obstweinrechner.data

// Mengenfelder bewusst als Text statt Zahl: manche Positionen in der
// Originaltabelle stehen als "viele" statt einer Zahl (z.B. Kaffee-
// Rührstäbchen), eine Zahl-Pflicht würde das crashen lassen.
data class StockItem(
    val id: String = "",
    val jahr: Int = 0,
    val art: String = "",
    val quelle: String = "",
    val einheit: String = "",
    val bestandVorjahr: String = "",
    val bedarf: String = "",
    val rest: String = "",
    val bemerkung: String = "",
    val geloescht: Boolean = false
)
