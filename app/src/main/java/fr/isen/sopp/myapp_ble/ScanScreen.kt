package fr.isen.sopp.myapp_ble

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Écran principal gérant l'affichage de la liste, du détail ou de la localisation.
 */
@Composable
fun MainApp(viewModel: BleViewModel) {
    val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()
    val showLocalization by viewModel.showLocalization.collectAsStateWithLifecycle()

    when {
        showLocalization -> {
            val localizationViewModel: LocalizationViewModel = viewModel(
                factory = LocalizationViewModelFactory(BleRepository(LocalContext.current))
            )
            LocalizationScreen(
                viewModel = localizationViewModel,
                onBack = { viewModel.toggleLocalization(false) }
            )
        }
        selectedDevice != null -> {
            val deviceViewModel: DeviceViewModel = viewModel(
                factory = DeviceViewModelFactory(BleRepository(LocalContext.current))
            )
            DeviceScreen(
                address = selectedDevice!!.address,
                name = selectedDevice!!.name ?: "STM32",
                viewModel = deviceViewModel,
                onBack = { viewModel.disconnect() }
            )
        }
        else -> {
            ScanScreen(viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(viewModel: BleViewModel) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE Scanner", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { viewModel.toggleLocalization(true) }) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Localisation")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (isScanning) viewModel.stopScan() else viewModel.startScan() },
                containerColor = if (isScanning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                icon = { Icon(if (isScanning) Icons.Default.Stop else Icons.AutoMirrored.Filled.BluetoothSearching, null) },
                text = { Text(if (isScanning) "Arrêter" else "Scanner") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (devices.isEmpty()) {
                EmptyState(isScanning)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(devices, key = { it.address }) { device ->
                        DeviceCard(device) {
                            viewModel.selectDevice(device)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceCard(device: BleDevice, onClick: () -> Unit) {
    val animatedRssi by animateIntAsState(
        targetValue = device.rssi,
        animationSpec = tween(500),
        label = "rssi_anim"
    )

    // Calcul de la distance en temps réel pour l'UI
    val distance = RssiToDistance.estimate(device.rssi)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RssiBar(
                rssi = animatedRssi,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "Inconnu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                // AJOUT : Affichage de la distance estimée
                Text(
                    text = "Distance : ${"%.1f".format(distance)} m",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = "$animatedRssi dBm",
                style = MaterialTheme.typography.bodyMedium,
                color = rssiToColor(animatedRssi),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyState(isScanning: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
            } else {
                Icon(Icons.Default.Bluetooth, null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(if (isScanning) "Recherche d'appareils..." else "Appuyez sur Scanner", color = Color.Gray)
        }
    }
}
