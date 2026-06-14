# Architecture

## Layers

- UI: Jetpack Compose screens in `MainActivity.kt`.
- View model: `AppViewModel`, exposing one immutable `UiState`.
- Repository: `BmsRepository`, switching between mock mode and real BLE.
- BLE service: `BleManager`, handling Android BLE scan, connection, service discovery, notifications, and chunked writes.
- Protocol: `BmsProtocol`, containing UUIDs, CRC16/MODBUS, commands, parsers, and parameter serialization.
- Data: immutable Kotlin data classes in `data/Models.kt`.

## Mock Mode

Mock mode is enabled by default and supplies deterministic dashboard, cell, temperature, switch, and parameter values. It lets the app launch without a BMS device.

## BLE Flow

1. Request BLE permissions.
2. Scan for `BRT_AFE2616` or service prefix `6953FF00`.
3. Connect and discover services/characteristics.
4. Enable notifications.
5. Send command chunks of up to 20 bytes.
6. Assemble notification bytes until the fixed length or dynamic `(byte[2] + 5)` length is reached.
7. Parse the frame through `BmsProtocol`.
