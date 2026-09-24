# Joita Farmer Collective

Android-first, offline field-data collection for Joita agricultural programs. This is a usable local-first release for a pilot of roughly 200 farmers, 200 acres and 50 lead farmers; the dashboard reports actual records entered on each phone.

## Product

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

Signed release builds require the ignored JOITA keystore and `keystore.properties`, then use `./gradlew assembleRelease`. Preserve that private key: every future in-place Android update must be signed with the same key. Version 1.1.0 updates the same `ai.joita.farmercollective` installation and retains its local SQLite records.

## Data and storage

`farmers` own zero or more `fields`; each field owns zero or more chronological `visits`. Records live in the on-device `joita_collective.db` SQLite database. Camera or selected photos are validated, resized to a maximum dimension of 1,600 pixels, recompressed, and copied into the app-private `files/field-photos` directory; SQLite stores only their private paths. No broad storage permission is requested. Android device transfer includes the database and photos, while cloud backup excludes private field records. Uninstalling or clearing app data removes local records and photos.

There is intentionally no pretend sync. Future cloud sync should add stable UUIDs, update/tombstone columns and a durable outbox, then push idempotently to authenticated Supabase/Postgres tables under row-level policies. Photos should upload separately to private object storage. Define conflict behavior and field-staff identity before rollout.

## Registration and evidence

Tap **Add farmer**, enter name/village/contact/tenure, add a farmer photo, and enter the registering officer. Ask the farmer before photographing them. Read back the profile and tick acknowledgement only after agreement. Editing a profile requires acknowledgement to be recorded again. This records the officer's statement; it is not an electronic signature or a contract.

Add fields with acreage, crop/variety/season, sowing date, soil, irrigation, inputs and boundaries. Record visits with date, officer, crop stage, observations, issues, recommendations, harvest/yield, and photos. The climate activity notes can hold activity/date, material quantity and unit, source/batch, application method, plot reference and witness. Keep external receipts, laboratory reports and agreements separately.

Open **Reports**, select the farmer, and choose **Save PDF to phone** or **Save evidence folder (.zip)**. The archive has records.json, farmer-report.pdf, photos, metadata and a checksum list. New images preserve original received bytes alongside a bounded display copy. Camera and selected-image sources are distinguished; metadata timestamps use the device clock. Earlier version photos have no retained original/metadata. Missing photo files are explicitly listed in the archive. ZIP import/restore is not implemented.

These are editable local records, not independent verification or carbon certification. Hashes detect later file changes but do not prove the scene or consent. Confirm the actual Climate Collective program's evidence requirements before relying on the export for an audit. GPS capture uses a recent device fix; when one is unavailable, obtain a fix in Maps or enter coordinates manually.

## Verification

Run `./gradlew testDebugUnitTest lintDebug assembleRelease`. On an emulator or connected test device, run `./gradlew connectedDebugAndroidTest` for upgrade persistence, original-photo preservation and PDF/evidence export checks. Test data is confined to test databases and test-generated photos.

## One install QR (not farmer or field QR codes)

`release/JOITA-Farmer-Collective-INSTALL-QR.svg` is the single staff-distribution QR. It opens the permanent GitHub Releases destination:

`https://github.com/prakau/joita-farmer-collective/releases/latest`

Field staff scan the QR, open the latest release, download the `.apk`, approve installation from their browser when Android asks, and tap **Install**. Regenerate and test the QR if the repository destination changes. It is solely for app installation; farmers and fields do not receive individual QR codes.

## Tools

Kotlin 2.2, Jetpack Compose Material 3, Android SQLite (`SQLiteOpenHelper`), Android Activity Result APIs, Gradle/Android Gradle Plugin, and Python `qrcode` only for the distribution QR artifact.
