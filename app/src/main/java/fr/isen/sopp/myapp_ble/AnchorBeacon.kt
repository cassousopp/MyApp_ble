package fr.isen.sopp.myapp_ble

/**
 * Modèle représentant une balise BLE fixe (ancre) pour la localisation.
 */
data class AnchorBeacon(
    val name: String,
    val address: String,
    val position: Point2D,
    val txPower: Int,
    var currentRssi: Double = -100.0,
    val rssiHistory: MutableList<Int> = mutableListOf()
)
