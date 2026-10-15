#!/usr/bin/env python3
"""
Extrae del APK original los recursos gráficos que usa la app y los importa a Android e iOS.

    python3 tools/extract_assets.py --apk /ruta/cu.xetid.apk.enzona-v20003.apk

Tres grupos:
- Iconos del inicio (`service_*`): el orden y las etiquetas salen de `assets/json/items.json` DEL
  APK. Se mapea por etiqueta, no por nombre de archivo: en el APK están cruzados (`Bulevar.png`
  es la tienda que la app llama "Mis compras").
- Skins de tarjeta (`card_skin_*`): una imagen completa por banco (`images/card/tarjeta_*.png`).
- Iconos de tipo de operación (`op_*`): los del detalle de tarjeta (`images/iconos/ic_*.png`).

Falla en voz alta si el APK trae una etiqueta nueva, falta un archivo o cambian las medidas.
Copia los PNG TAL CUAL (sin recomprimir), escribe `tools/assets_manifest.json` con el origen y el
sha256 de cada archivo, y es idempotente.

Los recursos son arte de EnZona/XETID y contienen marcas de terceros. Ver el manifiesto.
"""
import argparse
import hashlib
import json
import struct
import sys
import zipfile
from pathlib import Path
from urllib.parse import quote

ROOT = Path(__file__).resolve().parent.parent
PREFIX = "assets/flutter_assets/"
ANDROID = ROOT / "androidApp/src/main/res/drawable-nodpi"
IOS = ROOT / "iosApp/iosApp/Assets.xcassets"

# Etiqueta del APK -> clave estable (la usa HomeService en sharedLogic).
SERVICE_KEYS = {
    "Transferir": "transfer", "Balance": "balance", "Bulevar": "bulevar", "Regalo": "gift",
    "Donar": "donate", "Electricidad": "electricity", "Etecsa": "etecsa", "Impuestos": "taxes",
    "Gas": "gas", "Correos": "mail", "Caja extra": "extra_cash", "Mis compras": "purchases",
    "Crear cuenta": "create_account", "Organizaciones": "organizations",
}

# Clave (= CardSkin en minúsculas) -> imagen del APK. `tarjeta_clasica_ofa.png` existe en el APK
# pero ningún widget la usa, así que no se importa.
CARD_SKINS = {
    "ais": "assets/images/card/tarjeta_ais.png",
    "bandec": "assets/images/card/tarjeta_bandec.png",
    "bandec_usd": "assets/images/card/tarjeta_bandec_usd.png",
    "banmet": "assets/images/card/tarjeta_banmet.png",
    "bicsa": "assets/images/card/tarjeta_bicsa.png",
    "bpa": "assets/images/card/tarjeta_bpa.png",
    "clasica": "assets/images/card/tarjeta_clasica.png",
}
CARD_SIZE = (630, 332)

# Iconos de tipo de operación (pantalla de detalle de tarjeta). La clave la usa la UI de cada plataforma
# para mapear `OperationKind` -> imagen: el dominio no conoce nombres de iconos.
# `op_ez` NO es `images/EZ.png` (70x40, no cuadrado, que usa la app original dentro del círculo): se usa
# `ic_enzona_new.png` (257x257, el mismo círculo azul con las barras EZ) porque aquí va como avatar circular.
OPERATION_ICONS = {
    "op_transfer": "assets/images/iconos/ic_tranferencia.png",  # "tranferencia" (sic) es el nombre real
    "op_mobile": "assets/images/iconos/ic_pago_telefono.png",
    "op_payroll": "assets/images/iconos/ic_nomina_verde.png",
    "op_pos": "assets/images/iconos/ic_pos.png",
    "op_withdrawal": "assets/images/iconos/ic_extraccion.png",
    "op_merchant": "assets/images/iconos/ic_pago_comercio.png",
    "op_ez": "assets/images/iconos/ic_enzona_new.png",
}


