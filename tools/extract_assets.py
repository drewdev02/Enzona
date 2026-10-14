#!/usr/bin/env python3
"""
Extrae los iconos del home desde el APK original y los importa a Android e iOS.

    python3 tools/extract_assets.py --apk /ruta/cu.xetid.apk.enzona-v20003.apk

- Lee `assets/json/items.json` DEL APK: de ahí salen el orden y las etiquetas. Se mapea por
  etiqueta, no por nombre de archivo: en el APK están cruzados (`Bulevar.png` es la tienda
  que la app llama "Mis compras").
- Falla en voz alta si el APK trae una etiqueta nueva o falta un archivo.
- Escribe Android (drawable-nodpi), iOS (imageset 3x) y `tools/assets_manifest.json` con el
  origen y el sha256 de cada archivo. Los PNG se copian TAL CUAL (sin recomprimir).
- Es idempotente: volver a ejecutarlo deja los mismos archivos.

Los iconos son arte de EnZona/XETID y contienen marcas de terceros. Ver el manifiesto.
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

# Etiqueta del APK -> clave estable (la usa HomeService.iconKey en sharedLogic).
LABEL_TO_KEY = {
    "Transferir": "transfer", "Balance": "balance", "Bulevar": "bulevar", "Regalo": "gift",
    "Donar": "donate", "Electricidad": "electricity", "Etecsa": "etecsa", "Impuestos": "taxes",
    "Gas": "gas", "Correos": "mail", "Caja extra": "extra_cash", "Mis compras": "purchases",
    "Crear cuenta": "create_account", "Organizaciones": "organizations",
}


def png_size(data: bytes):
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("no es un PNG")
    return struct.unpack(">II", data[16:24])


def read_member(z: zipfile.ZipFile, logical: str) -> tuple[bytes, str]:
    """El APK guarda algunos nombres con `%20` literal; prueba ambas formas."""
    for candidate in (PREFIX + logical, PREFIX + quote(logical, safe="/")):
        try:
            return z.read(candidate), candidate
        except KeyError:
            continue
    raise SystemExit(f"ERROR: el APK no contiene {logical!r}")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--apk", required=True, type=Path)
    args = ap.parse_args()

    apk_sha = hashlib.sha256(args.apk.read_bytes()).hexdigest()
    entries = []
    with zipfile.ZipFile(args.apk) as z:
        items_raw, _ = read_member(z, "assets/json/items.json")
        items = json.loads(items_raw)
        unknown = [i["name"] for i in items if i["name"] not in LABEL_TO_KEY]
        if unknown:
            raise SystemExit(f"ERROR: etiquetas nuevas en el APK sin clave asignada: {unknown}")

        for order, item in enumerate(items):
            key = LABEL_TO_KEY[item["name"]]
            data, member = read_member(z, item["image"])
            w, h = png_size(data)
            if w != h:
                raise SystemExit(f"ERROR: {item['image']} no es cuadrado ({w}x{h})")
            if w != 210:
                # Excepción real del APK (PCC.png mide 175): la app original lo escala al
                # mismo tamaño que el resto, así que se verá algo más suave.
                print(f"AVISO: {item['name']} mide {w}x{h} (el resto 210x210)")
            name = f"service_{key}"

            android = ROOT / "androidApp/src/main/res/drawable-nodpi" / f"{name}.png"
            android.parent.mkdir(parents=True, exist_ok=True)
            android.write_bytes(data)

            imageset = ROOT / "iosApp/iosApp/Assets.xcassets" / f"{name}.imageset"
            imageset.mkdir(parents=True, exist_ok=True)
            (imageset / f"{name}.png").write_bytes(data)
            (imageset / "Contents.json").write_text(json.dumps({
                "images": [{"filename": f"{name}.png", "idiom": "universal", "scale": "3x"}],
                "info": {"author": "xcode", "version": 1},
            }, indent=2) + "\n")

            entries.append({
                "order": order, "label": item["name"], "key": key, "resource": name,
                "source": member, "width": w, "height": h,
                "sha256": hashlib.sha256(data).hexdigest(),
            })

    manifest = {
        "origin": "APK oficial de EnZona (cu.xetid.apk.enzona)",
        "apk_sha256": apk_sha,
        "note": "Arte de EnZona/XETID; incluye marcas de terceros (Cupet, ONAT, Etecsa, Correos, PCC). "
                "Importado por decisión del titular del proyecto.",
        "items": entries,
    }
    out = ROOT / "tools/assets_manifest.json"
    out.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n")
    print(f"OK: {len(entries)} iconos -> Android + iOS ; manifiesto: {out.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
