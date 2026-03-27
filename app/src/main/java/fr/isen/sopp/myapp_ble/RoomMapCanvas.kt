package fr.isen.sopp.myapp_ble

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

/**
 * Plan 2D dessiné avec Compose Canvas pour la localisation indoor (9.0m x 9.45m).
 */
@Composable
fun RoomMapCanvas(
    anchors: List<AnchorBeacon>,
    position: Point2D?,
    modifier: Modifier = Modifier,
    roomWidth: Float = 9.0f,
    roomHeight: Float = 9.45f
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(roomWidth / roomHeight)
    ) {
        // 2. Calcul des échelles pour occuper tout l'écran
        val scaleX = size.width / roomWidth
        val scaleY = size.height / roomHeight

        // 3. Fond de la salle
        drawRect(color = Color(0xFFF5F5F5)) // gris clair
        drawRect(
            color = Color(0xFF90A4AE), // bordure gris-bleu
            style = Stroke(width = 3f)
        )

        // 4. Pour chaque balise anchor
        anchors.forEach { anchor ->
            val cx = (anchor.position.x * scaleX).toFloat()
            val cy = (anchor.position.y * scaleY).toFloat()

            // Dessiner le point bleu pour la balise
            drawCircle(
                color = Color.Blue,
                radius = 20f,
                center = Offset(cx, cy)
            )

            // Afficher le nom de l'ancre via nativeCanvas
            drawContext.canvas.nativeCanvas.drawText(
                anchor.name,
                cx + 25f,
                cy + 10f,
                android.graphics.Paint().apply {
                    color = Color.Black.toArgb()
                    textSize = 30f
                    isAntiAlias = true
                }
            )
        }

        // 5. Si position != null, dessiner la position estimée
        position?.let { pos ->
            // Contrainte de position dans les limites du canvas
            val px = (pos.x * scaleX).toFloat().coerceIn(0f, size.width)
            val py = (pos.y * scaleY).toFloat().coerceIn(0f, size.height)

            // Point rouge pour l'utilisateur
            drawCircle(
                color = Color.Red,
                radius = 28f,
                center = Offset(px, py)
            )

            // Cercle rouge clair pour le rayon d'incertitude
            drawCircle(
                color = Color.Red.copy(alpha = 0.3f),
                radius = 50f,
                center = Offset(px, py),
                style = Stroke(width = 2f)
            )
        }
    }
}
