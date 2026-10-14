#!/usr/bin/env python3
"""
Comprueba que cada recurso que el código compartido espera existe en Android e iOS, y que no
sobran recursos huérfanos. Sale con código 1 si algo no cuadra.

    python3 tools/check_assets.py

Dos grupos:
- Casillas del inicio: `HomeService` (sharedLogic)  ->  `service_<nombre en minúsculas>`
- Skins de tarjeta:    `CardSkin`   (sharedLogic)  ->  `card_skin_<nombre en minúsculas>` (GENERIC no tiene imagen)
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SHARED = ROOT / "sharedLogic/src/commonMain/kotlin/org/andrew/enzona/feature"
ANDROID = ROOT / "androidApp/src/main/res/drawable-nodpi"
IOS = ROOT / "iosApp/iosApp/Assets.xcassets"


def enum_names(path: Path, enum: str, end_marker: str) -> list[str]:
    src = path.read_text()
    body = src[src.index(f"enum class {enum}"):]
    body = body[:body.index(end_marker)]
    return re.findall(r"^\s+([A-Z][A-Z0-9_]*)\(", body, re.M)


groups = {
    "service_": ("casillas", [n for n in enum_names(SHARED / "home/domain/HomeService.kt", "HomeService",
                                                    "/** Nombre del recurso")], 14),
    "card_skin_": ("skins", [n for n in enum_names(SHARED / "cards/domain/CardSkin.kt", "CardSkin",
                                                   "/** Nombre del recurso de imagen")
                             if n != "GENERIC"], 7),
}

problems = []
for prefix, (label, names, expected_count) in groups.items():
    expected = {f"{prefix}{n.lower()}" for n in names}
    android = {p.stem for p in ANDROID.glob(f"{prefix}*.png")}
    ios = {p.name.removesuffix(".imageset") for p in IOS.glob(f"{prefix}*.imageset")}
    if len(names) != expected_count:
        problems.append(f"{label}: se esperaban {expected_count} y hay {len(names)} en el código")
    for plat, have in (("Android", android), ("iOS", ios)):
        for m in sorted(expected - have):
            problems.append(f"{plat}: falta {m}")
        for o in sorted(have - expected):
            problems.append(f"{plat}: recurso huérfano {o}")
    for n in sorted(expected & ios):
        if not (IOS / f"{n}.imageset" / f"{n}.png").is_file():
            problems.append(f"iOS: {n}.imageset no contiene {n}.png")
    print(f"{label}: {len(names)} | Android: {len(android)} | iOS: {len(ios)}")

print("OK" if not problems else "\n".join("FALLA: " + p for p in problems))
sys.exit(1 if problems else 0)
