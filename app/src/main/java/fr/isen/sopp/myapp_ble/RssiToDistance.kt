package fr.isen.sopp.myapp_ble

import kotlin.math.pow

/**
 * Objet utilitaire pour estimer la distance entre le téléphone et l'appareil BLE.
 */
object RssiToDistance {

    /**
     * Estime la distance en mètres.
     *
     * Formule : distance = 10 ^ ((txPower - rssi) / (10 * n))
     * @param rssi Puissance du signal reçu.
     * @param txPower Puissance à 1 mètre (par défaut -59 dBm).
     * @param n Facteur d'atténuation (2.5 par défaut pour intérieur).
     */
    fun estimate(rssi: Int, txPower: Int = -59): Double {
        val n = 2.5
        val exponent = (txPower - rssi) / (10.0 * n)
        val rawDistance = 10.0.pow(exponent)

        // Contraintes : entre 0.1m et 20.0m
        return rawDistance.coerceIn(0.1, 20.0)
    }
}
