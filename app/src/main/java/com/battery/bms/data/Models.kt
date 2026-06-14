package com.battery.bms.data

data class BmsDevice(val id: String, val name: String, val rssi: Int?)
data class SwitchState(val balanceEnabled: Boolean, val dischargeMosEnabled: Boolean, val chargeMosEnabled: Boolean)
data class HealthState(val batteryGood: Boolean, val balanceGood: Boolean, val dischargeMosGood: Boolean, val chargeMosGood: Boolean)
data class BatterySnapshot(
    val socPercent: Int,
    val totalVoltage: Double,
    val current: Double,
    val maxCellVoltage: Double,
    val minCellVoltage: Double,
    val cycleCount: Int,
    val cellVoltages: List<Double>
)
data class TemperatureSnapshot(val mos: Double, val balance: Double, val t1: Double, val t2: Double)

data class ParameterSettings(
    val batteryType: Int = 2,
    val seriesCount: Int = 16,
    val singleOverVoltage: Double = 4.2,
    val totalOverVoltage: Double = 67.2,
    val singleUnderVoltage: Double = 2.7,
    val totalUnderVoltage: Double = 43.2,
    val chargeOverCurrent: Double = 30.0,
    val dischargeOverCurrent: Double = 100.0,
    val shortCircuitCurrent: Double = 200.0,
    val dischargeOverCurrentAlarm: Double = 80.0,
    val secondDischargeOverCurrent: Double = 150.0,
    val mosTempLimit: Double = 90.0,
    val balanceTempLimit: Double = 90.0,
    val t1TempLimit: Double = 90.0,
    val t2TempLimit: Double = 90.0,
    val balanceLimitVoltage: Double = 4.2,
    val balanceStartVoltage: Double = 2.5,
    val balanceStartDiff: Double = 0.05,
    val balanceEndDiff: Double = 0.01,
    val chargeOverCurrentDelay: Int = 10,
    val dischargeOverCurrentDelay: Int = 10,
    val shortCircuitDelay: Int = 100
)
