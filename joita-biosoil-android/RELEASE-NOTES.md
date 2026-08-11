# Release 4.0.1 — Direct Soil Reading

- Opens directly to the eight live soil measurements—no home page, field form, or setup wizard.
- Automatically discovers the USB OTG probe and requests Android USB permission.
- Reconnects when a probe is attached and prioritizes the three SoilDetector-compatible VID/PID combinations.
- Shows the connected device VID/PID when a USB device is unsupported, making field diagnosis practical.
- Keeps Save and Share on the same screen; a saved branded PDF can be sent through WhatsApp or any Android share target.
- Keeps manual entry as an easy fallback when hardware is unavailable.
- Adds a one-tap English/Hindi switch on the reading screen.
- Updates the app normally over JOITA version 4.0.0 using the same package and signing key.

Release SHA-256:

- APK: `95b7668b1eea67f0e17c1434817a5cab645a611b1ea85436930765f83194dff1`
- AAB: `ee1e58b2931c3c9b77b42033adb2d80f831f555883ff848ec9948a73f2e9e669`

Physical OTG communication must still be confirmed with the exact probe and cable. If a probe is unsupported, the app now displays its VID/PID for a targeted compatibility update.

## Release 4.0.0 — JOITA BioSeed AI Soil Saathi

First JOITA-branded release.

- Rebuilt as a new Android application with package `ai.joita.biosoil`.
- Added English and Hindi per-app language support.
- Added a new India-first field workflow and JOITA visual system.
- Added offline farmer, field and soil-test records.
- Added corrected USB sensor response parsing and clear hardware recovery states.
- Added manual and clearly labelled sample reading sources.
- Added GPS and camera evidence capture from the phone.
- Added cautious soil scoring, parameter status and next-action guidance.
- Added branded PDF reports and Android sharing.
- Added Android 6 through Android 16 compatibility and a universal APK.
- Added dedicated JOITA release signing for future compatible updates.

Before a large field rollout, test USB connection and readings with the exact JOITA probe, OTG cable and representative Android phone brands.

Release SHA-256:

- APK: `d0815e1f1490636e9469f95613e879267cfdcbf3d75461887b10c7f7cf905eb5`
- AAB: `948cac0a9674b1a7eebd7928892bace9ba5508fcbef00260d262193f918233d5`
