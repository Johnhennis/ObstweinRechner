package app.johnhennis.obstweinrechner.data

data class FruitPrice(
    val id: String = "",
    val fruchtart: String = "",
    val jahr: Int = 0,
    val datum: String = "",
    val preis: Double = 0.0,
    val quelle: String = "",
    val geloescht: Boolean = false
)
