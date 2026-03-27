package fr.isen.sopp.myapp_ble

enum class ConnectionState {
    CONNECTING,
    CONNECTED,
    DISCONNECTED
}

/**
 * État de l'interface utilisateur enrichi pour les 3 boutons.
 */
data class DeviceUiState(
    val connectionState: ConnectionState = ConnectionState.CONNECTING,
    val led1On: Boolean = false,
    val led2On: Boolean = false,
    val led3On: Boolean = false,
    val buttonClickCount: Int = 0,  // Bouton 1
    val button2ClickCount: Int = 0, // Bouton 2
    val button3ClickCount: Int = 0  // Bouton 3
)
