package app.johnhennis.obstweinrechner.data

data class InventoryItem(
    val id: String = "",
    val name: String = "",
    val einheit: String = "",
    val soll: Double = 0.0,
    val ist: Double = 0.0,
    val geloescht: Boolean = false
)
