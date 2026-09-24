# Joita Farmer Collective

Android-first, offline field-data collection for Joita agricultural programs. This is a usable local-first release for a pilot of roughly 200 farmers, 200 acres and 50 lead farmers; the dashboard reports actual records entered on each phone.

## Product

- Hindi-first farmer registration, field/visit forms, navigation and PDF reports.
- A dedicated six-step Hindi impact assessment matching the supplied `JOITA_CCF_Farmer_Impact_Form_Hindi_FULLPAGE` form: FarmAssist, all eight Soil Saathi readings, BioSynth Nano, climate/CRM outcomes, farmer feedback/training, and photo/consent evidence.
- Per-farmer baseline, follow-up, final and correction assessments; saved assessments are append-only in the app. Draft answers save automatically on this phone. Unknown responses remain blank, and consent is never preselected.
- Material 3 dashboard with farmer, acreage and lead-farmer totals.
- Searchable farmer register and lead-farmer filter.
- Farmer proforma: contact, village, optional GPS coordinates, lead status, tenure, and family/farming notes.
- Farmer-linked fields: acreage, crop, variety, season, sowing date, soil, irrigation, inputs, and boundary/location notes.
- Dated visits: officer, crop stage, observations, pest/disease issues, recommendations, yield/harvest information, notes, and photo.
- Farmer → field → visit history that works without connectivity.
- One-tap PDF export for a farmer profile, every linked field, and complete visit history through Android's Save/Share sheet.
- A faster four-tab workspace: dashboard, farmer register, field journal, and reports.
- Editing for every saved farmer, field, and visit without losing linked history.
- Current-location capture, phone/map actions, camera or gallery evidence, and bounded photo resizing.
- PDF reports saved directly to a chosen phone folder, with field photos included on dedicated pages.
- Farmer registration photos, registering officer and optional officer-recorded acknowledgement with device time.
- Evidence ZIP export containing complete JSON records, a readable PDF, photos, available original images and SHA-256 checksums. Keep this copy somewhere other than the phone as well.

## Setup and Android build

Use Android Studio with JDK 17 and Android SDK 36, or run here:

```bash
./gradlew testDebugUnitTest assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The app uses the distinct package `ai.joita.farmercollective`, supports Android 6.0 (API 23) and newer, and can coexist with Soil Saathi. In Android Studio, open this directory, select `app`, then run on an emulator or USB-debuggable device.

Signed release builds require the ignored JOITA keystore and `keystore.properties`, then use `./gradlew assembleRelease`. Preserve that private key: every future in-place Android update must be signed with the same key. Version 1.1.1 updates the same `ai.joita.farmercollective` installation and retains its local SQLite records.

## Data and storage

`farmers` own zero or more `fields`; each field owns zero or more chronological `visits`. Farmers also own `impact_assessments` (date, officer, stable-key JSON answers, categorized photos, officer-recorded consent time, creation time). Records live in the on-device `joita_collective.db` SQLite database. Schema 3 migrates old databases additively without replacing existing records. Impact drafts live in private `impact_drafts` preferences and are excluded from cloud backup.

Camera or selected photos are validated, resized to a maximum dimension of 1,600 pixels, recompressed, and copied into the app-private `files/field-photos` directory; SQLite stores only their private paths. No broad storage permission is requested. Android device transfer includes the database, drafts and photos, while cloud backup excludes these private records. Uninstalling or clearing app data removes local records and photos.

There is intentionally no pretend sync. Future cloud sync should add stable UUIDs, update/tombstone columns and a durable outbox, then push idempotently to authenticated Supabase/Postgres tables under row-level policies. Photos should upload separately to private object storage. Define conflict behavior and field-staff identity before rollout.

## Registration and evidence

Tap **किसान जोड़ें**, enter name/village/contact/tenure, add a farmer photo, and enter the registering officer. Ask the farmer before photographing them. Read back the profile and tick acknowledgement only after agreement. Editing a profile requires acknowledgement to be recorded again. This records the officer's statement; it is not an electronic signature or a contract.

Open the farmer → **नया प्रभाव आकलन भरें**. Select baseline/follow-up/final/correction and complete sections A–F. Enter the assessment date and officer; all other unknown answers may remain blank. Validate pH 0–14, moisture 0–100%, finite numeric values and real dates; EC, salinity, N/P/K and material quantities require an explicit unit. Use actual measurements, not guessed readings. The app does not calculate CO₂e. Supplementary before/after pump-hour/input fields and comparison-period notes provide context, not automatic causal attribution.

Registration references are local to a phone. Use an agreed project-wide Farmer ID and Plot ID in each assessment when collecting across phones. The app snapshots profile/field references into the assessment; later profile edits do not silently rewrite old assessments. Saved assessments have no edit/delete UI: append a correction naming the older assessment ID. This is an application workflow, not tamper-proof storage.

Assessment photos support baseline, Soil Saathi, demo, follow-up and an optional signed-paper consent photo. The supplied consent wording is displayed in Hindi, with an unchecked officer acknowledgement. A photo or checkbox is not a digital signature. No fingerprint/biometric template is captured. Every assessment, including blanks marked **दर्ज नहीं**, is included in the Hindi PDF and structured ZIP export.

Add fields with acreage, crop/variety/season, sowing date, soil, irrigation, inputs and boundaries. Record visits with date, officer, crop stage, observations, issues, recommendations, harvest/yield, and photos. The climate activity notes can hold activity/date, material quantity and unit, source/batch, application method, plot reference and witness. Keep external receipts, laboratory reports and agreements separately.

Open **रिपोर्ट**, select the farmer, and choose **फोन में PDF सहेजें** or **प्रमाण फ़ोल्डर (.zip) सहेजें**. The archive has records.json (including impactAssessments and Hindi question labels), farmer-report.pdf, photos, metadata and a checksum list. New images preserve original received bytes alongside a bounded display copy. Camera and selected-image sources are distinguished; metadata timestamps use the device clock. Earlier version photos have no retained original/metadata. Missing photo files are explicitly listed in the archive. ZIP import/restore is not implemented.

These are editable local records, not independent verification or carbon certification. Hashes detect later file changes but do not prove the scene or consent. Confirm the actual Climate Collective program's evidence requirements before relying on the export for an audit. GPS capture uses a recent device fix; when one is unavailable, obtain a fix in Maps or enter coordinates manually.

## Verification

Run `./gradlew testDebugUnitTest lintDebug assembleRelease`. On an emulator or connected test device, run `./gradlew connectedDebugAndroidTest` for migration/persistence, original-photo preservation, Hindi impact validation, draft resumption and PDF/evidence exports. Use only the debug app on a test device: the UI test creates a clearly labeled QA farmer in the debug database. Debug and release use different package IDs. CI retains generated sample PDFs and screenshots as build artifacts, not production farmer data.

## One install QR (not farmer or field QR codes)

`release/JOITA-Farmer-Collective-INSTALL-QR.svg` is the single staff-distribution QR. It opens the permanent GitHub Releases destination:

`https://github.com/prakau/joita-farmer-collective/releases/latest`

Field staff scan the QR, open the latest release, download the `.apk`, approve installation from their browser when Android asks, and tap **Install**. Regenerate and test the QR if the repository destination changes. It is solely for app installation; farmers and fields do not receive individual QR codes.

## Tools

Kotlin 2.2, Jetpack Compose Material 3, Android SQLite (`SQLiteOpenHelper`), Android Activity Result APIs, Gradle/Android Gradle Plugin, and Python `qrcode` only for the distribution QR artifact.
