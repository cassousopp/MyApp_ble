package fr.isen.sopp.myapp_ble

/**
 * Modèle de données représentant un appareil BLE détecté.
 *
 * @property name Nom affiché de l'appareil (peut être null).
 * @property address Adresse MAC unique de l'appareil.
 * @property rssi Puissance du signal reçu en dBm.
 * @property lastSeen Timestamp de la dernière détection (par défaut : maintenant).
 */
data class BleDevice(
    val name: String?,
    val address: String,
    val rssi: Int,
    val lastSeen: Long = System.currentTimeMillis()
)
