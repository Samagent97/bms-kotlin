package com.battery.bms.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.battery.bms.data.AppLogger
import com.battery.bms.data.BmsDevice
import com.battery.bms.protocol.BmsProtocol
import com.battery.bms.protocol.BmsProtocol.hex
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

class BleManager(private val context: Context) {
    private val adapter: BluetoothAdapter? = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private var gatt: BluetoothGatt? = null
    private var writeChar: BluetoothGattCharacteristic? = null
    private var notifyChar: BluetoothGattCharacteristic? = null
    private var scanCallback: ScanCallback? = null
    private val queue = ConcurrentLinkedQueue<Pending>()
    private var active: Pending? = null

    data class Pending(val command: ByteArray, val expectedLength: Int?, val result: CompletableDeferred<ByteArray>) {
        val buffer = mutableListOf<Byte>()
        var dynamicExpected: Int? = expectedLength
    }

    fun hasBlePermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= 31) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    }

    fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun scan(onDevice: (BmsDevice) -> Unit) {
        if (!hasBlePermissions()) {
            AppLogger.push("warn", "BLE permissions are not granted")
            return
        }
        if (!isBluetoothEnabled()) {
            AppLogger.push("warn", "Bluetooth is turned off")
            return
        }
        stopScan()
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = result.device.name ?: result.scanRecord?.deviceName ?: ""
                val serviceMatch = result.scanRecord?.serviceUuids?.any { it.uuid.toString().uppercase().startsWith(BmsProtocol.targetServicePrefix) } == true
                if (name == BmsProtocol.targetName || serviceMatch) {
                    onDevice(BmsDevice(result.device.address, name.ifBlank { BmsProtocol.targetName }, result.rssi))
                }
            }
        }
        scanCallback?.let { adapter?.bluetoothLeScanner?.startScan(it) }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!hasBlePermissions()) return
        scanCallback?.let { adapter?.bluetoothLeScanner?.stopScan(it) }
        scanCallback = null
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(deviceId: String): BmsDevice = withContext(Dispatchers.IO) {
        if (!hasBlePermissions()) error("BLE permissions are not granted")
        if (!isBluetoothEnabled()) error("Bluetooth is turned off")
        stopScan()
        val device = adapter?.getRemoteDevice(deviceId) ?: error("Device not found")
        val ready = CompletableDeferred<Unit>()
        gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    if (!g.discoverServices() && !ready.isCompleted) ready.completeExceptionally(IllegalStateException("Service discovery failed"))
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    AppLogger.push("warn", "BLE disconnected")
                    if (!ready.isCompleted) ready.completeExceptionally(IllegalStateException("BLE disconnected"))
                }
            }

            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                if (status != GATT_SUCCESS) {
                    ready.completeExceptionally(IllegalStateException("Service discovery failed: $status"))
                    return
                }
                val service = g.getService(BmsProtocol.serviceUuid) ?: pickService(g.services)
                writeChar = service?.characteristics?.firstOrNull { it.uuid == BmsProtocol.writeCharUuid || it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 || it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0 }
                notifyChar = service?.characteristics?.firstOrNull { it.uuid == BmsProtocol.notifyCharUuid || it.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 || it.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0 }
                if (writeChar == null || notifyChar == null) {
                    ready.completeExceptionally(IllegalStateException("BMS BLE service not found"))
                    return
                }
                notifyChar?.let {
                    g.setCharacteristicNotification(it, true)
                    val descriptor = it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    if (descriptor != null) {
                        if (!g.writeDescriptor(descriptor)) ready.completeExceptionally(IllegalStateException("Notification setup failed"))
                    } else {
                        ready.complete(Unit)
                    }
                }
            }

            override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                if (status == GATT_SUCCESS) ready.complete(Unit) else ready.completeExceptionally(IllegalStateException("Notification setup failed: $status"))
            }

            override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                receive(characteristic.value)
            }
        })
        ready.await()
        AppLogger.push("info", "connected ${device.name ?: device.address}")
        BmsDevice(device.address, device.name ?: BmsProtocol.targetName, null)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        stopScan()
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        writeChar = null
        notifyChar = null
        active?.result?.completeExceptionally(IllegalStateException("Disconnected"))
        active = null
        queue.clear()
    }

    suspend fun send(command: ByteArray, expectedLength: Int? = null): ByteArray {
        val pending = Pending(command, expectedLength, CompletableDeferred())
        queue.add(pending)
        pump()
        return pending.result.await()
    }

    @SuppressLint("MissingPermission")
    private fun pump() {
        if (active != null) return
        val next = queue.poll() ?: return
        active = next
        AppLogger.push("debug", "tx ${next.command.hex()}")
        val g = gatt ?: run {
            next.result.completeExceptionally(IllegalStateException("Not connected"))
            active = null
            return
        }
        val ch = writeChar ?: run {
            next.result.completeExceptionally(IllegalStateException("Write characteristic not found"))
            active = null
            return
        }
        next.command.asIterable().chunked(20).forEach { bytes ->
            ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            ch.value = bytes.toByteArray()
            g.writeCharacteristic(ch)
        }
    }

    private fun receive(bytes: ByteArray) {
        val pending = active ?: return
        pending.buffer.addAll(bytes.toList())
        if (pending.dynamicExpected == null && pending.buffer.size >= 3) pending.dynamicExpected = (pending.buffer[2].toInt() and 0xff) + 5
        val expected = pending.dynamicExpected
        if (expected != null && pending.buffer.size >= expected) {
            val frame = pending.buffer.take(expected).toByteArray()
            AppLogger.push("debug", "rx ${frame.hex()}")
            pending.result.complete(frame)
            active = null
            pump()
        }
    }

    private fun pickService(services: List<BluetoothGattService>): BluetoothGattService? =
        services.firstOrNull { service ->
            val hasWrite = service.characteristics.any { it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 || it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0 }
            val hasNotify = service.characteristics.any { it.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 || it.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0 }
            hasWrite && hasNotify
        }
}
