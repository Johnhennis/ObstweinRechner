package app.johnhennis.obstweinrechner.data

data class WineOrder(
    val id: String = "",
    val jahr: Int = 0,
    val wer: String = "",
    val sorte: String = "",
    val menge: Double = 0.0,
    // Format "JJJJ-MM-TTTHH:mm"; leer = kein Termin gesetzt
    val wannZeitpunkt: String = "",
    // Stunden vor dem Termin, zu denen jeweils eine eigene Erinnerung
    // ausgeloest wird (z.B. [24, 1] = einen Tag und eine Stunde vorher)
    val erinnerungenStunden: List<Int> = emptyList(),
    val abgefuellt: Boolean = false,
    val abgeholt: Boolean = false,
    val geloescht: Boolean = false
)
