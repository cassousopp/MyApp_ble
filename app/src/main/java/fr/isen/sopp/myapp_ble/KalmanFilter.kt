package fr.isen.sopp.myapp_ble

/**
 * Filtre de Kalman simple pour lisser les coordonnées (x, y).
 */
class KalmanFilter(
    private val q: Double = 0.1, // Bruit de processus (process noise)
    private val r: Double = 1.0  // Bruit de mesure (measurement noise)
) {
    private var x: Double? = null
    private var y: Double? = null
    private var pX: Double = 1.0
    private var pY: Double = 1.0

    /**
     * Filtre un nouveau point (x, y) et retourne la position lissée.
     */
    fun filter(measuredX: Double, measuredY: Double): Point2D {
        // Initialisation si c'est le premier point
        val currentX = x
        val currentY = y
        if (currentX == null || currentY == null) {
            x = measuredX
            y = measuredY
            return Point2D(measuredX, measuredY)
        }

        // Prédiction (modèle statique : la position ne change pas théoriquement)
        pX += q
        pY += q

        // Mise à jour pour X
        val kX = pX / (pX + r)
        val newX = currentX + kX * (measuredX - currentX)
        pX = (1 - kX) * pX
        x = newX

        // Mise à jour pour Y
        val kY = pY / (pY + r)
        val newY = currentY + kY * (measuredY - currentY)
        pY = (1 - kY) * pY
        y = newY

        return Point2D(newX, newY)
    }
}
