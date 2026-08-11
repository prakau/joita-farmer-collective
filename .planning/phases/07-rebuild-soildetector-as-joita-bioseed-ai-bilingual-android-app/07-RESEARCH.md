---
phase: 7
status: complete
researched: 2026-08-11
---

# Phase 7 Android Implementation Research

## Executive Summary

Build a new native Android application rather than repackaging the supplied APK. Repackaging cannot provide maintainable source, reliable security updates, or a valid in-place update without the original signing key. The new app will use package `ai.joita.biosoil`, version `4.0.0`, and a new JOITA-owned signing key. It can coexist with the reference app and can receive future JOITA updates signed with the same new key.

The app should be a single-activity Compose application with a small dependency surface. Core records are stored in an on-device SQLite database; reports use Android's built-in PDF and sharing APIs; camera capture delegates to the installed camera; location uses framework `LocationManager`; USB serial uses the MIT-licensed `usb-serial-for-android` library. With no backend credentials, synchronization must be a visible local queue/adapter that never claims remote success.

## Standard Stack

| Concern | Pinned choice | Rationale |
|---------|---------------|-----------|
| JDK | 17 | Required/supported by AGP 8.x |
| Gradle | 8.13 wrapper | Required by AGP 8.11 |
| Android Gradle Plugin | 8.11.1 | Stable; supports API 36 without AGP 9 built-in Kotlin migration risk |
| Kotlin + Compose compiler plugin | 2.2.10 | Stable Kotlin 2.x plugin pairing |
| Compile/target SDK | 36 | Android 16 and 2026 Play target requirement |
| Minimum SDK | 23 | Current AndroidX default; covers approximately 99% of Play-active devices |
| Compose | BOM `2026.06.00` + Material 3 | Official stable BOM, code-native accessible UI |
| Activity Compose | 1.12.3 | Stable activity/Compose integration |
| AppCompat | 1.7.1 | Backward-compatible per-app locale API |
| USB serial | `com.github.mik3y:usb-serial-for-android:3.10.0` | Pure Java, MIT, CH34x/CDC/FTDI/Prolific support, no native ABI split |
| Persistence | Framework `SQLiteOpenHelper` | No KSP/Room build complexity; explicit schema/migrations; works offline |
| PDF/share | `android.graphics.pdf.PdfDocument` + `FileProvider` | No external renderer; install-safe and offline |
| Camera | `ActivityResultContracts.TakePicture` | Delegates to phone camera; no heavy CameraX dependency |
| Location | `LocationManager` + runtime coarse/fine permission | Google Play Services is not required on low-end/non-GMS devices |

Repositories: `google()`, `mavenCentral()`, and JitPack only for the pinned USB library. Dependency verification/lockfiles are recommended after the first resolved build.

## Architecture

```text
MainActivity (AppCompatActivity)
  └─ JoitaSoilApp composable
      ├─ AppState / route state
      ├─ Home, Fields, Tests, More top-level screens
      ├─ Test wizard: source → reading → evidence → result/report
      └─ Android integration launchers

SoilRepository
  ├─ SoilDatabaseHelper (farmers, fields, tests, settings, sync queue)
  ├─ SensorReading source labels: USB | MANUAL | SAMPLE
  └─ JSON-free typed cursor mapping

UsbSoilSensorManager
  ├─ Android UsbManager permission and attach/detach handling
  ├─ usb-serial-for-android 9600/8N1 port
  ├─ SoilProbeProtocol command/buffering/parser
  └─ StateFlow-like callback state (unsupported, permission, connecting, live, error)

ReportService
  ├─ advisory scoring/range classification
  ├─ PdfDocument rendering
  └─ FileProvider share intent
```

Keep the project in new top-level folder `joita-biosoil-android/`. The supplied APK stays outside it and is never modified.

## Recovered USB Probe Contract

Reference facts recovered from `SoilDetector_3.2.0_release_20260628_190347.apk`:

