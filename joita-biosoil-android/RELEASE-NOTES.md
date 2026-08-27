# Release 4.2.0 - Exhibition Report + Smart Advisory

- Replaces the weak single-page PDF with a structured field-intelligence report using measured text heights, consistent cards, automatic page breaks, repeating headers, footers and page numbers.
- Adds optional farmer name, father name, village and mobile number directly on the reading screen. These details never block or delay the sensor.
- Adds farmer details to local history and safely migrates existing 4.x records without deleting them.
- Adds a clear decision-readiness assessment: Retest required, Preliminary, Stronger field indication, Manual entry or Demonstration only.
- Separates eight observed indicators, parameter status, priority actions, recommended retesting and important decision limits.
- Adds a more intuitive JOITA Smart Advisory card in English and Hindi while remaining honest that guidance is generated on-device and is not a laboratory prescription.
- Removes duplicate dry-soil advice and prioritises retesting before irrigation or fertiliser decisions.

Release SHA-256:

- APK: `7156f15407cb384531f249f79b63f803d7b5aa008ea03e197cd576eace1c4944`
- AAB: `289f0f0172cd4933f1e5bd9158bc861d3676a00155ffef23f2c648bcf4ce3de9`

## Release 4.1.0 — Reliable Sensor + Farmer Advice

- Restores the exact working SoilDetector 3.2.0 response behavior: the probe’s `0x7F` sentinel byte becomes zero and one unusual register no longer discards all eight measurements.
- Adds regression tests derived from the working APK’s receive callback.
- Shows Low / In range / High on each live parameter.
- Averages up to five recent sensor responses for a steadier same-spot result, with a one-tap reset when moving to a new spot.
- Gives immediate English/Hindi farmer actions for dry soil, moisture, pH, possible salinity and N-P-K indicators.
- Adds an English/Hindi measurement guide translated from the supplied manufacturer manual: soil moisture and preparation, root-zone depth, full probe insertion, stabilization time, repeat sampling and cleaning.
- Adds the same farmer actions to the shared JOITA PDF report.
- Clearly identifies the manufacturer “fertility” value as a soluble-salt indicator, not an official Indian soil-fertility grade.
- Avoids invented fertiliser quantities; crop/area-specific doses must use a recognised soil test or Soil Health Card and qualified advice.

Release SHA-256:

- APK: `26ee649622fb2d8615756cb4d81c531d62ff06f4510c48f3ff225be0550b2632`
- AAB: `b1bdbe3699cd6cb57d98e7377143c8c162e88c0642f695ef8892100445231184`

Physical verification remains required on the same OPPO phone, OTG cable and probe because no Android hardware is connected to this build machine.

## Release 4.0.1 — Direct Soil Reading

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
