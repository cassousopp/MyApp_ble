package fr.isen.sopp.myapp_ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel optimisé pour une actualisation ultra-réactive et un lissage performant.
 */
class LocalizationViewModel(private val repo: BleRepository) : ViewModel() {

    // 1. Configuration des 4 Ancres selon les coordonnées demandées
    private val initialAnchors = listOf(
        AnchorBeacon("Emeric3", "00:80:E1:26:C6:64", Point2D(0.0, 0.0), -59),  // Haut Gauche
        AnchorBeacon("Stmaur", "00:80:E1:27:AF:9E", Point2D(9.45, 0.0), -54),   // Haut Droit
        AnchorBeacon("Emeric", "00:80:E1:27:92:0A", Point2D(0.0, 9.0), -57),    // Bas Gauche
        AnchorBeacon("Emeric2", "00:80:E1:27:B5:87", Point2D(8.87, 9.0), -66)  // Bas Droit
    )

    private val _anchorsState = MutableStateFlow(initialAnchors)
    val anchorsState: StateFlow<List<AnchorBeacon>> = _anchorsState.asStateFlow()

    private val _position = MutableStateFlow<Point2D?>(null)
    val position: StateFlow<Point2D?> = _position.asStateFlow()

    // Liste pour stocker le tracé du chemin (Historique)
    private val _pathHistory = MutableStateFlow<List<Point2D>>(emptyList())
    val pathHistory: StateFlow<List<Point2D>> = _pathHistory.asStateFlow()

    private val _isScanning = MutableStateFlow(true)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Filtre de Kalman réglé pour être RÉACTIF
    private val kalmanFilter = KalmanFilter(q = 0.2, r = 0.4)

    // Fenêtre de lissage RSSI très courte pour le temps réel
    private val rssiWindows = mutableMapOf<String, MutableList<Int>>()
    private val windowSize = 5

    init {
        // Collecte des résultats de scan depuis le SharedFlow pour ne pas bloquer le scan principal
        viewModelScope.launch {
            repo.scanResults.collect { device ->
                if (_isScanning.value) {
                    onDeviceScanned(device)
                }
            }
        }
        // Démarre le scan physique si ce n'est pas déjà fait
        repo.startScan()
    }

    fun toggleScan() {
        if (_isScanning.value) {
            _isScanning.value = false
        } else {
            _isScanning.value = true
            repo.startScan()
        }
    }

    private fun onDeviceScanned(device: BleDevice) {
        val anchorIndex = initialAnchors.indexOfFirst { it.address == device.address }
        if (anchorIndex == -1) return

        // Mise à jour de la moyenne glissante
        val window = rssiWindows.getOrPut(device.address) { mutableListOf() }
        window.add(device.rssi)
        if (window.size > windowSize) window.removeAt(0)
        val newAvg = window.average()

        // Mise à jour réactive de l'UI
        _anchorsState.update { currentList ->
            currentList.map {
                if (it.address == device.address) it.copy(currentRssi = newAvg) else it
            }
        }

        updatePosition()
    }

    private fun updatePosition() {
        val currentAnchors = _anchorsState.value
        // Seuil strict : exclusion si < -80 dBm
        val validAnchors = currentAnchors.filter { it.currentRssi > -80.0 }

        if (validAnchors.size < 3) return

        val distances = validAnchors.map {
            RssiToDistance.estimate(it.currentRssi.toInt(), it.txPower)
        }

        val rawPosition = Trilateration.calculate(validAnchors, distances)
        val smoothed = kalmanFilter.filter(rawPosition.x, rawPosition.y)

        val newPos = Point2D(
            smoothed.x.coerceIn(-0.5, 10.0),
            smoothed.y.coerceIn(-0.5, 9.5)
        )
        _position.value = newPos

        // On garde les 20 derniers points pour dessiner une "traînée."
        _pathHistory.update { list ->
            (list + newPos).takeLast(20)
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}

class LocalizationViewModelFactory(private val repository: BleRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LocalizationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LocalizationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
