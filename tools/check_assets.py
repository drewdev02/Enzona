#!/usr/bin/env python3
"""
Comprueba que cada recurso que el código compartido espera existe en Android e iOS, y que no
sobran recursos huérfanos. Sale con código 1 si algo no cuadra.

    python3 tools/check_assets.py

Tres grupos:
- Casillas del inicio: `HomeService` (sharedLogic)  ->  `service_<nombre en minúsculas>`
- Skins de tarjeta:    `CardSkin`   (sharedLogic)  ->  `card_skin_<nombre en minúsculas>` (GENERIC no tiene imagen)
- Iconos de operación: `op_*` según `tools/assets_manifest.json` (existen en ambas plataformas, mismo sha256)
"""
import hashlib
import json
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

# Iconos de operación: no hay enum compartido (el dominio no conoce nombres de iconos); la fuente de verdad
# es el manifiesto. Cada entrada debe existir en Android e iOS con el mismo contenido (sha256) y no sobrar ninguno.
manifest = json.loads((ROOT / "tools/assets_manifest.json").read_text())
ops = {e["resource"]: e["sha256"] for e in manifest.get("operation_icons", [])}
if not ops:
    problems.append("operaciones: el manifiesto no trae operation_icons")
android_ops = {p.stem: p for p in ANDROID.glob("op_*.png")}
ios_ops = {p.name.removesuffix(".imageset"): p / p.name.replace(".imageset", ".png")
           for p in IOS.glob("op_*.imageset")}
for plat, have in (("Android", android_ops), ("iOS", ios_ops)):
    for name in sorted(set(ops) - set(have)):
        problems.append(f"{plat}: falta {name}")
    for name in sorted(set(have) - set(ops)):
        problems.append(f"{plat}: recurso huérfano {name}")
    for name in sorted(set(ops) & set(have)):
        path = have[name]
        if not path.is_file():
            problems.append(f"{plat}: {name} no contiene su PNG")
        elif hashlib.sha256(path.read_bytes()).hexdigest() != ops[name]:
            problems.append(f"{plat}: {name} no coincide con el sha256 del manifiesto")
print(f"operaciones: {len(ops)} | Android: {len(android_ops)} | iOS: {len(ios_ops)}")

# Mapeo tipo de operación -> icono. Cada plataforma tiene su tabla (el dominio no conoce iconos); aquí se
# comprueba que las dos cubren todos los `OperationKind`, que dicen lo mismo y que usan iconos del manifiesto.
kinds = set(re.findall(r"^\s+([A-Z][A-Z0-9_]*),?\s*$",
                       (lambda s: s[s.index("enum class OperationKind"):].split("}")[0])(
                           (SHARED / "cards/domain/CardActivity.kt").read_text()), re.M))
android_src = (ROOT / "androidApp/src/main/kotlin/org/andrew/enzona/ui/cards/OperationIcons.kt").read_text()
android_map, pending = {}, []
for m in re.finditer(r"OperationKind\.([A-Z_]+)|R\.drawable\.(op_\w+)", android_src[android_src.index("when (this)"):]):
    if m.group(1):
        pending.append(m.group(1))
    else:
        android_map.update({k: m.group(2) for k in pending})
        pending = []
swift_src = (ROOT / "iosApp/iosApp/Cards/OperationPresentation.swift").read_text()
# El tipo `[String: String]` ya contiene un `]`: se corta en el cierre del literal, no en el primero.
swift_table = swift_src[swift_src.index("= [", swift_src.index("static let icons")):].split("\n    ]")[0]
swift_map = dict(re.findall(r'"([A-Z_]+)":\s*"(op_\w+)"', swift_table))
if not kinds:
    problems.append("operaciones: no se pudo leer OperationKind")
for plat, mapping in (("Android", android_map), ("iOS", swift_map)):
    for k in sorted(kinds - set(mapping)):
        problems.append(f"{plat}: OperationKind.{k} no tiene icono")
    for k in sorted(set(mapping) - kinds):
        problems.append(f"{plat}: el mapeo de iconos menciona un tipo que no existe ({k})")
    for k, icon in sorted(mapping.items()):
        if icon not in ops:
            problems.append(f"{plat}: {k} usa {icon}, que no está en el manifiesto")
for k in sorted(kinds & set(android_map) & set(swift_map)):
    if android_map[k] != swift_map[k]:
        problems.append(f"{k}: Android usa {android_map[k]} e iOS {swift_map[k]}")
for icon in sorted(set(ops) - set(android_map.values()) - set(swift_map.values())):
    problems.append(f"{icon} está en el manifiesto pero ninguna plataforma lo usa")
print(f"tipos de operación: {len(kinds)} | Android: {len(android_map)} | iOS: {len(swift_map)}")

print("OK" if not problems else "\n".join("FALLA: " + p for p in problems))
sys.exit(1 if problems else 0)
