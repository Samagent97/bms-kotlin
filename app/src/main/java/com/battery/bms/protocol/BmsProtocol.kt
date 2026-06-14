package com.battery.bms.protocol

import com.battery.bms.data.BatterySnapshot
import com.battery.bms.data.HealthState
import com.battery.bms.data.ParameterSettings
import com.battery.bms.data.SwitchState
import com.battery.bms.data.TemperatureSnapshot
import java.util.UUID

object BmsProtocol {
    val serviceUuid: UUID = UUID.fromString("6953FF00-0000-1000-8000-00805F9B34FB")
    val writeCharUuid: UUID = UUID.fromString("6953FF01-0000-1000-8000-00805F9B34FB")
    val notifyCharUuid: UUID = UUID.fromString("6953FF02-0000-1000-8000-00805F9B34FB")
    const val targetName = "BRT_AFE2616"
    const val targetServicePrefix = "6953FF00"

    fun crc16Modbus(data: ByteArray): ByteArray {
        var crc = 0xffff
        for (raw in data) {
            crc = crc xor (raw.toInt() and 0xff)
            repeat(8) {
                crc = if ((crc and 1) != 0) (crc ushr 1) xor 0xa001 else crc ushr 1
            }
        }
        return byteArrayOf((crc and 0xff).toByte(), ((crc ushr 8) and 0xff).toByte())
    }

    fun command(functionCode: Int, address: Int, valueOrLength: Int, data: ByteArray = byteArrayOf()): ByteArray {
        val frame = byteArrayOf(
            0xaa.toByte(), functionCode.toByte(),
            ((address ushr 8) and 0xff).toByte(), (address and 0xff).toByte(),
            ((valueOrLength ushr 8) and 0xff).toByte(), (valueOrLength and 0xff).toByte()
        ) + data
        return frame + crc16Modbus(frame)
    }

    fun readSwitches() = command(0x01, 0x0001, 0x0003)
    fun readHealth() = command(0x02, 0x1001, 0x0004)
    fun readDashboard() = command(0x04, 0x7531, 0x0018)
    fun readTemperatures() = command(0x04, 0x7547, 0x0004)
    fun readParameters() = command(0x03, 0x9c41, 0x001d)
    fun toggleCharge(enabled: Boolean) = command(0x05, 0x0001, if (enabled) 1 else 0)
    fun toggleDischarge(enabled: Boolean) = command(0x05, 0x0002, if (enabled) 1 else 0)
    fun toggleBalance(enabled: Boolean) = command(0x05, 0x0003, if (enabled) 1 else 0)
    fun shutdown() = command(0x05, 0x0004, 1)

    private fun ByteArray.u8(offset: Int) = getOrNull(offset)?.toInt()?.and(0xff) ?: 0
    private fun ByteArray.u16(offset: Int) = (u8(offset) shl 8) or u8(offset + 1)
    private fun ByteArray.u32(offset: Int): Long =
        ((u8(offset).toLong() shl 24) or (u8(offset + 1).toLong() shl 16) or (u8(offset + 2).toLong() shl 8) or u8(offset + 3).toLong())

    fun parseSwitches(frame: ByteArray): SwitchState {
        val data = frame.u8(3)
        return SwitchState((data and 0x01) == 1, (data and 0x02) == 2, (data and 0x04) == 4)
    }

    fun parseHealth(frame: ByteArray): HealthState {
        val data = frame.u8(3)
        return HealthState((data and 0x01) == 1, (data and 0x02) == 2, (data and 0x04) == 4, (data and 0x10) == 16)
    }

    fun parseBattery(frame: ByteArray): BatterySnapshot {
        val data = frame.copyOfRange(3, frame.size)
        return BatterySnapshot(
            socPercent = data.u16(0),
            totalVoltage = data.u32(2) / 1000.0,
            current = data.u32(6) / 1000.0,
            maxCellVoltage = data.u16(10) / 1000.0,
            minCellVoltage = data.u16(12) / 1000.0,
            cycleCount = data.u16(14),
            cellVoltages = List(16) { data.u16(16 + it * 2) / 1000.0 }
        )
    }