- Serial: 9600 baud, 8 data bits, 1 stop bit, no parity.
- Known devices: vendor `6790` (`0x1A86`, QinHeng) and products `29987` (`0x7523`), `21795` (`0x5523`), `21778` (`0x5512`).
- Poll once per second with eight bytes: `00 04 00 00 00 08 F0 XX`, where `XX` is one request nonce byte. Preserve this behavior for compatibility, but isolate it behind `SoilProbeProtocol.pollCommand(nonce)` and document that checksum/nonce semantics came from the reference binary.
- Accept only buffered frames of at least 19 bytes. USB reads can split or coalesce frames, so never assume a callback equals one frame.
- Payload fields:

| Bytes | Value |
|-------|-------|
| 3–4 | moisture, unsigned big-endian / 10, percent |
| 5–6 | temperature, signed 16-bit big-endian / 10, °C |
| 7–8 | electrical conductivity, unsigned big-endian, µS/cm |
| 9–10 | pH, unsigned big-endian / 10 |
| 11–12 | nitrogen, unsigned big-endian, mg/kg |
| 13–14 | phosphorus, unsigned big-endian, mg/kg |
| 15–16 | potassium, unsigned big-endian, mg/kg |
| 17–18 | fertility/salt proxy, unsigned big-endian, mg/kg |

Parser guardrails: reject frames outside plausible broad instrument bounds (moisture 0–100%, temperature -40–85°C, pH 0–14, remaining unsigned values 0–65535); keep last complete frame; label all output `USB`; never silently fall back to sample values. Unit tests must cover signed negative temperature, unsigned `0x7F` bytes (the reference incorrectly maps raw 127 to zero), short buffers, split buffers, and maximum values.

USB host must be declared `<uses-feature android:name="android.hardware.usb.host" android:required="false"/>`. At runtime check `PackageManager.FEATURE_USB_HOST`. Non-OTG devices retain manual/sample/history/report functionality.

## Localization

- Base `values/strings.xml` contains English only; `values-hi/strings.xml` contains Hindi only.
- `res/xml/locales_config.xml` declares `en` and `hi` only.
- Activity extends `AppCompatActivity`; language switch calls `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))`.
- Manifest opts into AndroidX locale persistence for Android 12 and lower.
- No hard-coded user-facing strings in composables, protocol errors, notifications, reports, or accessibility descriptions.
- Hindi text may expand 35%; buttons wrap and no fixed-height text containers are allowed.

## Permissions and Mobile Integrations

| Capability | Manifest/runtime behavior |
|------------|---------------------------|
| USB | No dangerous permission; explicit `UsbManager.requestPermission` PendingIntent; attach receiver not exported |
| GPS | `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`; request only after Capture location; manual fallback |
| Camera | Prefer external camera contract and `FileProvider`; camera hardware optional; explain before launch; no gallery for trusted evidence |
| Network state | `ACCESS_NETWORK_STATE` normal permission; display state only; no backend transmission in this release |
| Phone/WhatsApp | Explicit `ACTION_DIAL` and `ACTION_VIEW`/share intents; never direct-call or send silently |
| Reports | App-scoped files through `FileProvider`; no legacy external-storage permissions |

Use immutable PendingIntents on modern Android. Receivers registered at runtime must specify exported/not-exported behavior for target SDK 36. Do not use cleartext traffic, broad storage permissions, install-package permission, background location, device identifiers, or raw government ID fields.

## Soil Advice Boundary

The app can calculate a transparent demo/advisory score and parameter bands, but it cannot claim certified diagnosis or prescribe agronomic treatment as fact. Store observed values separately from derived classifications and copy. Every report states the reading source and advisory disclaimer. Recommendations are simple, conservative next actions (retest, consult lab/agronomist, consider organic matter, verify crop-specific needs), not a pesticide/fertilizer dosage engine unless a reviewed agronomic data source is later supplied.

## Signing and Updateability

