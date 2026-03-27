package fr.isen.sopp.myapp_ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.experimental.and

/**
 * ViewModel gérant la logique d'interaction avec la STM32 (LEDs, Boutons).
 */
class DeviceViewModel(private val repository: BleRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceUiState())
    val uiState: StateFlow<DeviceUiState> = _uiState.asStateFlow()

    fun connect(address: String) {
        repository.connect(address) { event ->
            when (event) {
                is Gattevent.Connected -> {
                    _uiState.update { it.copy(connectionState = ConnectionState.CONNECTED) }
                }
                is Gattevent.Disconnected -> {
                    _uiState.update { it.copy(connectionState = ConnectionState.DISCONNECTED) }
                }
                is Gattevent.ServicesDiscovered -> {
                    subscribeToButtonNotifications(event.services)
                }
                is Gattevent.Notification -> {
                    handleNotification(event)
                }
                else -> {}
            }
        }
    }

    private fun subscribeToButtonNotifications(services: List<android.bluetooth.BluetoothGattService>) {
        // Selon la consigne : Service 2 Carac 1 et Service 3 Carac 0
        services.getOrNull(2)?.characteristics?.getOrNull(1)?.let {
            repository.subscribeToNotification(it)
        }
        services.getOrNull(3)?.characteristics?.getOrNull(0)?.let {
            repository.subscribeToNotification(it)
        }
    }

    /**
     * Gère la réception d'une notification (clic bouton).
     * Utilisation du masquage binaire pour détecter quel(s) bouton(s) sont pressés.
     */
    private fun handleNotification(event: Gattevent.Notification) {
        val value = event.value.getOrNull(0) ?: return
        
        _uiState.update { state ->
            var newState = state

            // Masquage binaire pour identifier les boutons (bit 0, bit 1, bit 2)
            if ((value and 0x01).toInt() != 0) {
                newState = newState.copy(buttonClickCount = newState.buttonClickCount + 1)
            }
            if ((value and 0x02).toInt() != 0) {
                newState = newState.copy(button2ClickCount = newState.button2ClickCount + 1)
            }
            if ((value and 0x04).toInt() != 0) {
                newState = newState.copy(button3ClickCount = newState.button3ClickCount + 1)
            }
            
            newState
        }
    }

    fun toggleLed(ledNumber: Int, currentlyOn: Boolean) {
        val value = if (currentlyOn) byteArrayOf(0x00) else byteArrayOf(ledNumber.toByte())
        repository.writeLed(value)
        
        _uiState.update { state ->
            when (ledNumber) {
                1 -> state.copy(led1On = !currentlyOn)
                2 -> state.copy(led2On = !currentlyOn)
                3 -> state.copy(led3On = !currentlyOn)
                else -> state
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }
}

class DeviceViewModelFactory(private val repository: BleRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeviceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeviceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
