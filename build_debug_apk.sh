#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

./gradlew :app:assembleDebug

INPUT_APK="app/build/outputs/apk/debug/app-debug.apk"
OUTPUT_APK="app/build/outputs/apk/debug/Leo-motors.apk"

if [[ ! -f "$INPUT_APK" ]]; then
  echo "APK debug nao encontrado em: $INPUT_APK" >&2
  exit 1
fi

cp "$INPUT_APK" "$OUTPUT_APK"
echo "APK gerado:"
echo " - $INPUT_APK"
echo " - $OUTPUT_APK"
