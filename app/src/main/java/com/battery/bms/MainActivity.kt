package com.battery.bms

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.battery.bms.data.AppLogger
import com.battery.bms.data.ParameterSettings
import com.battery.bms.ui.AppViewModel
import com.battery.bms.ui.UiState

private val Ink = Color(0xFF111827)
private val Muted = Color(0xFF667085)
private val Page = Color(0xFFF5F7FB)
private val Panel = Color.White
private val Brand = Color(0xFF0F766E)
private val BrandDark = Color(0xFF0B4F4A)
private val Good = Color(0xFF039855)
private val Warn = Color(0xFFF79009)
private val Danger = Color(0xFFD92D20)

private data class TabItem(val key: String, val label: String)

private val tabs = listOf(
    TabItem("Scan", "Scan"),
    TabItem("Dashboard", "Home"),
    TabItem("Cells", "Cells"),
    TabItem("Controls", "MOS"),
    TabItem("Settings", "Params"),
    TabItem("Logs", "Logs")
)

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

    MaterialTheme(colorScheme = lightColorScheme(primary = Brand, secondary = BrandDark, surface = Panel, background = Page)) {
        Scaffold(
            containerColor = Page,
            bottomBar = {
                NavigationBar(containerColor = Panel, tonalElevation = 0.dp) {
                    tabs.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item.key,
                            onClick = { tab = item.key },
                            icon = { NavDot(selected = tab == item.key) },
                            label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Clip, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Brand,
                                selectedTextColor = Ink,
                                indicatorColor = Color(0xFFE0F2F1),
                                unselectedIconColor = Color(0xFF98A2B3),
                                unselectedTextColor = Color(0xFF667085)
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Surface(Modifier.fillMaxSize().padding(padding), color = Page) {
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
private fun NavDot(selected: Boolean) {
    Box(
        Modifier
            .size(if (selected) 7.dp else 5.dp)
            .clip(CircleShape)
            .background(if (selected) Brand else Color(0xFFD0D5DD))
    )
}

@Composable
fun Screen(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Header(title, subtitle)
        content()
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun Header(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.evtop_logo),
                contentDescription = "EVTOP BMS",
                modifier = Modifier.size(44.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("EVTOP BMS", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(title, color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(subtitle, color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
}

@Composable
fun RowText(label: String, value: String, valueColor: Color = Ink) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Brand, contentColor = Color.White),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SoftStatus(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ScanScreen(state: UiState, vm: AppViewModel) = Screen(
    title = "Device Scan",
    subtitle = if (state.mockMode) "Mock mode is on for screen testing" else "Scanning for nearby BMS devices"
) {
    InfoCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                SectionTitle("Connection Mode")
                Text(if (state.mockMode) "Using sample BMS readings" else "Bluetooth scan enabled", color = Muted, fontSize = 13.sp)
            }
            Switch(
                checked = state.mockMode,
                onCheckedChange = vm::setMockMode,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Brand)
            )
        }
        state.connected?.let { RowText("Connected device", it.name, Good) }
        if (state.message.isNotBlank()) Text(state.message, color = Warn, fontSize = 13.sp)
    }

    state.devices.forEach { device ->
        InfoCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    SectionTitle(device.name)
                    Text(device.id, color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                SoftStatus("RSSI ${device.rssi ?: "n/a"}", Brand)
            }
            PrimaryButton("Connect", onClick = { vm.connect(device) })
        }
    }

    if (state.connected != null) {
        TextButton(onClick = vm::disconnect) { Text("Disconnect", color = Danger, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun DashboardScreen(state: UiState, vm: AppViewModel) = Screen(
    title = "Dashboard",
    subtitle = "Live battery overview and protection state"
) {
    PrimaryButton("Refresh", onClick = vm::refreshAll)
    InfoCard {
        SectionTitle("Battery")
        RowText("SOC", "${state.battery?.socPercent ?: 0}%", Good)
        RowText("Total voltage", "%.2f V".format(state.battery?.totalVoltage ?: 0.0))
        RowText("Current", "%.2f A".format(state.battery?.current ?: 0.0))
        RowText("Cycles", "${state.battery?.cycleCount ?: 0}")
    }
    InfoCard {
        SectionTitle("Protection")
        RowText("Charge MOS", if (state.switches?.chargeMosEnabled == true) "On" else "Off", if (state.switches?.chargeMosEnabled == true) Good else Muted)
        RowText("Discharge MOS", if (state.switches?.dischargeMosEnabled == true) "On" else "Off", if (state.switches?.dischargeMosEnabled == true) Good else Muted)
        RowText("Balance", if (state.switches?.balanceEnabled == true) "On" else "Off", if (state.switches?.balanceEnabled == true) Good else Muted)
        RowText("Battery health", if (state.health?.batteryGood != false) "Good" else "Fault", if (state.health?.batteryGood != false) Good else Danger)
    }
    InfoCard {
        SectionTitle("Temperature")
        RowText("MOS", "%.1f C".format(state.temperatures?.mos ?: 0.0))
        RowText("Balance", "%.1f C".format(state.temperatures?.balance ?: 0.0))
        RowText("T1 / T2", "%.1f / %.1f C".format(state.temperatures?.t1 ?: 0.0, state.temperatures?.t2 ?: 0.0))
    }
}

@Composable
fun CellsScreen(state: UiState) = Screen("Cell Voltages", "Per-cell voltage readings") {
    state.battery?.cellVoltages.orEmpty().forEachIndexed { index, voltage ->
        InfoCard { RowText("Cell ${index + 1}", "%.3f V".format(voltage), if (voltage in 3.0..4.2) Good else Warn) }
    }
}

@Composable
fun ControlsScreen(state: UiState, vm: AppViewModel) = Screen("BMS Controls", "Switch MOS and balance outputs") {
    InfoCard {
        RowText("Charge MOS", if (state.switches?.chargeMosEnabled == true) "On" else "Off")
        Switch(state.switches?.chargeMosEnabled == true, vm::setCharge, colors = SwitchDefaults.colors(checkedTrackColor = Brand))
    }
    InfoCard {
        RowText("Discharge MOS", if (state.switches?.dischargeMosEnabled == true) "On" else "Off")
        Switch(state.switches?.dischargeMosEnabled == true, vm::setDischarge, colors = SwitchDefaults.colors(checkedTrackColor = Brand))
    }
    InfoCard {
        RowText("Balance", if (state.switches?.balanceEnabled == true) "On" else "Off")
        Switch(state.switches?.balanceEnabled == true, vm::setBalance, colors = SwitchDefaults.colors(checkedTrackColor = Brand))
    }
    PrimaryButton("BMS Shutdown", onClick = vm::shutdown)
}

@Composable
fun SettingsScreen(state: UiState, vm: AppViewModel) = Screen("Parameter Settings", "Edit protection thresholds") {
    var p by remember(state.parameters) { mutableStateOf(state.parameters) }

    @Composable
    fun field(label: String, value: Double, update: (Double) -> ParameterSettings) {
        InfoCard {
            SectionTitle(label)
            TextField(
                value = value.toString(),
                onValueChange = { text -> text.toDoubleOrNull()?.let { p = update(it); vm.updateParameters(p) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    field("Single over voltage", p.singleOverVoltage) { p.copy(singleOverVoltage = it) }
    field("Total over voltage", p.totalOverVoltage) { p.copy(totalOverVoltage = it) }
    field("Single under voltage", p.singleUnderVoltage) { p.copy(singleUnderVoltage = it) }
    field("Charge over current", p.chargeOverCurrent) { p.copy(chargeOverCurrent = it) }
    field("Discharge over current", p.dischargeOverCurrent) { p.copy(dischargeOverCurrent = it) }
    field("Balance start diff", p.balanceStartDiff) { p.copy(balanceStartDiff = it) }
    PrimaryButton("Save Settings", onClick = vm::saveParameters)
}

@Composable
fun LogsScreen() {
    val logs by AppLogger.logs.collectAsState()
    Screen("Logs", "BLE commands and app events") {
        PrimaryButton("Clear", onClick = AppLogger::clear)
        logs.forEach { line ->
            InfoCard {
                Text("${line.time}  ${line.level.uppercase()}", color = Brand, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(line.text, color = Ink, fontSize = 14.sp)
            }
        }
    }
}
