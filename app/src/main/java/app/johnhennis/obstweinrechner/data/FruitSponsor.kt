package app.johnhennis.obstweinrechner.data

data class FruitSponsor(
    val id: String = "",
    val jahr: Int = 0,
    val wer: String = "",
    val sorte: String = "",
    val geschenkt: Boolean = false,
    val geloescht: Boolean = false
)