- New package means the APK cannot update the original `com.dacundianzi.tr5z` installation in-place.
- Generate a JOITA release keystore and keep it outside Git through `keystore.properties` and `.gitignore`.
- Sign release with APK Signature Scheme v2/v3 (and v1 for Android 6 compatibility if the build tool enables it).
- Deliver keystore and credentials locally with restrictive permissions and a prominent backup warning; losing it prevents future updates under `ai.joita.biosoil`.
- Build both signed APK and AAB; APK is for direct installation, AAB is for future store upload.

## UI and Logo Handling

Copy the exact 934×433 supplied PNG into `app/src/main/res/drawable-nodpi/joita_bioseed_logo.png` without alteration and record its SHA-256. Use `ContentScale.Fit` on splash and About. Android adaptive icons need a safe-zone mark, so implement a code-native seedling vector using the same green/white brand language while retaining the exact full logo in-app.

## Failure Modes and Mitigations

| Risk | Mitigation |
|------|------------|
| USB permission denied or OEM OTG toggle off | Explicit state, brand-specific OTG help, retry, manual path |
| USB callback fragments payload | Accumulating buffer/parser tests; no parsing under 19 bytes |
| False “live” demo data | Source enum and permanent badge in DB/UI/PDF |
| User expects cloud sync | `Local only — cloud sync not configured`; queue never marks remote success |
| Locale changes lose form state | Saveable wizard state and repository drafts |
| Camera/GPS unavailable | Optional hardware declarations and manual/evidence-missing trust state |
| Old phone or low memory | minSdk 23, one activity, no maps/Play Services/CameraX/Room, no native ABIs |
| Future update cannot be signed | Deliver and back up JOITA keystore/credentials |

## Validation Architecture

### Automated layers

1. JVM unit tests:
   - `SoilProbeProtocolTest`: poll bytes, 19-byte parsing, signed temperature, short/split buffer behavior, source label.
   - `SoilAdvisorTest`: boundary classification, score limits 0–100, disclaimer always present.
   - `HindiResourceParityTest` or resource script: every translatable English key exists in `values-hi` and no extra locale folders ship.
2. Android lint and build:
   - `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease bundleRelease`
3. APK inspection:
   - application ID `ai.joita.biosoil`, min 23, target 36, only approved permissions, USB/camera optional, four supported CPU families implicit because there are no native libraries.
4. Signature verification:
   - `apksigner verify --verbose --print-certs <release.apk>` must report v1/v2/v3 as applicable and one JOITA certificate.
5. Install smoke test when an emulator/device is available:
   - `adb install -r <release.apk>` then launch `ai.joita.biosoil/.MainActivity` and verify no crash.

### Manual/UAT matrix

- Android 6/8/11/13/16 representative devices or emulators where available.
- 320dp and 360dp phones; 600dp tablet; portrait and landscape; 100% and 200% font scale.
- English/Hindi: Home, field form, source chooser, USB states, manual entry, result, PDF, settings.
- Airplane mode: create field/test/report and view history.
- USB: no host, no device, permission denied, compatible device, unplug mid-read, malformed/partial payload.
- Camera/GPS: allowed, denied, permanently denied, unavailable hardware, manual fallback.

### Phase gate

Release is acceptable when unit tests/lint/build succeed, signed APK metadata is correct, the exact supplied logo hash is preserved, manual offline flow completes end-to-end, and no UI claims live sensor/cloud data when those sources are absent.

## Sources

- Android API 36/AGP support: https://developer.android.com/build/releases/about-agp
- AGP 8.11 compatibility: https://developer.android.com/build/releases/agp-8-11-0-release-notes
- 2026 target requirement: https://developer.android.com/google/play/requirements/target-sdk
- Per-app languages: https://developer.android.com/guide/topics/resources/app-languages
- USB host: https://developer.android.com/develop/connectivity/usb/host
- Compose BOM: https://developer.android.com/develop/ui/compose/bom
- USB serial library: https://github.com/mik3y/usb-serial-for-android