def png_size(data: bytes):
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("no es un PNG")
    return struct.unpack(">II", data[16:24])


def read_member(z: zipfile.ZipFile, logical: str):
    """El APK guarda algunos nombres con `%20` literal; prueba ambas formas."""
    for candidate in (PREFIX + logical, PREFIX + quote(logical, safe="/")):
        try:
            return z.read(candidate), candidate
        except KeyError:
            continue
    raise SystemExit(f"ERROR: el APK no contiene {logical!r}")


def write_resource(name: str, data: bytes):
    ANDROID.mkdir(parents=True, exist_ok=True)
    (ANDROID / f"{name}.png").write_bytes(data)
    imageset = IOS / f"{name}.imageset"
    imageset.mkdir(parents=True, exist_ok=True)
    (imageset / f"{name}.png").write_bytes(data)
    (imageset / "Contents.json").write_text(json.dumps({
        "images": [{"filename": f"{name}.png", "idiom": "universal", "scale": "3x"}],
        "info": {"author": "xcode", "version": 1},
    }, indent=2) + "\n")


def entry(name: str, member: str, data: bytes, w: int, h: int, **extra):
    return {"resource": name, "source": member, "width": w, "height": h,
            "sha256": hashlib.sha256(data).hexdigest(), **extra}


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--apk", required=True, type=Path)
    args = ap.parse_args()

    apk_sha = hashlib.sha256(args.apk.read_bytes()).hexdigest()
    services, skins, operation_icons = [], [], []
    with zipfile.ZipFile(args.apk) as z:
        items = json.loads(read_member(z, "assets/json/items.json")[0])
        unknown = [i["name"] for i in items if i["name"] not in SERVICE_KEYS]
        if unknown:
            raise SystemExit(f"ERROR: etiquetas nuevas en el APK sin clave asignada: {unknown}")

        for order, item in enumerate(items):
            key = SERVICE_KEYS[item["name"]]
            data, member = read_member(z, item["image"])
            w, h = png_size(data)
            if w != h:
                raise SystemExit(f"ERROR: {item['image']} no es cuadrado ({w}x{h})")
            if w != 210:
                # Excepción real del APK (PCC.png mide 175): la app original lo escala al
                # mismo tamaño que el resto, así que se verá algo más suave.
                print(f"AVISO: {item['name']} mide {w}x{h} (el resto 210x210)")
            name = f"service_{key}"
            write_resource(name, data)
            services.append(entry(name, member, data, w, h, order=order, label=item["name"], key=key))

        for key, logical in CARD_SKINS.items():
            data, member = read_member(z, logical)
            w, h = png_size(data)
            if (w, h) != CARD_SIZE:
                raise SystemExit(f"ERROR: {logical} mide {w}x{h}, se esperaba {CARD_SIZE}")
            name = f"card_skin_{key}"
            write_resource(name, data)
            skins.append(entry(name, member, data, w, h, key=key))

        for name, logical in OPERATION_ICONS.items():
            data, member = read_member(z, logical)
            w, h = png_size(data)
            if w != h:
                raise SystemExit(f"ERROR: {logical} no es cuadrado ({w}x{h})")
            write_resource(name, data)
            operation_icons.append(entry(name, member, data, w, h, key=name))

    manifest = {
        "origin": "APK oficial de EnZona (cu.xetid.apk.enzona)",
        "apk_sha256": apk_sha,
        "note": "Arte de EnZona/XETID; incluye marcas de terceros (Cupet, ONAT, Etecsa, Correos, PCC, "
                "bancos y la red 'red'). Importado por decisión del titular del proyecto.",
        "services": services,
        "card_skins": skins,
        "operation_icons": operation_icons,
    }
    out = ROOT / "tools/assets_manifest.json"
    out.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n")
    print(f"OK: {len(services)} iconos + {len(skins)} skins + {len(operation_icons)} iconos de operación "
          f"-> Android + iOS ; manifiesto: {out.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
