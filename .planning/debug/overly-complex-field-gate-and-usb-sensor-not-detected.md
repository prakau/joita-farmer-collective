---
status: awaiting_human_verify
trigger: "First real-phone test of JOITA Soil Saathi 4.0.0 shows a huge required Add field dialog that blocks the core reading flow, typing is difficult, and the USB soil sensor is not detected. User wants the app to open directly to reading, require little/no typing, then save and share easily."
created: 2026-08-11T21:06:18+05:30
updated: 2026-08-11T21:29:46+05:30
---

## Current Focus

hypothesis: The confirmed field-gate and USB lifecycle regressions are fixed in code; only real-phone OTG/probe behavior remains unobservable locally.
test: Install the 4.0.1 APK on the original phone, launch it with the probe attached, accept USB access if prompted, and confirm eight live values plus same-screen Save and Share.
expecting: Launch shows only the branded JOITA Soil Saathi reading screen, automatically detects the QinHeng probe, fills all eight tiles, and saves/shares without any field/profile typing.
next_action: Await physical phone and sensor verification from the user/root agent; if detection still fails, capture the displayed VID/PID diagnostic and Android model/OTG state.

## Symptoms

expected: Launch/app Start soil test should lead immediately to a simple sensor reading screen; no required field/profile. Sensor should auto-detect on attach/OTG, show eight readings, and allow one-tap save/share. Optional field/farmer context may be added later.
actual: UI redirects/gates behind Add field form with farmer name, field name, village, district, state and more required on phone; sensor is not detected.
errors: User reports sensor is not detected; screenshot at /Users/meenakshi/Downloads/WhatsApp Image 2026-08-11 at 21.02.53.jpeg shows validation errors on all required form fields, no crash message.
reproduction: Install signed public APK on Android phone, launch, tap Start soil test / add first field; observe required field gate. Connect original soil sensor via OTG; app does not detect/read it.
started: First real-phone test after version 4.0.0 release; it has not yet worked on hardware.

## Eliminated

- hypothesis: The current app uses incorrect known USB VID/PID values or serial settings.
  evidence: Direct comparison with decompiled 3.2.0 shows identical three IDs, 9600/8N1, driver family and poll command.
  timestamp: 2026-08-11T21:24:03+05:30

- hypothesis: FLAG_IMMUTABLE alone prevents UsbManager from returning permission extras.
  evidence: Current official Android USB host guidance explicitly constructs the USB permission PendingIntent with FLAG_IMMUTABLE.
  timestamp: 2026-08-11T21:24:03+05:30

## Evidence

- timestamp: 2026-08-11T21:08:02+05:30
  checked: .planning/debug/knowledge-base.md
  found: No debug knowledge base exists yet.
  implication: There is no known-pattern candidate; investigate directly from the implementation and original APK evidence.

- timestamp: 2026-08-11T21:08:02+05:30
  checked: repository worktree and Android file inventory
  found: The Android app is an untracked workspace subtree, while numerous unrelated user files are modified/untracked. No AGENTS.md was found in the workspace scan.
  implication: Limit edits strictly to joita-biosoil-android and this debug session; do not disturb unrelated work.

- timestamp: 2026-08-11T21:13:41+05:30
  checked: reported phone screenshot
  found: The Add field AlertDialog occupies nearly the full phone height and shows simultaneous validation errors for farmer name, field name, village, district and state, with more inputs below the fold.
  implication: The symptom is a real interaction gate and high typing burden, not merely ambiguous copy.

- timestamp: 2026-08-11T21:13:41+05:30
  checked: JoitaSoilApp.kt, TestFlow.kt and TopLevelScreens.kt navigation/validation paths
  found: Start soil test opens a four-step wizard whose first Continue refuses to proceed when fields.firstOrNull() is null. The only escape sends the user to Fields, where AddFieldDialog requires six text values (farmer, field, village, district, state, crop). Saving also force-unwraps a selected field.
  implication: The field/profile gate is explicitly designed into the core reading and save path; it cannot be bypassed in the shipped UI.

- timestamp: 2026-08-11T21:13:41+05:30
  checked: SoilRepository.kt schema and saveTest implementation
  found: soil_tests.field_id is nullable, SoilTestRecord.fieldId is nullable, and saveTest already omits field_id when null; field_label and crop accept empty strings.
  implication: Storage already supports field-free readings. The UI's mandatory field requirement is unnecessary and can be removed without a database migration.

- timestamp: 2026-08-11T21:13:41+05:30
  checked: HomeScreen.kt and TestWizard USB lifecycle
  found: Home always renders the sensor_not_connected string; it has no USB manager. UsbSoilSensorManager is created only inside TestWizard, never calls connect on creation, and is invoked only by the Reading-step button after source/field selection.
  implication: An attached sensor cannot be detected or reflected on Home, during source selection, or behind the field gate. The user-observed “not detected” state is deterministic even when hardware is attached.

- timestamp: 2026-08-11T21:24:03+05:30
  checked: decompiled SoilDetector 3.2.0 MainActivity, manifest and device_filter.xml
  found: The reference APK uses the same three QinHeng VID/PID pairs (6790/29987, 6790/21795, 6790/21778), the default usb-serial prober, 9600/8N1 and the recovered poll command. Crucially, it calls device enumeration from onResume and again on USB attach.
  implication: The new app's recovered hardware identifiers/protocol match the reference; the principal regression is lifecycle/orchestration, not a different known sensor ID.