    private fun parseTemp(data: ByteArray, offset: Int): Double {
        var high = data.u8(offset)
        val negative = (high and 0x80) == 0x80
        if (negative) high = high and 0x7f
        val value = (high shl 8) or data.u8(offset + 1)
        return (if (negative) -value else value) / 100.0
    }

    fun parseTemperatures(frame: ByteArray): TemperatureSnapshot {
        val data = frame.copyOfRange(3, frame.size)
        return TemperatureSnapshot(parseTemp(data, 0), parseTemp(data, 2), parseTemp(data, 4), parseTemp(data, 6))
    }

    fun parseParameters(frame: ByteArray): ParameterSettings {
        val data = frame.copyOfRange(3, frame.size)
        var o = 0
        fun r16(): Int = data.u16(o).also { o += 2 }
        fun r32(): Long = data.u32(o).also { o += 4 }
        return ParameterSettings(
            batteryType = r16(), seriesCount = r16(),
            singleOverVoltage = r16() / 1000.0, totalOverVoltage = r32() / 1000.0,
            singleUnderVoltage = r16() / 1000.0, totalUnderVoltage = r32() / 1000.0,
            chargeOverCurrent = r32() / 1000.0, dischargeOverCurrent = r32() / 1000.0,
            shortCircuitCurrent = r32() / 1000.0, dischargeOverCurrentAlarm = r32() / 1000.0,
            secondDischargeOverCurrent = r32() / 1000.0, mosTempLimit = r16().toDouble(),
            balanceTempLimit = r16().toDouble(), t1TempLimit = r16().toDouble(), t2TempLimit = r16().toDouble(),
            balanceLimitVoltage = r16() / 1000.0, balanceStartVoltage = r16() / 1000.0,
            balanceStartDiff = r16() / 1000.0, balanceEndDiff = r16() / 1000.0,
            chargeOverCurrentDelay = r16(), dischargeOverCurrentDelay = r16(), shortCircuitDelay = r16()
        )
    }

    fun writeParameters(settings: ParameterSettings): ByteArray {
        fun b16(v: Int) = byteArrayOf(((v ushr 8) and 0xff).toByte(), (v and 0xff).toByte())
        fun b32(v: Int) = byteArrayOf(((v ushr 24) and 0xff).toByte(), ((v ushr 16) and 0xff).toByte(), ((v ushr 8) and 0xff).toByte(), (v and 0xff).toByte())
        fun mv(v: Double) = kotlin.math.round(v * 1000).toInt()
        val body = b16(settings.batteryType) + b16(settings.seriesCount) +
            b16(mv(settings.singleOverVoltage)) + b32(mv(settings.totalOverVoltage)) +
            b16(mv(settings.singleUnderVoltage)) + b32(mv(settings.totalUnderVoltage)) +
            b32(mv(settings.chargeOverCurrent)) + b32(mv(settings.dischargeOverCurrent)) +
            b32(mv(settings.shortCircuitCurrent)) + b32(mv(settings.dischargeOverCurrentAlarm)) +
            b32(mv(settings.secondDischargeOverCurrent)) +
            b16(settings.mosTempLimit.toInt()) + b16(settings.balanceTempLimit.toInt()) +
            b16(settings.t1TempLimit.toInt()) + b16(settings.t2TempLimit.toInt()) +
            b16(mv(settings.balanceLimitVoltage)) + b16(mv(settings.balanceStartVoltage)) +
            b16(mv(settings.balanceStartDiff)) + b16(mv(settings.balanceEndDiff)) +
            b16(settings.chargeOverCurrentDelay) + b16(settings.dischargeOverCurrentDelay) + b16(settings.shortCircuitDelay)
        return command(0x10, 0x9c41, 0x001d, byteArrayOf(0x3a) + body)
    }

    fun ByteArray.hex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
