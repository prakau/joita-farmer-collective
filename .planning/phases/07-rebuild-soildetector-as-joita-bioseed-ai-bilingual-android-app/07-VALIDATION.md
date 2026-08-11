---
phase: 7
slug: rebuild-soildetector-as-joita-bioseed-ai-bilingual-android-app
status: draft
nyquist_compliant: false
wave_0_complete: false
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

| Task family | Wave | Capability | Test Type | Automated Command | Status |
|-------------|------|------------|-----------|-------------------|--------|
| Project/bootstrap | 1 | API 36/min 23/build/signing config | build/static | `./gradlew tasks assembleDebug --no-daemon` | ⬜ pending |
| Probe protocol | 1 | USB command and payload parser | JVM unit | `./gradlew testDebugUnitTest --tests '*SoilProbeProtocolTest' --no-daemon` | ⬜ pending |
| Persistence/advice | 1 | Offline records and 0–100 advisory score | JVM unit | `./gradlew testDebugUnitTest --tests '*SoilAdvisorTest' --no-daemon` | ⬜ pending |
| Localization | 2 | English/Hindi parity and locale config | script/resource | `./scripts/check-locales.sh` | ⬜ pending |
| Compose UI | 2 | Complete screen/state implementation | compile/lint | `./gradlew lintDebug assembleDebug --no-daemon` | ⬜ pending |
| Mobile integrations | 2 | USB/GPS/camera/share manifest and source states | unit/static | `./gradlew testDebugUnitTest lintDebug --no-daemon` | ⬜ pending |
| Release packaging | 3 | Signed APK/AAB and correct metadata | build/SDK tools | `./scripts/verify-release.sh` | ⬜ pending |
| Offline E2E | 3 | Field → test → result → report | emulator/manual | `adb install -r <apk>` plus UAT | ⬜ pending |

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

- [ ] Every implementation task has an automated verification command or a documented manual hardware reason.
- [ ] Sampling continuity: no three consecutive tasks without automated verification.
- [ ] Wave 0 creates all missing test infrastructure.
- [ ] No watch-mode flags.
- [ ] Full release verification completes before handoff.
- [ ] `nyquist_compliant: true` set after plan task mapping is finalized.

**Approval:** pending plan finalization
