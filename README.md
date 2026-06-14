# BMS Kotlin

Native Android Kotlin replacement for the UniApp BMS application.

## Features

- Jetpack Compose UI with Device Scan, Dashboard, Cell Voltages, BMS Controls, Parameter Settings, and Logs.
- Mock data mode enabled by default.
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
