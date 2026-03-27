package fr.isen.sopp.myapp_ble

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.isen.sopp.myapp_ble.ui.theme.MyApp_bleTheme

/**
 * Point d'entrée de l'application.
 * Gère la demande des permissions nécessaires au Bluetooth Low Energy dès le lancement.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = BleRepository(this)

        setContent {
            MyApp_bleTheme {
                val viewModel: BleViewModel = viewModel(
                    factory = BleViewModelFactory(repository)
                )

                val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                } else {
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    val allGranted = results.values.all { it }
                    if (!allGranted) {
                        Toast.makeText(
                            this,
                            "Permissions nécessaires pour le Bluetooth.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(permissions)
                }

                // Utilisation de MainApp qui gère la navigation entre Scan et Détail
                MainApp(viewModel = viewModel)
            }
        }
    }
}
