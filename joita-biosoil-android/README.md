# JOITA BioSeed AI – Soil Saathi

A new, offline-first Android soil companion made by **JOITA BIOSEED AI** for Indian farmers and field teams. The interface is available only in English and Hindi.

## What is included

- A completely new Jetpack Compose Material 3 interface using JOITA green, cream, turmeric and soil colours.
- Farmer and field profiles with village, district, state, pincode, Khasra/plot number, crop and acre-based area.
- USB OTG soil-probe support at 9600 baud with complete-frame validation for moisture, temperature, EC, pH, nitrogen, phosphorus, potassium and fertility.
- Manual laboratory/meter readings and permanently labelled demonstration readings.
- Four-step workflow: field/source, reading, GPS/photo evidence, review.
- Offline SQLite history—no login, network or cloud account is required.
- India-focused sampling, OTG and agronomy guidance in English and Hindi.
- Local soil-health score, parameter status and cautious next actions with a laboratory/agronomist disclaimer.
- Branded PDF reports and Android share-sheet support.
- Optional phone GPS, camera and USB hardware; all records remain usable without USB.
- Optional USB, camera and location features so installation is not restricted to phones with every sensor.

## Android compatibility

- Package: `ai.joita.biosoil`
- Version: `4.0.0` (`40000`)
- Minimum: Android 6.0 / API 23
- Target and compile SDK: Android 16 / API 36
- One universal APK containing ARM64, ARMv7, x86 and x86-64 support.

The new JOITA package installs alongside the supplied SoilDetector APK. It cannot silently replace the old package because Android requires the same package name and the original developer’s signing key for an in-place update. All future JOITA releases can update this app when they use the private JOITA release keystore stored outside Git in `release-signing/`.

## Release files

- `release/JOITA-BioSeed-AI-Soil-Saathi-4.0.0.apk` — direct installation on Android phones.
- `release/JOITA-BioSeed-AI-Soil-Saathi-4.0.0.aab` — Google Play submission bundle.
- `release/SHA256SUMS.txt` — file integrity hashes.

## Installation

Transfer the APK to the phone, open it from Android’s Files app or another trusted transfer app, allow that app to install unknown apps if prompted, and tap **Install**. The release folder includes `INSTALL.txt` with the same steps. For a development device with USB debugging enabled, use `adb install -r release/JOITA-BioSeed-AI-Soil-Saathi-4.0.0.apk`.

## Languages and offline behavior

English and Hindi are the only packaged app languages. The first screen asks the user to choose one, and it can be changed later under More. Fields, manual/sample readings, history, guidance and reports work without internet. Cloud sync is not configured and no fake sync success is shown.

## Permissions

The app requests location only when the user chooses to add GPS evidence. Camera capture is delegated to the phone’s camera app. Android displays the USB-device consent prompt only after a compatible USB sensor is selected. The manifest does not request legacy storage, background location, direct calling or broad package access.

## Signing and future updates

The release APK and AAB use the dedicated private JOITA 4096-bit RSA signing key. Preserve `release-signing/joita-biosoil-release.jks`, its private credentials file and `keystore.properties` in at least two secure backups. A future APK with a higher version code, the same package and the same key can update version 4.0.0 without losing local app records.

## Build

Use JDK 17 and an Android SDK containing platform 36:

```bash
./gradlew testDebugUnitTest lintRelease assembleRelease bundleRelease
```

Release builds require the ignored `keystore.properties` and JOITA private keystore. Back up both private signing files securely: losing them prevents compatible future updates.

## USB sensor support

The protocol is based on the supplied SoilDetector 3.2.0 application. It sends an 8-byte poll command and accepts only a complete, plausible 19-byte response before displaying values. The original known CH34x-compatible USB IDs are included:

- Vendor `6790`, product `29987`
- Vendor `6790`, product `21795`
- Vendor `6790`, product `21778`

Other drivers recognized by `usb-serial-for-android` are also probed. Physical OTG cable/sensor validation is still required on the target probe model before field rollout.

## Privacy and safety

Farmer, field and soil records are kept in the app’s local database. “Local only — cloud sync is not configured” is shown explicitly; the app does not pretend to upload data. Reports are shared only when the user invokes Android’s share sheet.

Soil scoring is advisory. Major fertilizer or treatment decisions must be confirmed with a soil laboratory or qualified agronomist.
