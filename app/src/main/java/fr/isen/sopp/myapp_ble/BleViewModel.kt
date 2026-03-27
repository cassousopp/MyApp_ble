package fr.isen.sopp.myapp_ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel gérant l'état de l'interface de scan et la logique métier.
 */
class BleViewModel(private val repository: BleRepository) : ViewModel() {

    // Map interne pour stocker les appareils par adresse MAC (déduplication)
    private val _devices = MutableStateFlow<Map<String, BleDevice>>(emptyMap())

    // StateFlow exposé à l'UI, trié par RSSI décroissant
    val devices: StateFlow<List<BleDevice>> = _devices
        .map { it.values.sortedByDescending { device -> device.rssi } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // État du scan (en cours ou arrêté)
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning
    
    // État de la connexion GATT
    private val _gattEvent = MutableStateFlow<Gattevent?>(null)
    val gattEvent: StateFlow<Gattevent?> = _gattEvent

    // Appareil actuellement sélectionné pour le détail
    private val _selectedDevice = MutableStateFlow<BleDevice?>(null)
    val selectedDevice: StateFlow<BleDevice?> = _selectedDevice

    // État pour afficher ou non la vue de localisation
    private val _showLocalization = MutableStateFlow(false)
    val showLocalization: StateFlow<Boolean> = _showLocalization.asStateFlow()

    init {
        // Collecte des résultats de scan depuis le SharedFlow du repository
        viewModelScope.launch {
            repository.scanResults.collect { device ->
                if (_isScanning.value) {
                    _devices.value = _devices.value + (device.address to device)
                }
            }
        }
    }

    /**
     * Démarre le scan BLE.
     */
    fun startScan() {
        if (_isScanning.value) return
        _isScanning.value = true
        _devices.value = emptyMap()
        repository.startScan()
    }

    /**
     * Arrête le scan BLE.
     */
    fun stopScan() {
        _isScanning.value = false
        repository.stopScan()
    }

    /**
     * Sélectionne un appareil et lance la connexion GATT.
     */
    fun selectDevice(device: BleDevice) {
        _selectedDevice.value = device
        stopScan()
        repository.connect(device.address) { event ->
            _gattEvent.value = event
        }
    }

    /**
     * Déconnecte l'appareil et revient à la liste.
     */
    fun disconnect() {
        repository.disconnect()
        _gattEvent.value = Gattevent.Disconnected
        _selectedDevice.value = null
    }

    /**
     * Alterne l'affichage de la localisation.
     */
    fun toggleLocalization(show: Boolean) {
        _showLocalization.value = show
        // Note: On ne stop pas forcément le scan ici si LocalizationViewModel en a besoin
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
        repository.disconnect()
    }
}

/**
 * Factory pour créer le BleViewModel avec son repository.
 */
class BleViewModelFactory(private val repository: BleRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BleViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
