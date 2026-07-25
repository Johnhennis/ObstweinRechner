package app.johnhennis.obstweinrechner.data

data class ManualShoppingItem(
    val id: String = "",
    val name: String = "",
    val menge: String = "",
    val quelle: String = "",
    val erledigt: Boolean = false
)
