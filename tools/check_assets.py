#!/usr/bin/env python3
"""
Comprueba que cada recurso que el código compartido espera existe en Android e iOS, y que no
sobran recursos huérfanos. Sale con código 1 si algo no cuadra.

    python3 tools/check_assets.py

Grupos:
- Casillas del inicio: `HomeService` (sharedLogic)  ->  `service_<nombre en minúsculas>`
- Skins de tarjeta:    `CardSkin`   (sharedLogic)  ->  `card_skin_<nombre en minúsculas>` (GENERIC no tiene imagen)
- Secciones de `tools/assets_manifest.json` (iconos de operación `op_*`, menú de transferir `transfer_option_*`,
  logos de banco `bank_*`, avatar `avatar_*`): existen en ambas plataformas con el mismo sha256 y no sobra ninguna.
- Tablas "tipo -> imagen" de cada plataforma (`OperationKind`, `BankBrand`): cubren todo el enum, son idénticas en
  Android e iOS y solo usan imágenes del manifiesto.
"""
import hashlib
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SHARED = ROOT / "sharedLogic/src/commonMain/kotlin/org/andrew/enzona/feature"
ANDROID = ROOT / "androidApp/src/main/res/drawable-nodpi"
ANDROID_UI = ROOT / "androidApp/src/main/kotlin/org/andrew/enzona/ui"
IOS = ROOT / "iosApp/iosApp/Assets.xcassets"
IOS_UI = ROOT / "iosApp/iosApp"


def enum_names(path: Path, enum: str, end_marker: str) -> list[str]:
    src = path.read_text()
    body = src[src.index(f"enum class {enum}"):]
    body = body[:body.index(end_marker)]
    return re.findall(r"^\s+([A-Z][A-Z0-9_]*)\(", body, re.M)


def enum_entries(path: Path, enum: str) -> set[str]:
    """Entradas de un `enum class` sin parámetros (una por línea)."""
    src = path.read_text()
    body = src[src.index(f"enum class {enum}"):].split("}")[0]
    return set(re.findall(r"^\s+([A-Z][A-Z0-9_]*),?\s*$", body, re.M))


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

# Secciones del manifiesto: no hay enum compartido que diga qué imágenes existen (el dominio no conoce nombres de
# imagen); la fuente de verdad es el manifiesto. Cada entrada existe en Android e iOS con el mismo contenido (sha256)
# y no sobra ninguna.
manifest = json.loads((ROOT / "tools/assets_manifest.json").read_text())
SECTIONS = {
    "operation_icons": ("op_", "operaciones"),
    "transfer_options": ("transfer_option_", "menú de transferir"),
    "bank_logos": ("bank_", "bancos"),
    "avatars": ("avatar_", "avatares"),
}
in_manifest: set[str] = set()
for section, (prefix, label) in SECTIONS.items():
    wanted = {e["resource"]: e["sha256"] for e in manifest.get(section, [])}
    if not wanted:
        problems.append(f"{label}: el manifiesto no trae {section}")
    android_found = {p.stem: p for p in ANDROID.glob(f"{prefix}*.png")}
    ios_found = {p.name.removesuffix(".imageset"): p / p.name.replace(".imageset", ".png")
                 for p in IOS.glob(f"{prefix}*.imageset")}
    for plat, have in (("Android", android_found), ("iOS", ios_found)):
        for name in sorted(set(wanted) - set(have)):
            problems.append(f"{plat}: falta {name}")
        for name in sorted(set(have) - set(wanted)):
            problems.append(f"{plat}: recurso huérfano {name}")
        for name in sorted(set(wanted) & set(have)):
            path = have[name]
            if not path.is_file():
                problems.append(f"{plat}: {name} no contiene su PNG")
            elif hashlib.sha256(path.read_bytes()).hexdigest() != wanted[name]:
                problems.append(f"{plat}: {name} no coincide con el sha256 del manifiesto")
    in_manifest |= set(wanted)
    print(f"{label}: {len(wanted)} | Android: {len(android_found)} | iOS: {len(ios_found)}")


def check_mapping(label: str, enum_file: Path, enum: str, image_prefix: str,
                  android_file: Path, swift_file: Path, swift_table: str, exempt: set[str] = frozenset()):
    """Cada plataforma tiene su tabla `tipo -> imagen`; aquí se comprueba que las dos cubren todo el enum (salvo
    [exempt], que en Android se escribe `-> null` y en iOS no aparece), que dicen lo mismo y que usan imágenes del
    manifiesto."""
    entries = enum_entries(enum_file, enum)
    if not entries:
        problems.append(f"{label}: no se pudo leer {enum}")
        return
    if not android_file.is_file() or not swift_file.is_file():
        problems.append(f"{label}: faltan las tablas de las plataformas ({android_file.name} / {swift_file.name})")
        return
    required = entries - exempt
    android_src = android_file.read_text()
    android_map: dict[str, str] = {}
    pending: list[str] = []
    for m in re.finditer(rf"{enum}\.([A-Z_]+)|R\.drawable\.({image_prefix}\w+)|->\s*(null)\b",
                         android_src[android_src.index("when (this)"):]):
        if m.group(1):
            pending.append(m.group(1))
        elif m.group(2):
            android_map.update({k: m.group(2) for k in pending})
            pending = []
        else:
            pending = []  # `-> null`: ese caso no lleva imagen
    swift_src = swift_file.read_text()
    # El tipo `[String: String]` ya contiene un `]`: se corta en el cierre del literal, no en el primero.
    table = swift_src[swift_src.index("= [", swift_src.index(swift_table)):].split("\n    ]")[0]
    swift_map = dict(re.findall(rf'"([A-Z_]+)":\s*"({image_prefix}\w+)"', table))
    for plat, mapping in (("Android", android_map), ("iOS", swift_map)):
        for k in sorted(required - set(mapping)):
            problems.append(f"{plat}: {enum}.{k} no tiene imagen")
        for k in sorted(set(mapping) - entries):
            problems.append(f"{plat}: la tabla de {label} menciona un valor que no existe ({k})")
        for k in sorted(set(mapping) & exempt):
            problems.append(f"{plat}: {enum}.{k} no debería tener imagen")
        for k, image in sorted(mapping.items()):
            if image not in in_manifest:
                problems.append(f"{plat}: {k} usa {image}, que no está en el manifiesto")
    for k in sorted(required & set(android_map) & set(swift_map)):
        if android_map[k] != swift_map[k]:
            problems.append(f"{k}: Android usa {android_map[k]} e iOS {swift_map[k]}")
    used = set(android_map.values()) | set(swift_map.values())
    for image in sorted({i for i in in_manifest if i.startswith(image_prefix)} - used):
        problems.append(f"{image} está en el manifiesto pero ninguna plataforma lo usa")
    print(f"{label}: {len(required)} | Android: {len(android_map)} | iOS: {len(swift_map)}")


check_mapping(
    "tipos de operación", SHARED / "cards/domain/CardActivity.kt", "OperationKind", "op_",
    ANDROID_UI / "cards/OperationIcons.kt", IOS_UI / "Cards/OperationPresentation.swift", "static let icons",
)

check_mapping(
    "bancos", SHARED / "cards/domain/BankBrand.kt", "BankBrand", "bank_",
    ANDROID_UI / "transfer/BankLogos.kt", IOS_UI / "Transfer/TransferPresentation.swift", "static let bankLogos",
    exempt={"UNKNOWN"},
)

print("OK" if not problems else "\n".join("FALLA: " + p for p in problems))
sys.exit(1 if problems else 0)
