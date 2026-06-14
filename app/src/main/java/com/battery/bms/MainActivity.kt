package com.battery.bms

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.battery.bms.data.AppLogger
import com.battery.bms.data.ParameterSettings
import com.battery.bms.ui.AppViewModel
import com.battery.bms.ui.UiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BmsApp() }
    }
}

@Composable
fun BmsApp(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var tab by remember { mutableStateOf("Scan") }
    val permissions = if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT) else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vm.scan() }
    LaunchedEffect(Unit) { launcher.launch(permissions) }
    MaterialTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    listOf("Scan", "Dashboard", "Cells", "Controls", "Settings", "Logs").forEach {
                        NavigationBarItem(selected = tab == it, onClick = { tab = it }, icon = {}, label = { Text(it) })
                    }
                }
            }
        ) { padding ->
            Surface(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    "Scan" -> ScanScreen(state, vm)
                    "Dashboard" -> DashboardScreen(state, vm)
                    "Cells" -> CellsScreen(state)
                    "Controls" -> ControlsScreen(state, vm)
                    "Settings" -> SettingsScreen(state, vm)
                    "Logs" -> LogsScreen()
                }
            }
        }
    }
}

@Composable
fun Screen(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        content()
    }
}

@Composable
fun InfoCard(content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() } }
}

@Composable
fun RowText(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun ScanScreen(state: UiState, vm: AppViewModel) = Screen("Device Scan") {
    InfoCard {
        RowText("Mock data mode", if (state.mockMode) "On" else "Off")
        Switch(checked = state.mockMode, onCheckedChange = vm::setMockMode)
        state.connected?.let { RowText("Connected", it.name) }
    }
    state.devices.forEach { device ->
        InfoCard {
            RowText(device.name, "RSSI ${device.rssi ?: "n/a"}")
            Text(device.id)
            Button(onClick = { vm.connect(device) }) { Text("Connect") }
        }
    }
    if (state.connected != null) Button(onClick = vm::disconnect) { Text("Disconnect") }
}

@Composable
fun DashboardScreen(state: UiState, vm: AppViewModel) = Screen("Dashboard") {
    Button(onClick = vm::refreshAll) { Text("Refresh") }
    InfoCard {
        RowText("SOC", "${state.battery?.socPercent ?: 0}%")
        RowText("Total voltage", "%.2f V".format(state.battery?.totalVoltage ?: 0.0))
        RowText("Current", "%.2f A".format(state.battery?.current ?: 0.0))
        RowText("Cycles", "${state.battery?.cycleCount ?: 0}")
    }
    InfoCard {
        RowText("Charge MOS", if (state.switches?.chargeMosEnabled == true) "On" else "Off")
        RowText("Discharge MOS", if (state.switches?.dischargeMosEnabled == true) "On" else "Off")
        RowText("Balance", if (state.switches?.balanceEnabled == true) "On" else "Off")
        RowText("Battery health", if (state.health?.batteryGood != false) "Good" else "Fault")
    }
    InfoCard {
        RowText("MOS temp", "%.1f C".format(state.temperatures?.mos ?: 0.0))
        RowText("Balance temp", "%.1f C".format(state.temperatures?.balance ?: 0.0))
        RowText("T1 / T2", "%.1f / %.1f C".format(state.temperatures?.t1 ?: 0.0, state.temperatures?.t2 ?: 0.0))
    }
}

@Composable
fun CellsScreen(state: UiState) = Screen("Cell Voltages") {
    state.battery?.cellVoltages.orEmpty().forEachIndexed { index, voltage ->
        InfoCard { RowText("Cell ${index + 1}", "%.3f V".format(voltage)) }
    }
}

@Composable
fun ControlsScreen(state: UiState, vm: AppViewModel) = Screen("BMS Controls") {
    InfoCard { RowText("Charge MOS", ""); Switch(state.switches?.chargeMosEnabled == true, vm::setCharge) }
    InfoCard { RowText("Discharge MOS", ""); Switch(state.switches?.dischargeMosEnabled == true, vm::setDischarge) }
    InfoCard { RowText("Balance", ""); Switch(state.switches?.balanceEnabled == true, vm::setBalance) }
    Button(onClick = vm::shutdown) { Text("BMS Shutdown") }
}

@Composable
fun SettingsScreen(state: UiState, vm: AppViewModel) = Screen("Parameter Settings") {
    var p by remember(state.parameters) { mutableStateOf(state.parameters) }
    @Composable
    fun field(label: String, value: Double, update: (Double) -> ParameterSettings) {
        InfoCard {
            Text(label)
            TextField(value = value.toString(), onValueChange = { text -> text.toDoubleOrNull()?.let { p = update(it); vm.updateParameters(p) } })
        }
    }
    field("Single over voltage", p.singleOverVoltage) { p.copy(singleOverVoltage = it) }
    field("Total over voltage", p.totalOverVoltage) { p.copy(totalOverVoltage = it) }
    field("Single under voltage", p.singleUnderVoltage) { p.copy(singleUnderVoltage = it) }
    field("Charge over current", p.chargeOverCurrent) { p.copy(chargeOverCurrent = it) }
    field("Discharge over current", p.dischargeOverCurrent) { p.copy(dischargeOverCurrent = it) }
    field("Balance start diff", p.balanceStartDiff) { p.copy(balanceStartDiff = it) }
    Button(onClick = vm::saveParameters) { Text("Save Settings") }
}

@Composable
fun LogsScreen() {
    val logs by AppLogger.logs.collectAsState()
    Screen("Logs") {
        Button(onClick = AppLogger::clear) { Text("Clear") }
        logs.forEach { line -> InfoCard { Text("${line.time} ${line.level.uppercase()}"); Text(line.text) } }
    }
}
