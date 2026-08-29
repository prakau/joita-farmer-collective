# Joita Farmer Collective

Android-first, offline field-data collection for Joita agricultural programs. This is a usable local-first release for a pilot of roughly 200 farmers, 200 acres and 50 lead farmers; the dashboard reports actual records entered on each phone.

## Product

- Material 3 dashboard with farmer, acreage and lead-farmer totals.
- Searchable farmer register and lead-farmer filter.
- Farmer proforma: contact, village, optional GPS coordinates, lead status, tenure, and family/farming notes.
- Farmer-linked fields: acreage, crop, variety, season, sowing date, soil, irrigation, inputs, and boundary/location notes.
- Dated visits: officer, crop stage, observations, pest/disease issues, recommendations, yield/harvest information, notes, and photo.
- Farmer → field → visit history that works without connectivity.

## Setup and Android build

Use Android Studio with JDK 17 and Android SDK 36, or run here:

```bash
./gradlew testDebugUnitTest assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The app supports Android 6.0 (API 23) and newer. In Android Studio, open this directory, select `app`, then run on an emulator or USB-debuggable device.

## Data and storage

`farmers` own zero or more `fields`; each field owns zero or more chronological `visits`. Records live in the on-device `joita_collective.db` SQLite database. Selected photos are copied into the app-private `files/field-photos` directory; SQLite stores only their private paths. No broad storage permission is requested. Uninstalling or clearing app data removes local records and photos.

There is intentionally no pretend sync. Future cloud sync should add stable UUIDs, update/tombstone columns and a durable outbox, then push idempotently to authenticated Supabase/Postgres tables under row-level policies. Photos should upload separately to private object storage. Define conflict behavior and field-staff identity before rollout.

## One install QR (not farmer or field QR codes)

`release/JOITA-Farmer-Collective-INSTALL-QR.svg` is the single staff-distribution QR. It currently encodes this explicit placeholder:

`https://REPLACE-BEFORE-RELEASE.example/joita-farmer-collective`

Before release, host an HTTPS landing page or trusted APK/Play link, replace `INSTALL_URL` in `release/generate_install_qr.py`, and rerun the script. Test the QR on two Android phones. It is solely for app installation; farmers and fields do not receive individual QR codes.

## Tools

Kotlin 2.2, Jetpack Compose Material 3, Android SQLite (`SQLiteOpenHelper`), Android Activity Result APIs, Gradle/Android Gradle Plugin, and Python `qrcode` only for the distribution QR artifact.
