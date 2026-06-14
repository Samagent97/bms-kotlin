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
    val mockMode: Boolean = true,
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
        refreshAll()
    }

    fun setMockMode(enabled: Boolean) {
        repository.mockMode = enabled
        _state.update { it.copy(mockMode = enabled, devices = emptyList(), connected = null) }
        scan()
    }

    fun scan() {
        repository.scan { device ->
            _state.update { state ->
                val merged = (state.devices.filterNot { it.id == device.id } + device).sortedByDescending { it.rssi ?: -999 }
                state.copy(devices = merged)
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
