package com.battery.bms.data

object MockData {
    val device = BmsDevice("mock-bms", "BRT_AFE2616 Mock", -42)
    val switches = SwitchState(balanceEnabled = true, dischargeMosEnabled = true, chargeMosEnabled = true)
    val health = HealthState(batteryGood = true, balanceGood = true, dischargeMosGood = true, chargeMosGood = true)
    val battery = BatterySnapshot(86, 52.71, 12.4, 3.302, 3.288, 128, List(16) { 3.29 + (it % 5) * 0.003 })
    val temps = TemperatureSnapshot(31.2, 29.8, 28.5, 28.9)
    val params = ParameterSettings()
}
