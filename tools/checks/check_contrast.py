#!/usr/bin/env python3
"""
Valida el contraste WCAG de la paleta del sistema de diseño (claro y oscuro).
Texto >= 4.5:1 (AA). Elementos no textuales (bordes de campos, iconos) >= 3:1.
Los valores deben coincidir con EzColors.kt (Android) y EzColors.swift (iOS).

    python3 tools/checks/check_contrast.py          # sale con código 1 si algún par falla
"""
import sys

LIGHT = dict(
    background="FFFFFF", surface="FFFFFF", onSurface="12202C",
    surfaceVariant="F1F5F9", onSurfaceVariant="51606E",
    outline="7A8895", divider="C9D3DC",
    primary="0B6FB5", onPrimary="FFFFFF", primaryContainer="D6EBF8", onPrimaryContainer="0F2A5E",
    success="1B7A43", successContainer="D8F3E3", onSuccessContainer="0E4F2B",
    warning="8A5200", warningContainer="FFEFCC", onWarningContainer="6B3F00",
    error="C62828", onError="FFFFFF", errorContainer="FDE0DE", onErrorContainer="7A1212",
    info="0B6FB5", infoContainer="D6EBF8", onInfoContainer="0F2A5E",
    brandBlue="2196D8", brandNavy="0F2A5E",
)
DARK = dict(
    background="0B1218", surface="111A22", onSurface="E6EDF3",
    surfaceVariant="1C2833", onSurfaceVariant="A9B7C4",
    outline="7F8EA0", divider="2A3744",
    primary="6EC1F2", onPrimary="06263D", primaryContainer="0F4568", onPrimaryContainer="CDE9FA",
    success="5FD08A", successContainer="0F3D24", onSuccessContainer="BDECD0",
    warning="F2B84B", warningContainer="4A3300", onWarningContainer="FFE2A8",
    error="FF8A80", onError="4A0A0A", errorContainer="5C1A1A", onErrorContainer="FFDAD6",
    info="6EC1F2", infoContainer="0F4568", onInfoContainer="CDE9FA",
    brandBlue="2196D8", brandNavy="0F2A5E",
)

def lum(h):
    c = [int(h[i:i+2], 16) / 255 for i in (0, 2, 4)]
    c = [x / 12.92 if x <= 0.03928 else ((x + 0.055) / 1.055) ** 2.4 for x in c]
    return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]

def ratio(a, b):
    la, lb = sorted((lum(a), lum(b)), reverse=True)
    return (la + 0.05) / (lb + 0.05)

# (primer plano, fondo, mínimo, descripción)
PAIRS = [
    ("onSurface", "surface", 4.5, "texto principal"),
    ("onSurface", "surfaceVariant", 4.5, "texto en tarjeta"),
    ("onSurfaceVariant", "surface", 4.5, "texto secundario"),
    ("onSurfaceVariant", "surfaceVariant", 4.5, "texto secundario en tarjeta"),
    ("primary", "surface", 4.5, "enlace / texto de acento"),
    ("onPrimary", "primary", 4.5, "texto de botón primario"),
    ("onPrimaryContainer", "primaryContainer", 4.5, "avatar / chip tonal"),
    ("success", "surface", 4.5, "texto de éxito"),
    ("onSuccessContainer", "successContainer", 4.5, "pastilla de éxito"),
    ("warning", "surface", 4.5, "texto de aviso"),
    ("onWarningContainer", "warningContainer", 4.5, "pastilla de aviso"),
    ("error", "surface", 4.5, "texto de error"),
    ("onError", "error", 4.5, "botón destructivo"),
    ("onErrorContainer", "errorContainer", 4.5, "pastilla de error"),
    ("onInfoContainer", "infoContainer", 4.5, "pastilla informativa"),
    ("outline", "surface", 3.0, "borde de campo (no texto)"),
    ("outline", "surfaceVariant", 3.0, "borde de campo sobre tarjeta"),
]
# Solo claro: la marca no cambia con el tema (son elementos decorativos/iconos).
BRAND = [
    ("FFFFFF", "brandBlue", 3.0, "iconos blancos sobre la píldora"),
    ("brandNavy", "brandBlue", 3.0, "icono seleccionado sobre la píldora"),
]

def run(name, p):
    bad = 0
    print(f"\n== {name} ==")
    for fg, bg, mn, what in PAIRS:
        r = ratio(p[fg], p[bg]); ok = r >= mn; bad += not ok
        print(f"  {'OK ' if ok else 'FALLA'} {r:5.2f}  (min {mn})  {fg}/{bg}  — {what}")
    if name == "CLARO":
        for fg, bg, mn, what in BRAND:
            f = p.get(fg, fg); r = ratio(f, p[bg]); ok = r >= mn; bad += not ok
            print(f"  {'OK ' if ok else 'FALLA'} {r:5.2f}  (min {mn})  {fg}/{bg}  — {what}")
    return bad

if __name__ == "__main__":
    failures = run("CLARO", LIGHT) + run("OSCURO", DARK)
    print("\nREFERENCIA: texto blanco sobre el azul de marca #2196D8 =", f"{ratio('FFFFFF','2196D8'):.2f}:1 (no sirve para texto)")
    print(f"\n{'TODOS LOS PARES CUMPLEN' if not failures else str(failures)+' PAR(ES) FALLAN'}")
    sys.exit(1 if failures else 0)
