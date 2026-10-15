#!/bin/sh
# Compila y ejecuta los casos de TransferPresentation.swift sin Xcode ni el framework Kotlin
# (la presentación de iOS no depende de él). Sale con código 1 si algún caso falla.
set -eu
cd "$(dirname "$0")/.."
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
swiftc -o "$OUT/check" \
  tools/swiftcheck/stubs.swift tools/swiftcheck/main.swift \
  iosApp/iosApp/Transfer/TransferPresentation.swift iosApp/iosApp/Transfer/TransferData.swift \
  iosApp/iosApp/Transfer/OwnCardPresentation.swift iosApp/iosApp/Transfer/OwnCardData.swift \
  iosApp/iosApp/Cards/OperationPresentation.swift
"$OUT/check"
