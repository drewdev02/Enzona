#!/usr/bin/env python3
"""
Elige y valida el color del texto de cada skin de tarjeta MIDIENDO los píxeles bajo cada texto.

    python3 tools/check_card_skins.py --report   # tabla de contraste de cada candidato por zona
    python3 tools/check_card_skins.py            # verifica los colores declarados en CardSkin.kt

Por qué: cada tarjeta es una imagen distinta (dorada, beige, playa con palmera, verde oscuro...).
Un color de texto fijo no sirve para todas, y a ojo se falla justo en las difíciles.

Geometría (medida sobre la captura del original): la tarjeta mide ancho x (0.55 * ancho) y la
imagen se RECORTA (BoxFit.cover), así que se ve el 95.8% central del ancho de la imagen.
Cada zona es la caja donde se dibuja un texto, en fracciones de la tarjeta.

Sin dependencias: lee PNG con zlib (8 bits, RGB/RGBA/gris/paleta, sin entrelazado).
"""
import argparse
import re
import struct
import sys
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ANDROID = ROOT / "androidApp/src/main/res/drawable-nodpi"
SKIN_KT = ROOT / "sharedLogic/src/commonMain/kotlin/org/andrew/enzona/feature/cards/domain/CardSkin.kt"

CARD_RATIO = 0.55            # alto / ancho de la tarjeta
INK = {"BLACK": (0, 0, 0), "SOFT_BLACK": (0x20, 0x1F, 0x1F), "WHITE": (255, 255, 255)}

# zona -> (u0, u1, v0, v1) en fracciones de la tarjeta, y contraste mínimo exigido (WCAG AA):
# - texto normal (nombre, moneda, vencimiento): 4.5:1
# - número: 3:1, porque es TEXTO GRANDE (>= 24px)
# - icono del menú (no texto): 3:1
ZONES = {
    "number": (0.044, 0.68, 0.46, 0.57, 3.0),
    "name": (0.044, 0.68, 0.72, 0.83, 4.5),
    "info": (0.044, 0.46, 0.84, 0.94, 4.5),
    "menu": (0.915, 0.965, 0.12, 0.24, 3.0),
}
# Campo declarado en CardSkin.kt -> zonas que cubre.
ZONES_OF_FIELD = {"number": ["number"], "text": ["name", "info"], "menu": ["menu"]}
DEFAULT_INSET = 0.044   # = DEFAULT_NUMBER_INSET en CardSkin.kt
NUMBER_WIDTH = 0.636    # ancho que ocupa "**** **** **** 1234" (de 0.044 a 0.68)
SCRIM = 0.35   # opacidad del velo negro tras el icono cuando `menuScrim = true` (debe igualar a la UI)


def read_png(path):
    data = path.read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", f"{path.name}: no es PNG"
    pos, idat, plte = 8, b"", None
    while pos < len(data):
        n, typ = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + n]
        pos += 12 + n
        if typ == b"IHDR":
            w, h, depth, ctype, _, _, interlace = struct.unpack(">IIBBBBB", body)
            assert depth == 8 and interlace == 0, f"{path.name}: solo 8 bits sin entrelazado"
        elif typ == b"PLTE":
            plte = body
        elif typ == b"IDAT":
            idat += body
    chans = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    raw = zlib.decompress(idat)
    stride = w * chans
    rows, prev = [], bytearray(stride)
    p = 0
    for _ in range(h):
        f = raw[p]; line = bytearray(raw[p + 1:p + 1 + stride]); p += 1 + stride
        for i in range(stride):
            a = line[i - chans] if i >= chans else 0
            b = prev[i]
            c = prev[i - chans] if i >= chans else 0
            if f == 1: line[i] = (line[i] + a) & 255
            elif f == 2: line[i] = (line[i] + b) & 255
            elif f == 3: line[i] = (line[i] + ((a + b) >> 1)) & 255
            elif f == 4:
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                pr = a if pa <= pb and pa <= pc else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 255
        rows.append(line); prev = line

    def px(x, y):
        r = rows[y]
        if ctype == 6:
            R, G, B, A = r[x * 4:x * 4 + 4]
            return tuple((c * A + 255 * (255 - A)) // 255 for c in (R, G, B))   # sobre blanco
        if ctype == 2: return tuple(r[x * 3:x * 3 + 3])
        if ctype == 0: return (r[x],) * 3
        if ctype == 4:
            g, A = r[x * 2], r[x * 2 + 1]; v = (g * A + 255 * (255 - A)) // 255; return (v,) * 3
        i = r[x] * 3; return tuple(plte[i:i + 3])
    return w, h, px


def lum(rgb):
    c = [v / 255 for v in rgb]
    c = [v / 12.92 if v <= 0.03928 else ((v + 0.055) / 1.055) ** 2.4 for v in c]
    return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]


