package fr.isen.sopp.myapp_ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.LinkedList
import java.util.Queue
import java.util.UUID

/**
 * Repository gérant le scan BLE, la connexion GATT,
 * l'écriture des LEDs et l'abonnement aux notifications des boutons.
 */
class BleRepository(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var scanCallback: ScanCallback? = null
    private var gatt: BluetoothGatt? = null

    // SharedFlow pour diffuser les résultats du scan à plusieurs ViewModels
    // Ajoutez replay = 1 pour que le dernier appareil détecté soit immédiatement envoyé aux nouveaux abonnés
    private val _scanResults = MutableSharedFlow<BleDevice>(replay = 1, extraBufferCapacity = 64)
    val scanResults: SharedFlow<BleDevice> = _scanResults.asSharedFlow()

    // UUIDs STM32
    private val serviceUuid = UUID.fromString("0000fe40-cc7a-482a-984a-7f2ed5b3e58f")
    private val ledUuid = UUID.fromString("0000fe41-8e22-4541-9d4c-21edae82ed19")
    private val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val operationQueue: Queue<BluetoothGattDescriptor> = LinkedList()
    private var isOperationPending = false

    @SuppressLint("MissingPermission")
    fun startScan(onDeviceFound: ((BleDevice) -> Unit)? = null) {
        if (scanCallback != null) return // Déjà en train de scanner

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val filters = listOf(ScanFilter.Builder().build())

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val deviceName = result.device.name ?: result.scanRecord?.deviceName ?: "Inconnu"
                val device = BleDevice(
                    name = deviceName,
                    address = result.device.address,
                    rssi = result.rssi
                )
                // On diffuse via le Flow ET le callback optionnel (pour compatibilité)
                _scanResults.tryEmit(device)
                onDeviceFound?.invoke(device)
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e("BleRepository", "Scan failed: $errorCode")
            }
        }

        bluetoothAdapter?.bluetoothLeScanner?.startScan(filters, settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanCallback?.let {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(it)
            scanCallback = null
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(deviceAddress: String, onEvent: (Gattevent) -> Unit) {
        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress) ?: return

        gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    onEvent(Gattevent.Connected)
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    onEvent(Gattevent.Disconnected)
                    operationQueue.clear()
                    isOperationPending = false
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    onEvent(Gattevent.ServicesDiscovered(gatt.services))
                }
            }

            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    onEvent(Gattevent.CharacteristicRead(characteristic.uuid, value))
                }
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                onEvent(Gattevent.Notification(characteristic.uuid, value))
            }

            override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                isOperationPending = false
                processNextOperation()
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun subscribeToNotification(characteristic: BluetoothGattCharacteristic) {
        gatt?.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(cccdUuid)
        if (descriptor != null) {
            operationQueue.add(descriptor)
            processNextOperation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun processNextOperation() {
        if (isOperationPending || operationQueue.isEmpty()) return
        val descriptor = operationQueue.poll() ?: return
        isOperationPending = true
        if (Build.VERSION.SDK_INT >= 33) {
            gatt?.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            @Suppress("DEPRECATION")
            gatt?.writeDescriptor(descriptor)
        }
    }

    @SuppressLint("MissingPermission")
    fun writeLed(value: ByteArray) {
        val service = gatt?.getService(serviceUuid)
        val characteristic = service?.getCharacteristic(ledUuid)
        if (characteristic != null) {
            if (Build.VERSION.SDK_INT >= 33) {
                gatt?.writeCharacteristic(characteristic, value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = value
                @Suppress("DEPRECATION")
                gatt?.writeCharacteristic(characteristic)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        operationQueue.clear()
        isOperationPending = false
    }
}
