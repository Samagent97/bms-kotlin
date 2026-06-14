package com.battery.bms.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogLine(val id: Int, val time: String, val level: String, val text: String)

object AppLogger {
    private val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private var seq = 0
    private val _logs = MutableStateFlow<List<LogLine>>(emptyList())
    val logs: StateFlow<List<LogLine>> = _logs

    fun push(level: String, text: String) {
        _logs.value = (_logs.value + LogLine(++seq, formatter.format(Date()), level, text)).takeLast(300)
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
