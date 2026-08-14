package app.johnhennis.obstweinrechner.data

data class WineOrder(
    val id: String = "",
    val jahr: Int = 0,
    val wer: String = "",
    val sorte: String = "",
    val menge: Double = 0.0,
    // ISO JJJJ-MM-TT; leer = kein Termin/keine Erinnerung gesetzt
    val wannDatum: String = "",
    val abgefuellt: Boolean = false,
    val abgeholt: Boolean = false,
    val geloescht: Boolean = false
)
