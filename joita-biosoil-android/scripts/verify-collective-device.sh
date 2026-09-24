#!/usr/bin/env bash
set -uo pipefail
cd "$(dirname "$0")/.."
./gradlew connectedDebugAndroidTest
test_status=$?
mkdir -p app/build/qa
adb pull /sdcard/Android/data/ai.joita.farmercollective.debug/files/qa app/build/qa || true
adb shell am force-stop ai.joita.farmercollective.debug
adb shell am start -n ai.joita.farmercollective.debug/ai.joita.biosoil.MainActivity
sleep 3
adb exec-out screencap -p > app/build/qa/dashboard.png
exit "$test_status"
