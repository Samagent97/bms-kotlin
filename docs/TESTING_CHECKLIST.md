# Testing Checklist

- Launch app in mock mode.
- Confirm all six tabs open: Device Scan, Dashboard, Cell Voltages, BMS Controls, Parameter Settings, Logs.
- Confirm mock dashboard values display.
- Confirm 16 cell voltages display.
- Toggle mock controls and confirm UI state changes.
- Edit parameter fields and save.
- Disable mock mode and grant BLE permissions.
- Confirm scan finds `BRT_AFE2616` by name or `6953FF00` service prefix.
- Connect to a BMS device.
- Verify service and characteristic discovery.
- Verify dashboard command `AA 04 7531 0018` updates values.
- Verify temperature command `AA 04 7547 0004` updates temperatures.
- Verify parameter read/write fixed-length responses.
- Verify control commands change physical MOS/balance states.
- Build debug APK with `./gradlew assembleDebug`.
