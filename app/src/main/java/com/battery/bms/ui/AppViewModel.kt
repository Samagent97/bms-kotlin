package com.battery.bms.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.battery.bms.ble.BleManager
import com.battery.bms.data.BatterySnapshot
import com.battery.bms.data.BmsDevice
import com.battery.bms.data.HealthState
import com.battery.bms.data.ParameterSettings
import com.battery.bms.data.SwitchState
import com.battery.bms.data.TemperatureSnapshot
import com.battery.bms.repository.BmsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val mockMode: Boolean = false,
    val devices: List<BmsDevice> = emptyList(),
    val connected: BmsDevice? = null,
    val switches: SwitchState? = null,
    val health: HealthState? = null,
    val battery: BatterySnapshot? = null,
    val temperatures: TemperatureSnapshot? = null,
    val parameters: ParameterSettings = ParameterSettings(),
    val message: String = ""
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BmsRepository(BleManager(application))
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        scan()
    }

    fun setMockMode(enabled: Boolean) {
        repository.mockMode = enabled
        repository.disconnect()
        _state.update {
            it.copy(
                mockMode = enabled,
                devices = emptyList(),
                connected = null,
                switches = null,
                health = null,
                battery = null,
                temperatures = null,
                message = if (enabled) "Mock mode is for screen testing only" else ""
            )
        }
        scan()
    }

    fun scan() {
        if (!repository.mockMode && !repository.hasBlePermissions()) {
            setMessage("Allow Nearby devices/Bluetooth permission, then scan again")
            return
        }
        if (!repository.mockMode && !repository.isBluetoothEnabled()) {
            setMessage("Turn on Bluetooth, then scan again")
            return
        }
        _state.update { it.copy(message = if (repository.mockMode) it.message else "Scanning for BRT_AFE2616...") }
        repository.scan { device ->
            _state.update { state ->
                val merged = (state.devices.filterNot { it.id == device.id } + device).sortedByDescending { it.rssi ?: -999 }
                state.copy(devices = merged, message = "")
            }
        }
    }

    fun connect(device: BmsDevice) = viewModelScope.launch {
        runCatching {
            repository.connect(device)
            _state.update { it.copy(connected = repository.connectedDevice) }
            refreshAll()
        }.onFailure { setMessage(it.message ?: "Connect failed") }
    }

    fun disconnect() {
        repository.disconnect()
        _state.update { it.copy(connected = null) }
    }

    fun refreshAll() = viewModelScope.launch {
        if (repository.connectedDevice == null) {
            setMessage("Connect to a BMS first")
            return@launch
        }
        runCatching {
            _state.update {
                it.copy(
                    switches = repository.readSwitches(),
                    health = repository.readHealth(),
                    battery = repository.readBattery(),
                    temperatures = repository.readTemperatures(),
                    parameters = repository.readParameters()
                )
            }
        }.onFailure { setMessage(it.message ?: "Refresh failed") }
    }

    fun updateParameters(settings: ParameterSettings) {
        _state.update { it.copy(parameters = settings) }
    }

    fun saveParameters() = viewModelScope.launch {
        runCatching { repository.writeParameters(_state.value.parameters) }
            .onSuccess { setMessage("Parameters saved") }
            .onFailure { setMessage(it.message ?: "Save failed") }
    }

    fun setCharge(enabled: Boolean) = viewModelScope.launch { repository.setCharge(enabled); _state.update { it.copy(switches = it.switches?.copy(chargeMosEnabled = enabled)) } }
    fun setDischarge(enabled: Boolean) = viewModelScope.launch { repository.setDischarge(enabled); _state.update { it.copy(switches = it.switches?.copy(dischargeMosEnabled = enabled)) } }
    fun setBalance(enabled: Boolean) = viewModelScope.launch { repository.setBalance(enabled); _state.update { it.copy(switches = it.switches?.copy(balanceEnabled = enabled)) } }
    fun shutdown() = viewModelScope.launch { repository.shutdown(); setMessage("Shutdown command sent") }

    private fun setMessage(message: String) {
        _state.update { it.copy(message = message) }
    }
}