def contrast(l1, l2):
    a, b = sorted((l1, l2), reverse=True)
    return (a + 0.05) / (b + 0.05)


def zone_luminances(px, w, h, zone, number_inset=None):
    u0, u1, v0, v1, _ = ZONES[zone]
    if zone == "number" and number_inset is not None:      # el número puede empezar más a la derecha
        u0, u1 = number_inset, number_inset + NUMBER_WIDTH
    crop = (CARD_RATIO and (1 / CARD_RATIO) / (w / h))        # fracción visible del ancho (cover)
    out = []
    for yi in range(8):
        for xi in range(24):
            u = u0 + (u1 - u0) * (xi + 0.5) / 24
            v = v0 + (v1 - v0) * (yi + 0.5) / 8
            x = int((0.5 + (u - 0.5) * crop) * w); y = int(v * h)
            out.append(lum(px(min(x, w - 1), min(y, h - 1))))
    return out


def with_scrim(lums):
    """Luminancia de cada muestra tras un velo negro de opacidad SCRIM (lineal: L' = L * (1 - a))."""
    return [l * (1 - SCRIM) for l in lums]


def stat(lums, ink):
    li = lum(INK[ink]); cs = sorted(contrast(li, l) for l in lums)
    return cs[len(cs) // 10], cs[len(cs) // 2]       # p10 y mediana


def skins():
    return sorted(p.stem.removeprefix("card_skin_") for p in ANDROID.glob("card_skin_*.png"))


def report():
    for key in skins():
        w, h, px = read_png(ANDROID / f"card_skin_{key}.png")
        print(f"\n== {key} ==")
        for zone in ZONES:
            lums = zone_luminances(px, w, h, zone)
            need = ZONES[zone][4]
            cells = []
            for ink in INK:
                p10, med = stat(lums, ink)
                cells.append(f"{ink}: p10={p10:5.2f} med={med:5.2f}{'*' if p10 >= need else ' '}")
            best = max(INK, key=lambda i: stat(lums, i)[0])
            print(f"  {zone:6} (min {need}) " + " | ".join(cells) + f"  -> mejor {best}")


def declared():
    src = SKIN_KT.read_text()
    out = {}
    pat = (r"^\s+([A-Z][A-Z0-9_]*)\(\s*number = CardInk\.(\w+),\s*text = CardInk\.(\w+),\s*"
           r"menu = CardInk\.(\w+)(?:,\s*menuScrim = (true|false))?(?:,\s*numberInset = ([0-9.]+)f)?\s*\)")
    for m in re.finditer(pat, src, re.M):
        out[m.group(1).lower()] = dict(number=m.group(2), text=m.group(3), menu=m.group(4),
                                       scrim=(m.group(5) == "true"),
                                       inset=float(m.group(6)) if m.group(6) else DEFAULT_INSET)
    return out


def verify():
    decl = declared()
    problems = []
    if not decl:
        problems.append(f"no se pudo leer ningún skin de {SKIN_KT.name}")
    for key in skins():
        if key not in decl:
            problems.append(f"{key}: hay imagen pero no está en CardSkin.kt"); continue
        w, h, px = read_png(ANDROID / f"card_skin_{key}.png")
        for field, zones in ZONES_OF_FIELD.items():
            ink = decl[key][field]
            for zone in zones:
                lums = zone_luminances(px, w, h, zone, decl[key]["inset"])
                note = ""
                if field == "menu" and decl[key]["scrim"]:
                    lums, note = with_scrim(lums), " +velo"
                p10, med = stat(lums, ink); need = ZONES[zone][4]
                ok = p10 >= need
                print(f"{'OK ' if ok else 'FALLA'} {key:11} {zone:6} {ink:10}{note:7} p10={p10:5.2f} med={med:5.2f} (min {need})")
                if not ok:
                    problems.append(f"{key}.{zone}: {ink}{note} da p10={p10:.2f} < {need}")
    for k in decl:
        if k != "generic" and k not in skins():
            problems.append(f"{k}: está en CardSkin.kt pero falta la imagen")
    print("\nTODO CUMPLE" if not problems else "\n".join("FALLA: " + p for p in problems))
    return 1 if problems else 0


if __name__ == "__main__":
    ap = argparse.ArgumentParser(); ap.add_argument("--report", action="store_true")
    sys.exit(report() or 0 if ap.parse_args().report else verify())
