#!/usr/bin/env bash
set -uo pipefail
cd "$(dirname "$0")/.."
mkdir -p app/build/qa
adb install -r app/build/outputs/apk/debug/app-debug.apk || exit 1
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk || exit 1
adb shell am instrument -w -r ai.joita.farmercollective.debug.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/qa/instrumentation.txt
grep -Eq '^OK \([0-9]+ tests?\)' app/build/qa/instrumentation.txt
test_status=$?
adb pull /sdcard/Android/data/ai.joita.farmercollective.debug/files/qa app/build/qa || true
adb shell am force-stop ai.joita.farmercollective.debug
adb shell am start -n ai.joita.farmercollective.debug/ai.joita.biosoil.MainActivity
sleep 3
adb exec-out screencap -p > app/build/qa/dashboard.png
adb logcat -d -t 2000 > app/build/qa/logcat.txt
exit "$test_status"
