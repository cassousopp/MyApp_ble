package fr.isen.sopp.myapp_ble

import android.bluetooth.BluetoothGattService
import java.util.UUID

/**
 * Représente les différents événements pouvant survenir lors d'une connexion GATT.
 */
sealed class Gattevent {
    object Connected : Gattevent()
    object Disconnected : Gattevent()
    data class ServicesDiscovered(val services: List<BluetoothGattService>) : Gattevent()
    data class CharacteristicRead(val uuid: UUID, val value: ByteArray) : Gattevent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as CharacteristicRead
            if (uuid != other.uuid) return false
            if (!value.contentEquals(other.value)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = uuid.hashCode()
            result = 31 * result + value.contentHashCode()
            return result
        }
    }
    data class Notification(val uuid: UUID, val value: ByteArray) : Gattevent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Notification
            if (uuid != other.uuid) return false
            if (!value.contentEquals(other.value)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = uuid.hashCode()
            result = 31 * result + value.contentHashCode()
            return result
        }
    }
}
