#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
english="$project_dir/app/src/main/res/values/strings.xml"
hindi="$project_dir/app/src/main/res/values-hi/strings.xml"

english_keys="$(mktemp)"
hindi_keys="$(mktemp)"
trap 'rm -f "$english_keys" "$hindi_keys"' EXIT

sed -n 's/.*name="\([A-Za-z0-9_]*\)".*/\1/p' "$english" | sort -u > "$english_keys"
sed -n 's/.*name="\([A-Za-z0-9_]*\)".*/\1/p' "$hindi" | sort -u > "$hindi_keys"

if ! diff -u "$english_keys" "$hindi_keys"; then
  echo "English and Hindi string keys do not match." >&2
  exit 1
fi

unexpected_locales="$(find "$project_dir/app/src/main/res" -maxdepth 1 -type d -name 'values-*' ! -name 'values-hi' -print)"
if [[ -n "$unexpected_locales" ]]; then
  echo "Only English and Hindi locale folders are allowed:" >&2
  echo "$unexpected_locales" >&2
  exit 1
fi

echo "Locale verification passed: English and Hindi contain matching keys."

