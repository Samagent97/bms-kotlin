# EVTOP BMS Kotlin

Native Android Kotlin replacement for the UniApp BMS application.

## Test On Your Phone

### Android Studio

1. Open this `bms-kotlin` folder in Android Studio.
2. Let Gradle sync finish.
3. Connect your Android phone with USB debugging enabled.
4. Select your phone in Android Studio.
5. Press Run.

The app launches as `EVTOP BMS` in real Bluetooth mode. Android will ask for Bluetooth/Nearby Devices permission; allow it, keep Bluetooth turned on, then scan/connect from the Device Scan tab. The test-data switch is only for checking screens without hardware.

### APK Install

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Features

- Jetpack Compose UI with Device Scan, Dashboard, Cell Voltages, BMS Controls, Parameter Settings, and Logs.
- Real BLE scan mode enabled by default, with optional mock data mode for screen checks.
- Native Android BLE scan/connect/service discovery/notification/write path.
- CRC16/MODBUS command layer.
- Battery, temperature, status, control, and parameter parsers.
- Repository/service/protocol/data model separation.

## Setup

Open this folder in Android Studio, let Gradle sync, then run the `app` configuration.

Command line build:

```bash
./gradlew assembleDebug
```

If a Gradle wrapper is not present on your machine yet, install Gradle or generate one:

```bash
gradle wrapper --gradle-version 8.10.2
./gradlew assembleDebug
```

## APK Build

Debug APK:

```bash
./gradlew assembleDebug
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release APK:

```bash
./gradlew assembleRelease
```

Output:

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

## Notes

The original source project is not modified. See `docs/MIGRATION_REPORT.md` for source analysis and protocol mapping.
Architecture and validation guidance are in `docs/ARCHITECTURE.md` and `docs/TESTING_CHECKLIST.md`.
