---
status: awaiting_human_verify
trigger: "First real-phone test of JOITA Soil Saathi 4.0.0 shows a huge required Add field dialog that blocks the core reading flow, typing is difficult, and the USB soil sensor is not detected. User wants the app to open directly to reading, require little/no typing, then save and share easily."
created: 2026-08-11T21:06:18+05:30
updated: 2026-08-12T21:14:02+05:30
---

## Current Focus

hypothesis: Confirmed root cause: JOITA replaced the working probe callback's length-only acceptance and 0x7F byte normalization with an all-or-nothing plausibility filter and ordinary unsigned conversion, so a sentinel/out-of-range register drops the complete eight-metric response.
test: Self-verification is complete. Install the newly built release on the same OPPO phone, keep OTG enabled, connect the same proven probe/cable, and observe whether all eight values replace placeholders.
expecting: The exact payloads discarded by 4.0.1 are now decoded using SoilDetector 3.2.0 semantics, so readings should appear without any field/profile action.
next_action: Await physical OPPO/probe verification; if readings still do not appear, capture the visible sensor status and attach the phone via adb for raw RX/TX logging.

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

- hypothesis: usb-serial-for-android 3.11 uses incompatible CH34x initialization or read-loop semantics.
  evidence: Decompiled comparison shows identical CH34x control transfers, endpoint selection, 9600/8N1 configuration, max-packet read-buffer sizing and blocking port.read(buffer, 0) callbacks in the working and current libraries.
  timestamp: 2026-08-12T21:01:42+05:30

- hypothesis: The current accumulator's packet-boundary handling prevents the first complete response from being parsed.
  evidence: The target's working IO manager only handles callback chunks of at least 19 bytes. For that same first callback, JOITA also presents bytes 0..18 to parseFrame; the accumulator only differs after parsing/rejection, so it cannot explain the initial loss independently of the parser filter.
  timestamp: 2026-08-12T21:01:42+05:30

- hypothesis: Immediate first poll, sequential nonce, or a 1000ms write timeout prevents every response.
  evidence: Both apps repeat the identical seven fixed command bytes every second; the final byte spans the same 0..255 set (random versus sequential), and delay/timeout do not change transmitted bytes. A 200ms warm-up could lose only the first poll, not all later polls.
  timestamp: 2026-08-12T21:01:42+05:30

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

- timestamp: 2026-08-11T21:50:56+05:30
  checked: 4.0.1 real-phone human verification checkpoint
  found: JOITA 4.0.1 installs and opens directly as intended, but shows no sensor readings/data with the exact original probe. On the same OPPO phone with OTG enabled and the same cable/probe, SoilDetector 3.2.0 still produces readings.
  implication: The simple UI fix is verified, while the USB fix failed. Phone OTG configuration, cable and sensor hardware are controlled working variables; investigation must isolate an implementation/protocol difference between the two apps.

- timestamp: 2026-08-12T20:47:18+05:30
  checked: complete SoilDetector 3.2.0 receive callback versus current SoilProbeProtocol.parseFrame
  found: The working callback accepts every USB callback of at least 19 bytes and unconditionally renders offsets 3..18. JOITA extracts the same offsets but returns null for the entire frame if moisture is above 100.0, temperature is outside -50.0..100.0, or pH is above 14.0.
  implication: JOITA introduced a new all-or-nothing rejection point after bytes are received; a single probe sentinel or legitimate out-of-range raw register produces exactly the reported result—no values on any tile—while 3.2.0 still shows data.

- timestamp: 2026-08-12T20:50:05+05:30
  checked: SoilDetector MainActivity.m3091p and JOITA protocol unit tests
  found: The working app normalizes byte 0x7F to zero for every decoded register byte. JOITA uses ordinary unsigned conversion, and its test suite enshrines rejection of a complete 19-byte frame when pH is 25.5 even though the original renders that frame.
  implication: Current tests only prove the newly invented parser policy; they do not prove compatibility with the physical probe. A 0x7F sentinel in temperature or pH is converted to a benign zero by 3.2.0 but can make JOITA discard all eight values.

