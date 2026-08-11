#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
apk="$project_dir/release/JOITA-BioSeed-AI-Soil-Saathi-4.0.1.apk"
expected_apk_sha="95b7668b1eea67f0e17c1434817a5cab645a611b1ea85436930765f83194dff1"
expected_logo_sha="aaa21d2752a5e434c0101a3fb65202c42525f855fe6ef441ca315d8b0f34125b"

sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/opt/homebrew/share/android-commandlinetools}}"
build_tools="$(find "$sdk_root/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -1)"
aapt2="$build_tools/aapt2"
apksigner="$build_tools/apksigner"

[[ -f "$apk" ]] || { echo "Release APK not found: $apk" >&2; exit 1; }
[[ -x "$aapt2" && -x "$apksigner" ]] || { echo "Android build tools were not found under $sdk_root" >&2; exit 1; }

actual_apk_sha="$(shasum -a 256 "$apk" | awk '{print $1}')"
[[ "$actual_apk_sha" == "$expected_apk_sha" ]] || { echo "APK SHA-256 does not match the release manifest" >&2; exit 1; }

badging="$($aapt2 dump badging "$apk")"
grep -q "package: name='ai.joita.biosoil' versionCode='40001' versionName='4.0.1'" <<< "$badging"
grep -q "minSdkVersion:'23'" <<< "$badging"
grep -q "targetSdkVersion:'36'" <<< "$badging"
grep -q "application-label-hi:'JOITA बायोसीड AI – मिट्टी साथी'" <<< "$badging"
grep -q "uses-feature-not-required: name='android.hardware.camera'" <<< "$badging"
grep -q "uses-feature-not-required: name='android.hardware.usb.host'" <<< "$badging"
grep -q "native-code: 'arm64-v8a' 'armeabi-v7a' 'x86' 'x86_64'" <<< "$badging"

if grep -Eq "READ_EXTERNAL_STORAGE|WRITE_EXTERNAL_STORAGE|CALL_PHONE|ACCESS_BACKGROUND_LOCATION" <<< "$badging"; then
  echo "Release contains a forbidden broad permission." >&2
  exit 1
fi

if [[ -z "${JAVA_HOME:-}" && -d /opt/homebrew/opt/openjdk@17 ]]; then
  export JAVA_HOME=/opt/homebrew/opt/openjdk@17
fi
"$apksigner" verify "$apk" >/dev/null 2>&1

packaged_logo_sha="$(unzip -p "$apk" res/c0.png | shasum -a 256 | awk '{print $1}')"
if [[ "$packaged_logo_sha" != "$expected_logo_sha" ]]; then
  echo "The exact JOITA logo was not found inside the release APK." >&2
  exit 1
fi

echo "Release verification passed: JOITA package, version, SDKs, optional hardware, four common ABIs, logo, permissions, hash and signature."
