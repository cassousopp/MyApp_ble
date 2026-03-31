package fr.isen.sopp.myapp_ble

import kotlin.math.abs

/**
 * Algorithme de localisation par trilatération (moindres carrés linéarisés).
 */
object Trilateration {

    /**
     * Calcule la position estimée à partir d'une liste de balises et leurs distances.
     *
     * @param validAnchors Liste des balises ayant un signal suffisant (> -85 dBm).
     * @param distances Liste des distances correspondantes.
     * @return Point2D estimé.
     */
    fun calculate(validAnchors: List<AnchorBeacon>, distances: List<Double>): Point2D {
        // Logique d'exclusion : Si moins de 3 balises, retour au centre du rectangle (9.0m x 9.45m)
        if (validAnchors.size < 3 || distances.size < 3) {
            return Point2D(4.5, 4.725)
        }

        val n = validAnchors.size
        val k = n - 1 // Nombre d'équations linéarisées
        
        // On utilise la dernière balise comme référence pour la linéarisation
        val pRef = validAnchors.last().position
        val dRef = distances.last()

        // Matrices pour le système At * A * x = At * b (Moindres Carrés)
        var m11 = 0.0
        var m12 = 0.0
        var m22 = 0.0
        var v1 = 0.0
        var v2 = 0.0

        for (i in 0 until k) {
            val pi = validAnchors[i].position
            val di = distances[i]

            // Équation linéarisée : Ai * x + Bi * y = Ci
            val ai = 2 * (pRef.x - pi.x)
            val bi = 2 * (pRef.y - pi.y)
            val ci = di * di - dRef * dRef - pi.x * pi.x + pRef.x * pRef.x - pi.y * pi.y + pRef.y * pRef.y

            // Accumulation pour At * A et At * b
            m11 += ai * ai
            m12 += ai * bi
            m22 += bi * bi
            v1 += ai * ci
            v2 += bi * ci
        }

        // Résolution du système 2x2 par la règle de Cramer
        // [ m11 m12 ] [ x ] = [ v1 ]
        // [ m12 m22 ] [ y ] = [ v2 ]
        val det = m11 * m22 - m12 * m12

        return if (abs(det) < 1e-10) {
            // Cas dégénéré (balises alignées) : moyenne des positions
            var sumX = 0.0
            var sumY = 0.0
            validAnchors.forEach { sumX += it.position.x; sumY += it.position.y }
            Point2D(sumX / n, sumY / n)
        } else {
            val x = (v1 * m22 - m12 * v2) / det
            val y = (m11 * v2 - v1 * m12) / det
            Point2D(x, y)
        }
    }
}
