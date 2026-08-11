---
phase: 07-rebuild-soildetector-as-joita-bioseed-ai-bilingual-android-app
status: human_needed
automated_status: passed
verified: 2026-08-11
---

# Phase 7 Verification

## Automated result: PASS

- `testDebugUnitTest`: pass, including USB poll/frame/signed-temperature/buffering and advisory tests.
- `lintRelease`: pass with 0 errors.
- `assembleRelease` and `bundleRelease`: pass with R8/resource shrinking.
- Locale parity: English and Hindi keys match; no other locale folder exists.
- APK metadata: `ai.joita.biosoil`, version 4.0.0/40000, min 23, target 36.
- Hardware: camera and USB host are optional; four common ABIs are packaged.
- Permissions: network-state and foreground coarse/fine location only; no legacy storage, direct-call or background-location permission.
- Brand: exact supplied logo SHA-256 found in the APK.
- Signing: one 4096-bit JOITA BIOSEED AI signer; APK v1/v2/v3 verification passes.
- Artifacts: APK `d0815e1f...05eb5`; AAB `948cac0a...33d5`.

## Human verification required

No Android device was attached to the build Mac. Installation/launch, camera/GPS runtime dialogs, English/Hindi visual checks and the exact USB probe/OTG cable must be exercised using `07-HUMAN-UAT.md` before a large field rollout.

