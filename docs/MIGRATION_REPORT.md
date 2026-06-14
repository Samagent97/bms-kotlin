# BMS Migration Report

Source reviewed: UniApp/Vue 2 application in the parent workspace, including `App.vue`, `pages.json`, four page modules, `utils/device.js`, `utils/logger.js`, `utils/store.js`, `components/log-viewer/log-viewer.vue`, `manifest.json`, and the existing `BMS.apk` artifact.

## Screens And Navigation

The original app uses a UniApp tab bar with four tabs:

- Battery Status (`pages/battery-status/index`): scans for devices when disconnected, connects/disconnects, displays switch states, health flags, SOC, total voltage, current, max/min cell voltage, cycle count, temperatures, 16 cell voltages, and voltage protection warnings.
- Parameter Settings (`pages/parameter-settings/index`): grouped editable battery, voltage, current, temperature, balance, and delay parameters. Reads all settings on entry and writes the full block on save.
- BMS Control (`pages/bms-control/index`): toggles charge MOS, discharge MOS, balance, and sends BMS shutdown.
- Profile (`pages/profile/index`): reads standard Battery Service characteristic `0x2A19`, shows simple update/reset/about dialogs.

The replacement apps split the voltage grid into its own Cell Voltages screen, while keeping the source command and parser.

## Components

- `log-viewer`: debug-only floating log panel with level filter, copy, clear, and auto-scroll.
- Store: global singleton with `bluetoothDevices` and `connectedDevice`.

## Bluetooth Architecture

The app initializes Bluetooth on app show, requests Android location permissions, scans with duplicates, filters devices by name `BRT_AFE2616` or service prefix `6953FF00`, sorts by RSSI, and stops discovery after a match.

Commands are serialized through a single task queue. Each task ensures connection, discovers services/characteristics, selects a writable characteristic and a notify/indicate characteristic, enables notifications, writes the command in 20-byte chunks, and resolves when the response length is satisfied or after an 8-second timeout.

Preferred write type starts as write without response, then falls back to write with response on failure.

## BLE Services And Characteristics

Known UUIDs:

- Primary service: `6953FF00-0000-1000-8000-00805F9B34FB`
- Write characteristic: `6953FF01-0000-1000-8000-00805F9B34FB`
- Notify characteristic: `6953FF02-0000-1000-8000-00805F9B34FB`
- Profile battery read: service `0000180F-0000-1000-8000-00805F9B34FB`, characteristic `00002A19-0000-1000-8000-00805F9B34FB`

The source can also dynamically choose any service containing both writable and notifiable characteristics, preferring same-family UUID prefixes.

## CRC16/MODBUS

CRC initializes to `0xFFFF`, uses reflected polynomial `0xA001`, processes each byte LSB-first, and appends low byte then high byte.

## Command Protocol

Frame shape:

```text
AA function address_hi address_lo length/value_hi length/value_lo [byte_count data...] crc_lo crc_hi
```

Observed commands:

- Read switch enable states: function `0x01`, address `0001`, length `0003`.
- Read health/switch state flags: function `0x02`, address `1001`, length `0004`.
- Read dashboard values and 16 cells: function `0x04`, address `7531`, length `0018`.
- Read temperatures: function `0x04`, address `7547`, length `0004`.
- Read all parameters: function `0x03`, address `9C41`, length `001D`, fixed response 63 bytes.
- Write all parameters: function `0x10`, address `9C41`, length `001D`, byte count `0x3A`, fixed response 8 bytes.
- Toggle charge MOS: function `0x05`, address `0001`, value `0000/0001`.
- Toggle discharge MOS: function `0x05`, address `0002`, value `0000/0001`.
- Toggle balance: function `0x05`, address `0003`, value `0000/0001`.
- Shutdown BMS: function `0x05`, address `0004`, value `0001`.

## Response Parsing

Dynamic responses use response byte 3 as payload length. Expected total bytes are `payloadLength + 5` (device/function/length + payload + CRC).

Dashboard payload:

- SOC: uint16 percent.
- Total voltage: uint32 millivolts.
- Battery current: uint32 milliamps.
- Max/min cell voltage: uint16 millivolts.
- Cycle count: uint16.
- 16 cell voltages: uint16 millivolts each.

Temperatures are signed two-byte centi-degrees. The high byte `0x80` bit indicates negative; the remaining 15 bits are magnitude.

Parameter block is 58 payload bytes: battery type, series count, voltage thresholds, current thresholds, temperature thresholds, balance thresholds, and protection delays. Voltage/current values are stored as millivolts/milliamps.

## Settings And Controls

The parameter UI edits a local copy and writes the entire settings block on save. Controls verify state after toggling by reading status flags.

## Logging

The source wraps `console.log/info/warn/error/debug`, stores the latest 300 records with timestamps, and exposes a development-only floating viewer. Replacements include in-app logs with clear/filter behavior.

## APK Features And Capabilities

The UniApp manifest requests Bluetooth, Bluetooth Admin, Bluetooth Scan/Connect, location, internet, network state, and Wi-Fi state permissions. The native replacement targets Android BLE directly; the Expo replacement uses a development build because BLE is not supported in Expo Go.
