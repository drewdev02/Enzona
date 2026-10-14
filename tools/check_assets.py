#!/usr/bin/env python3
"""
Comprueba que cada casilla de HomeService tiene su icono en Android e iOS y que no sobran
recursos `service_*` huérfanos. Sale con código 1 si algo no cuadra.

    python3 tools/check_assets.py
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ENUM = ROOT / "sharedLogic/src/commonMain/kotlin/org/andrew/enzona/feature/home/domain/HomeService.kt"
ANDROID = ROOT / "androidApp/src/main/res/drawable-nodpi"
IOS = ROOT / "iosApp/iosApp/Assets.xcassets"

src = ENUM.read_text()
body = src[src.index("enum class HomeService"):src.index("/** Nombre del recurso")]
names = re.findall(r"^\s+([A-Z][A-Z0-9_]*)\(", body, re.M)
expected = {f"service_{n.lower()}" for n in names}

problems = []
if len(names) != 14:
    problems.append(f"se esperaban 14 casillas y hay {len(names)}")

android = {p.stem for p in ANDROID.glob("service_*.png")}
ios = {p.name.removesuffix(".imageset") for p in IOS.glob("service_*.imageset")}
for label, have in (("Android", android), ("iOS", ios)):
    for m in sorted(expected - have):
        problems.append(f"{label}: falta {m}")
    for o in sorted(have - expected):
        problems.append(f"{label}: recurso huérfano {o}")
for n in sorted(expected & ios):
    if not (IOS / f"{n}.imageset" / f"{n}.png").is_file():
        problems.append(f"iOS: {n}.imageset no contiene {n}.png")

print(f"casillas: {len(names)} | Android: {len(android)} | iOS: {len(ios)}")
print("OK" if not problems else "\n".join("FALLA: " + p for p in problems))
sys.exit(1 if problems else 0)
