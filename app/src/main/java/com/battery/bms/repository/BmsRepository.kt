package com.battery.bms.repository

import com.battery.bms.ble.BleManager
import com.battery.bms.data.BatterySnapshot
import com.battery.bms.data.BmsDevice
import com.battery.bms.data.HealthState
import com.battery.bms.data.MockData
import com.battery.bms.data.ParameterSettings
import com.battery.bms.data.SwitchState
import com.battery.bms.data.TemperatureSnapshot
import com.battery.bms.protocol.BmsProtocol

class BmsRepository(private val bleManager: BleManager) {
    var mockMode: Boolean = false
    var connectedDevice: BmsDevice? = null
        private set

    fun hasBlePermissions(): Boolean = bleManager.hasBlePermissions()
    fun isBluetoothEnabled(): Boolean = bleManager.isBluetoothEnabled()

    fun scan(onDevice: (BmsDevice) -> Unit) {
        if (mockMode) onDevice(MockData.device) else bleManager.scan(onDevice)
    }

    suspend fun connect(device: BmsDevice) {
        connectedDevice = if (mockMode) device else bleManager.connect(device.id)
    }

    fun disconnect() {
        bleManager.disconnect()
        connectedDevice = null
    }

    suspend fun readSwitches(): SwitchState = if (mockMode) MockData.switches else BmsProtocol.parseSwitches(bleManager.send(BmsProtocol.readSwitches()))
    suspend fun readHealth(): HealthState = if (mockMode) MockData.health else BmsProtocol.parseHealth(bleManager.send(BmsProtocol.readHealth()))
    suspend fun readBattery(): BatterySnapshot = if (mockMode) MockData.battery else BmsProtocol.parseBattery(bleManager.send(BmsProtocol.readDashboard()))
    suspend fun readTemperatures(): TemperatureSnapshot = if (mockMode) MockData.temps else BmsProtocol.parseTemperatures(bleManager.send(BmsProtocol.readTemperatures()))
    suspend fun readParameters(): ParameterSettings = if (mockMode) MockData.params else BmsProtocol.parseParameters(bleManager.send(BmsProtocol.readParameters(), 63))
    suspend fun writeParameters(settings: ParameterSettings) { if (!mockMode) bleManager.send(BmsProtocol.writeParameters(settings), 8) }
    suspend fun setCharge(enabled: Boolean) { if (!mockMode) bleManager.send(BmsProtocol.toggleCharge(enabled)) }
    suspend fun setDischarge(enabled: Boolean) { if (!mockMode) bleManager.send(BmsProtocol.toggleDischarge(enabled)) }
    suspend fun setBalance(enabled: Boolean) { if (!mockMode) bleManager.send(BmsProtocol.toggleBalance(enabled)) }
    suspend fun shutdown() { if (!mockMode) bleManager.send(BmsProtocol.shutdown()) }
}