- timestamp: 2026-08-11T21:24:03+05:30
  checked: current UsbSoilSensorManager.findDriver
  found: It selects firstOrNull from all default-probed devices and consults the custom known-probe table only when the default list is entirely empty.
  implication: A different attached serial device can shadow the known soil probe, and unsupported attached devices are indistinguishable from no USB device. Prioritized per-device probing and VID/PID diagnostics are warranted.

- timestamp: 2026-08-11T21:24:03+05:30
  checked: Android UsbManager documentation and current PendingIntent usage
  found: Official USB host guidance uses FLAG_IMMUTABLE, and UsbManager documents adding device/grant extras to its callback PendingIntent.
  implication: The immutable permission PendingIntent hypothesis is not supported and is eliminated; retain immutability while making the intent explicit and permission request automatic.

- timestamp: 2026-08-11T21:24:03+05:30
  checked: baseline Gradle unit-test attempt
  found: The shell has no installed Java runtime, so Gradle exits before configuration with “Unable to locate a Java Runtime.” Existing Android build outputs and Gradle caches are present.
  implication: Obtain/use a temporary JDK for post-fix verification; this environment failure does not implicate app code.

- timestamp: 2026-08-11T21:29:46+05:30
  checked: implemented launch and quick-reading UI against both supplied phone screenshots
  found: Fresh launch now bypasses the former first-run language gate, Home dashboard, bottom navigation, field dialog, source chooser and four-step wizard. The top-level branded JOITA Soil Saathi screen starts in USB mode, shows eight two-column pastel metric tiles with placeholders, keeps Save and Share visible as separate actions, offers manual entry only as a secondary link, and includes a one-tap English/Hindi switch.
  implication: The no-typing Reading -> Save/Share workflow is implemented on one screen and matches the explicit latest user direction.

- timestamp: 2026-08-11T21:29:46+05:30
  checked: implemented USB lifecycle and driver matching
  found: TestWizard calls connect on entry; the manager listens separately for attach/detach and permission events, requests access automatically, prioritizes all three recovered known IDs across every attached device, retains the default prober, adds CDC-interface fallback, and exposes VID/PID details for attached unsupported devices.
  implication: Detection no longer depends on field/source navigation or first-driver ordering, and remaining hardware mismatch reports actionable evidence.

- timestamp: 2026-08-11T21:29:46+05:30
  checked: automated verification with JAVA_HOME=/opt/homebrew/opt/openjdk@17 and Android SDK 36
  found: English/Hindi locale parity passed; 8 JVM tests passed (including the three recovered USB IDs); lintDebug, assembleDebug and minified assembleRelease all completed successfully.
  implication: The implementation compiles and passes focused regression/static checks in both debug and release variants.

- timestamp: 2026-08-11T21:29:46+05:30
  checked: generated release APK metadata and signature
  found: app-release.apk reports ai.joita.biosoil, versionCode 40001, versionName 4.0.1, minSdk 23 and targetSdk 36; apksigner verifies v1, v2 and v3 with one signer. Final SHA-256 is 95b7668b1eea67f0e17c1434817a5cab645a611b1ea85436930765f83194dff1.
  implication: The locally generated release artifact is structurally ready for root's signing/redeployment workflow, subject to physical USB verification.

## Resolution

root_cause: The 4.0.0 redesign made an optional field profile a hard prerequisite in TestWizard, so a first-time user cannot reach the reading screen or instantiate/start UsbSoilSensorManager. Unlike the original APK's onResume enumeration, the new manager only connects after field selection, source selection and a manual Take reading tap; Home meanwhile displays a hard-coded disconnected chip. USB selection is also fragile because findDriver chooses only the first default-probed serial device and hides unsupported attached VID/PIDs.
fix: Replaced the mandatory multi-step/profile flow with a top-level Soil Detector quick screen that defaults to USB, auto-connects, always renders eight metric tiles, saves field-free records and shares PDFs without leaving the screen; kept manual entry secondary. Reworked USB handling to enumerate all devices, prioritize recovered probe IDs, add default/CDC fallback, auto-request permission, handle attach/detach with separate receivers and display unsupported VID/PIDs. Bumped app to 4.0.1 (40001).
verification: Locale parity, all 8 JVM tests, lintDebug, assembleDebug and minified assembleRelease passed. APK metadata and v1/v2/v3 signature verified. Physical OTG/probe detection and live values remain awaiting human hardware verification.
files_changed: [joita-biosoil-android/app/build.gradle.kts, joita-biosoil-android/app/src/main/java/ai/joita/biosoil/sensor/UsbSoilSensorManager.kt, joita-biosoil-android/app/src/main/java/ai/joita/biosoil/ui/JoitaSoilApp.kt, joita-biosoil-android/app/src/main/java/ai/joita/biosoil/ui/TestFlow.kt, joita-biosoil-android/app/src/main/res/values/strings.xml, joita-biosoil-android/app/src/main/res/values-hi/strings.xml, joita-biosoil-android/app/src/test/java/ai/joita/biosoil/sensor/SoilProbeUsbCatalogTest.kt]
