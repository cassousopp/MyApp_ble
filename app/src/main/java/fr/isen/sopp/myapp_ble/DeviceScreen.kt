package fr.isen.sopp.myapp_ble

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Écran de contrôle de l'appareil BLE STM32.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    address: String,
    name: String,
    viewModel: DeviceViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(address) {
        viewModel.connect(address)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = when (uiState.connectionState) {
                                ConnectionState.CONNECTED -> "Connecté"
                                ConnectionState.CONNECTING -> "Connexion en cours..."
                                ConnectionState.DISCONNECTED -> "Déconnecté"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when (uiState.connectionState) {
                                ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                                ConnectionState.DISCONNECTED -> Color(0xFFF44336)
                                else -> Color(0xFFFF9800)
                            }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when (uiState.connectionState) {
                ConnectionState.CONNECTED -> {
                    Text("Contrôle des LEDs", style = MaterialTheme.typography.titleLarge)
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LedButton("LED 1", uiState.led1On, { viewModel.toggleLed(1, uiState.led1On) }, Modifier.weight(1f))
                        LedButton("LED 2", uiState.led2On, { viewModel.toggleLed(2, uiState.led2On) }, Modifier.weight(1f))
                        LedButton("LED 3", uiState.led3On, { viewModel.toggleLed(3, uiState.led3On) }, Modifier.weight(1f))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text("Boutons physiques", style = MaterialTheme.typography.titleLarge)
                    
                    // Affichage des 3 compteurs pour les boutons physiques
                    CounterCard("Bouton 1", uiState.buttonClickCount)
                    CounterCard("Bouton 2", uiState.button2ClickCount)
                    CounterCard("Bouton 3", uiState.button3ClickCount)

                    // Pousse le bouton de déconnexion vers le bas de l'écran
                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.BluetoothDisabled, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Déconnexion", fontWeight = FontWeight.Bold)
                    }
                }
                ConnectionState.CONNECTING -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                ConnectionState.DISCONNECTED -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Lien perdu avec l'appareil", color = Color.Gray)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = onBack) { Text("Retour au scan") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LedButton(label: String, isOn: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isOn) Color(0xFFFF9800) else Color.LightGray,
            contentColor = if (isOn) Color.White else Color.DarkGray
        )
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun CounterCard(label: String, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text("$count clics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
