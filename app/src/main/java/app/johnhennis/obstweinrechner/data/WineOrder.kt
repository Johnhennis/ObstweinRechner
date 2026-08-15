package app.johnhennis.obstweinrechner.data

data class WineOrderItem(
    val sorte: String = "",
    val menge: Double = 0.0
)

data class WineOrder(
    val id: String = "",
    val jahr: Int = 0,
    val wer: String = "",
    // Mehrere Sorten pro Person moeglich - fruehere Versionen hatten hier
    // ein einzelnes sorte/menge-Feld, siehe migrateToPositionen() im
    // Repository fuer die Uebernahme bereits bestehender Bestellungen.
    val positionen: List<WineOrderItem> = emptyList(),
    val wannZeitpunkt: String = "",
    val erinnerungenStunden: List<Int> = emptyList(),
    val abgefuellt: Boolean = false,
    val abgeholt: Boolean = false,
    val geloescht: Boolean = false
)
