package app.johnhennis.obstweinrechner.data

data class WineStockItem(
    val id: String = "",
    val jahr: Int = 0,
    val sorte: String = "",
    val sollmenge: Double = 0.0,
    val aktuelleMenge: Double = 0.0,
    val geloescht: Boolean = false
)
