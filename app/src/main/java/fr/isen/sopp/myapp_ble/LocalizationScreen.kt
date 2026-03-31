package fr.isen.sopp.myapp_ble

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Écran de visualisation 2D pour la localisation indoor avec "Zone de perception" style GPS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalizationScreen(
    viewModel: LocalizationViewModel,
    onBack: () -> Unit
) {
    val position by viewModel.position.collectAsStateWithLifecycle()
    val anchors by viewModel.anchorsState.collectAsStateWithLifecycle()
    val pathPoints by viewModel.pathHistory.collectAsStateWithLifecycle()

    // Animation pour l'effet de pulsation du GPS
    val infiniteTransition = rememberInfiniteTransition(label = "gps_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Échelle : 1 mètre = 40 pixels
    val scale = 40f
    val margin = 60f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Localisation Indoor", fontWeight = FontWeight.Bold) },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Les 4 Cartes d'Ancres
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(anchors) { index, anchor ->
                    AnchorCard(index + 1, anchor)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Zone de dessin 2D (Carte)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(Color.White, shape = MaterialTheme.shapes.medium)
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // Grille de fond
                    for (i in 0..15) {
                        val pos = i * scale + margin
                        drawLine(Color(0xFFEEEEEE), Offset(pos, margin), Offset(pos, canvasHeight - margin), 1f)
                        drawLine(Color(0xFFEEEEEE), Offset(margin, pos), Offset(canvasWidth - margin, pos), 1f)
                    }

                    // Dessiner les 4 Ancres
                    anchors.forEachIndexed { index, anchor ->
                        val px = anchor.position.x.toFloat() * scale + margin
                        val py = anchor.position.y.toFloat() * scale + margin
                        drawCircle(Color.Gray.copy(alpha = 0.5f), radius = 8f, center = Offset(px, py))
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 30f
                            }
                            drawText("${index + 1}", px + 10f, py - 10f, paint)
                        }
                    }

                    // Dessiner le tracé historique
                    if (pathPoints.size > 1) {
                        val strokePath = androidx.compose.ui.graphics.Path().apply {
                            val first = pathPoints.first()
                            moveTo(first.x.toFloat() * scale + margin, first.y.toFloat() * scale + margin)
                            pathPoints.forEach { p ->
                                lineTo(p.x.toFloat() * scale + margin, p.y.toFloat() * scale + margin)
                            }
                        }
                        drawPath(
                            path = strokePath,
                            color = Color(0xFF2196F3).copy(alpha = 0.3f), // Bleu transparent pour le tracé
                            style = Stroke(width = 6f)
                        )
                    }

                    // 4. POSITION GPS (ZONE DE PERCEPTION)
                    position?.let { pos ->
                        val userX = pos.x.toFloat() * scale + margin
                        val userY = pos.y.toFloat() * scale + margin

                        val bestRssi = anchors.maxOfOrNull { it.currentRssi } ?: -100.0
                        val baseRadius = if (bestRssi > -65.0) 40f else 80f
                        val animatedRadius = baseRadius * pulseScale

                        // Halo de perception
                        drawCircle(
                            color = Color(0xFF2196F3).copy(alpha = 0.2f),
                            radius = animatedRadius,
                            center = Offset(userX, userY)
                        )

                        drawCircle(
                            color = Color(0xFF2196F3).copy(alpha = 0.4f),
                            radius = animatedRadius,
                            center = Offset(userX, userY),
                            style = Stroke(2f)
                        )

                        drawCircle(
                            color = Color(0xFF2196F3),
                            radius = 12f,
                            center = Offset(userX, userY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 12f,
                            center = Offset(userX, userY),
                            style = Stroke(3f)
                        )
                    }
                }
            }
            
            Text(
                "Le cercle bleu indique votre zone de présence probable.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun AnchorCard(number: Int, anchor: AnchorBeacon) {
    val isNearby = anchor.currentRssi > -65.0
    Card(
        elevation = CardDefaults.cardElevation(if (isNearby) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                anchor.currentRssi > -65.0 -> Color(0xFFE8F5E9)
                anchor.currentRssi > -80.0 -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = if (isNearby) Color(0xFF2E7D32) else Color.Gray,
                    modifier = Modifier.size(16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("$number", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(anchor.name, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Text("${anchor.currentRssi.toInt()} dBm", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            val dist = RssiToDistance.estimate(anchor.currentRssi.toInt(), anchor.txPower)
            Text("${"%.2f".format(dist)}m", color = if (isNearby) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}
