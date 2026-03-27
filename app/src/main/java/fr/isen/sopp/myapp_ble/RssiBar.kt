package fr.isen.sopp.myapp_ble

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Retourne un niveau de signal entre 0 et 4 selon la valeur du RSSI.
 */
fun rssiToLevel(rssi: Int): Int {
    return when {
        rssi >= -60 -> 4
        rssi >= -70 -> 3
        rssi >= -80 -> 2
        rssi >= -90 -> 1
        else -> 0
    }
}

/**
 * Retourne une couleur selon la force du signal RSSI.
 */
fun rssiToColor(rssi: Int): Color {
    return when {
        rssi >= -60 -> Color(0xFF4CAF50) // vert
        rssi >= -75 -> Color(0xFFFF9800) // orange
        else -> Color(0xFFF44336)        // rouge
    }
}

/**
 * Composant affichant une barre de signal à 4 niveaux.
 */
@Composable
fun RssiBar(rssi: Int, modifier: Modifier = Modifier) {
    val level = rssiToLevel(rssi)
    val color = rssiToColor(rssi)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // On dessine 4 barres (index de 1 à 4)
        for (i in 1..4) {
            val isActive = i <= level
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height((i * 8).dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(
                        if (isActive) color else Color.Gray.copy(alpha = 0.3f)
                    )
            )
        }
    }
}
