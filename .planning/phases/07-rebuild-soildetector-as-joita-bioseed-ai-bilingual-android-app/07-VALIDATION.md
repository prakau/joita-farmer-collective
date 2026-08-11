---
phase: 7
slug: rebuild-soildetector-as-joita-bioseed-ai-bilingual-android-app
status: approved
nyquist_compliant: true
wave_0_complete: pending_execution
created: 2026-08-11
---

# Phase 7 — Validation Strategy

> Android validation contract for feedback sampling during implementation.

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 JVM tests + Android Lint + Gradle build + Android SDK inspection tools |
| **Config file** | `joita-biosoil-android/app/build.gradle.kts` (created in Wave 1) |
| **Quick run command** | `cd joita-biosoil-android && ./gradlew testDebugUnitTest --no-daemon` |
| **Full suite command** | `cd joita-biosoil-android && ./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease bundleRelease --no-daemon` |
| **Estimated runtime** | Quick ~30–90 seconds after warm dependency cache; full ~2–6 minutes |

## Sampling Rate

- **After every task commit:** Run the narrowest relevant test class or `./gradlew testDebugUnitTest --no-daemon`.
- **After every plan wave:** Run the full suite command.
- **Before phase verification:** Full suite, APK metadata inspection, and signature verification must be green.
- **Max feedback latency:** 180 seconds for code tasks after dependencies are cached.

## Per-Task Verification Map

| Task ID | Wave | Capability | Test Type | Automated Command | Status |
|---------|------|------------|-----------|-------------------|--------|
| 07-01-01 | 1 | API 36/min 23 project and manifest | build/static | `./gradlew tasks --no-daemon` | ⬜ pending |
| 07-01-02 | 1 | Offline models, SQLite and advisory | JVM unit | `./gradlew testDebugUnitTest --tests '*SoilAdvisorTest' --no-daemon` | ⬜ pending |
| 07-01-03 | 1 | USB command, parser and manager | JVM unit | `./gradlew testDebugUnitTest --tests '*SoilProbeProtocolTest' --no-daemon` | ⬜ pending |
| 07-02-01 | 2 | English/Hindi parity and theme | script/lint | `./scripts/check-locales.sh && ./gradlew lintDebug --no-daemon` | ⬜ pending |
| 07-02-02 | 2 | Navigation, Home and Fields | compile/lint | `./gradlew assembleDebug lintDebug --no-daemon` | ⬜ pending |
| 07-02-03 | 2 | Source wizard, evidence and result | unit/full debug | `./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon` | ⬜ pending |
| 07-02-04 | 2 | History, report and settings | unit/full debug | `./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon` | ⬜ pending |
| 07-03-01 | 3 | JOITA signing key | static/keytool | `test -f release-signing/joita-biosoil-release.jks` | ⬜ pending |
| 07-03-02 | 3 | Signed APK/AAB and metadata | full release | `./scripts/verify-release.sh` | ⬜ pending |
| 07-03-03 | 3 | Install/update documentation | content/hash | `rg -n 'Installation|USB sensor|Signing|Future updates' README.md` | ⬜ pending |

## Wave 0 Requirements

- [ ] Gradle wrapper 8.13 and Android application module.
- [ ] `SoilProbeProtocolTest.kt` with short/split/signed/max frame cases.
- [ ] `SoilAdvisorTest.kt` with score boundaries and disclaimer assertion.
- [ ] `scripts/check-locales.sh` to compare translatable resource keys and reject locale folders other than base/hi.
- [ ] `scripts/verify-release.sh` to inspect app ID, SDK levels, permissions, native libraries, logo hash, and APK signature.

## Manual-Only Verifications

| Behavior | Why Manual | Test Instructions |
|----------|------------|-------------------|
| Physical USB probe connection | Hardware and OEM OTG behavior cannot be emulated reliably | Connect one of the known devices, grant permission, capture stable values, unplug mid-read, retry, compare with reference app |
| Camera evidence | Depends on installed camera implementation | Capture photo, deny permission/unavailable camera path, rotate, reopen report |
| GPS accuracy/fallback | RF/environment and device settings | Capture with fine/coarse/denied permissions and verify timestamp/accuracy/manual fallback |
| Hindi layout and 200% font | Visual readability and clipping | Exercise primary flow at 320dp width and 200% font in both locales |
| PDF/share/WhatsApp/call handoff | Depends on installed target apps | Share via system sheet, test missing WhatsApp/viewer fallback, confirm no automatic send/call |

## Release Inspection Contract

`scripts/verify-release.sh` must fail unless all are true:

- application ID is `ai.joita.biosoil`;
- version name is `4.0.0`, min SDK 23, target SDK 36;
- USB host and camera are optional features;
- no legacy external-storage, direct-call, background-location, package-install, or all-files permissions;
- the APK contains no `lib/*/*.so`, so one APK is CPU-architecture independent;
- `joita_bioseed_logo.png` SHA-256 equals `aaa21d2752a5e434c0101a3fb65202c42525f855fe6ef441ca315d8b0f34125b`;
- `apksigner verify --verbose --print-certs` succeeds.

## Validation Sign-Off

- [x] Every implementation task has an automated verification command or a documented manual hardware reason.
- [x] Sampling continuity: no three consecutive tasks without automated verification.
- [x] Wave 0 requirements are assigned to Plan 07-01/07-02.
- [x] No watch-mode flags.
- [ ] Full release verification completes before handoff.
- [x] `nyquist_compliant: true` set after plan task mapping is finalized.

**Approval:** approved 2026-08-11