- timestamp: 2026-08-12T20:54:28+05:30
  checked: bundled SoilDetector CH34x driver and read manager versus usb-serial-for-android 3.11.0
  found: Both CH34x implementations issue the same claim-interface sequence, endpoint selection, initialization control transfers, 9600-baud setup and 8N1 control byte. Both managers allocate one USB max-packet read buffer, call port.read(buffer, 0) continuously, copy exactly the returned byte count, and deliver each read unchanged to the listener.
  implication: A usb-serial library initialization/read regression is not supported. The first complete USB response reaches each app with the same packet boundary and bytes; their first semantic divergence is JOITA's parser rejection.

- timestamp: 2026-08-12T20:54:28+05:30
  checked: direct Android device availability via adb
  found: adb is installed but reports no attached devices in this workspace session.
  implication: Raw on-phone bytes cannot be captured autonomously now; compatibility must be proven against the exact working APK code and later confirmed on the physical OPPO.

- timestamp: 2026-08-12T21:06:09+05:30
  checked: pre-fix SoilProbeProtocolTest compatibility reproduction
  found: Six tests ran and exactly the three new working-APK compatibility cases failed: complete out-of-range response acceptance, 0x7F sentinel normalization and clearing trailing packet bytes before the next callback.
  implication: The tests reproduce three concrete parser divergences before any production change and will distinguish the compatibility fix from a no-op.

- timestamp: 2026-08-12T21:10:31+05:30
  checked: post-fix focused SoilProbeProtocolTest run
  found: All six protocol tests pass after changing only SoilProbeProtocol.kt, including the three cases that failed before the fix.
  implication: The parser now matches the reference callback for complete frames and sentinel bytes, and trailing bytes from one USB callback do not corrupt the next reading.

- timestamp: 2026-08-12T21:14:02+05:30
  checked: complete Android regression/static/release verification
  found: A forced full unit run passed 12/12 tests (4 advisor, 6 protocol, 2 USB catalog); lintDebug and minified assembleRelease passed. The release APK reports ai.joita.biosoil 4.0.1, minSdk 23, targetSdk 36, verifies v1/v2/v3 with one signer, and has SHA-256 ca074c91e01a2d479f52b5149c9266b53888baf02d8935957e694e635c205f9c.
  implication: The protocol fix is regression-tested and included in a structurally valid release artifact. Only the physical target hardware can close end-to-end verification.

- timestamp: 2026-08-12T21:14:02+05:30
  checked: final owned diff versus concurrent shared-worktree changes
  found: This USB investigation changed only SoilProbeProtocol.kt and SoilProbeProtocolTest.kt. Concurrent edits in SoilAdvisor, ReportService, TestFlow and locale strings remain present and were not modified or reverted by this work.
  implication: The sensor fix is isolated from the separately owned English/Hindi guidance and recommendation work.

## Resolution

root_cause: The UI gate was removed successfully, but JOITA's recovered USB parser is not behavior-compatible with SoilDetector 3.2.0. The working callback accepts every response of at least 19 bytes, maps data byte 0x7F to zero, and renders all eight registers. JOITA instead uses ordinary unsigned bytes and rejects the entire reading when moisture, temperature or pH falls outside an invented plausibility range. One probe sentinel/outlier therefore leaves every tile on a placeholder even though serial bytes arrived. Transport, known VID/PIDs, CH34x initialization, 9600/8N1 and blocking read callbacks match the working APK.
fix: Restored the working callback's byte normalization/length-only parsing contract in SoilProbeProtocol, removed the incompatible all-frame plausibility rejection, and made SoilProbeFrameBuffer discard trailing bytes at each completed USB callback so CRC/residue cannot poison the next response. Added exact failing-then-passing compatibility regression tests.
verification: Automated verification passed: the three new compatibility tests failed before the production fix and pass after it; forced full unit suite passes 12/12; lintDebug and minified assembleRelease pass; the signed release verifies under v1/v2/v3. Physical OPPO/OTG/probe verification is still required because no adb device is attached to this environment.
files_changed: [joita-biosoil-android/app/src/main/java/ai/joita/biosoil/sensor/SoilProbeProtocol.kt, joita-biosoil-android/app/src/test/java/ai/joita/biosoil/sensor/SoilProbeProtocolTest.kt]
