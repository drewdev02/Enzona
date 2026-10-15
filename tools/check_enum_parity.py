#!/usr/bin/env python3
"""
Comprueba que los enums del dominio (Kotlin) y sus espejos en Swift (`iosApp/iosApp/Domain/EzKinds.swift`) no se
separan, y que iOS no vuelve a comparar nombres de enum en texto. Sale con código 1 si algo no cuadra.

    python3 tools/check_enum_parity.py

Qué comprueba:
1. Cada espejo Swift tiene EXACTAMENTE las mismas entradas que su enum Kotlin (por el valor crudo de cada caso).
2. Cada espejo tiene su conversión desde Kotlin en `EzKinds+Kotlin.swift`, y el espejo declara su `fallback`.
3. Fuera de `EzKinds.swift`, ningún `.swift` de la app escribe un nombre de entrada Kotlin como texto en un `case "X":`,
   un `== "X"` o una clave de diccionario `"X":`, ni hace `switch ... .name`. Un nombre de enum en texto es justo lo
   que dejaba caer una entrada nueva en un `default` sin que nadie se enterara.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FEATURE = ROOT / "sharedLogic/src/commonMain/kotlin/org/andrew/enzona/feature"
IOS_APP = ROOT / "iosApp/iosApp"
KINDS = IOS_APP / "Domain/EzKinds.swift"
KINDS_KOTLIN = IOS_APP / "Domain/EzKinds+Kotlin.swift"

# (espejo Swift, archivo Kotlin, enum Kotlin)
MIRRORS = [
    ("EzTransferDirection", FEATURE / "transfer/domain/TransferSummary.kt", "TransferDirection"),
    ("EzTransferStatus", FEATURE / "transfer/domain/TransferSummary.kt", "TransferStatus"),
    ("EzBankBrand", FEATURE / "cards/domain/BankBrand.kt", "BankBrand"),
    ("EzOperationKind", FEATURE / "cards/domain/CardActivity.kt", "OperationKind"),
    ("EzCardSkin", FEATURE / "cards/domain/CardSkin.kt", "CardSkin"),
    ("EzResultKind", FEATURE / "transfer/domain/TransferSubmission.kt", "TransferResultKind"),
    ("EzRejectionCause", FEATURE / "transfer/domain/TransferSubmission.kt", "RejectionCause"),
    ("EzNotSentReason", FEATURE / "transfer/domain/TransferSubmission.kt", "NotSentReason"),
    ("EzAmountProblem", FEATURE / "transfer/domain/TransferAmountInput.kt", "AmountProblem"),
    ("EzInvalidQuery", FEATURE / "transfer/domain/TransferRecipient.kt", "InvalidQuery"),
]

problems: list[str] = []


def strip_comments(src: str) -> str:
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return re.sub(r"//[^\n]*", "", src)


def kotlin_entries(path: Path, enum: str) -> list[str]:
    """Las entradas de un `enum class`, con o sin parámetros, en una línea o en varias."""
    src = strip_comments(path.read_text())
    match = re.search(rf"enum class {enum}\b", src)
    if not match:
        problems.append(f"{path.name}: no se encontró enum class {enum}")
        return []
    open_brace = src.index("{", match.end())
    entries: list[str] = []
    piece: list[str] = []
    depth = 0  # paréntesis (los parámetros de cada entrada)
    for ch in src[open_brace + 1:]:
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
        if depth == 0 and ch in ",;}":
            text = "".join(piece).strip()
            m = re.match(r"([A-Z][A-Z0-9_]*)\b", text)
            if m:
                entries.append(m.group(1))
            piece = []
            if ch in ";}":
                break
        else:
            piece.append(ch)
    return entries


def swift_cases(swift_enum: str) -> dict[str, str] | None:
    """`case outgoing = "OUTGOING"` de un espejo: nombre del caso -> nombre de la entrada Kotlin."""
    src = KINDS.read_text()
    start = src.find(f"enum {swift_enum}:")
    if start < 0:
        return None
    end = src.find("\n}", start)
    body = src[start:end]
    if "static var fallback" not in body:
        problems.append(f"{swift_enum}: no declara `static var fallback`")
    return dict(re.findall(r'case (\w+) = "([A-Z][A-Z0-9_]*)"', body))


all_kotlin_names: set[str] = set()
for swift_enum, kt_file, kt_enum in MIRRORS:
    kt = kotlin_entries(kt_file, kt_enum)
    all_kotlin_names.update(kt)
    cases = swift_cases(swift_enum)
    if cases is None:
        problems.append(f"{swift_enum}: no existe en EzKinds.swift (espejo de {kt_enum})")
        continue
    swift = set(cases.values())
    for name in sorted(set(kt) - swift):
        problems.append(f"{swift_enum}: Kotlin {kt_enum}.{name} no tiene caso en Swift")
    for name in sorted(swift - set(kt)):
        problems.append(f"{swift_enum}: el caso Swift \"{name}\" no existe en Kotlin {kt_enum}")
    if len(cases.values()) != len(swift):
        problems.append(f"{swift_enum}: hay valores crudos repetidos")
    conversion = re.search(rf"extension {swift_enum} \{{ init\(_ kotlin: {kt_enum}\)", KINDS_KOTLIN.read_text())
    if not conversion:
        problems.append(f"{swift_enum}: falta su conversión desde Kotlin ({kt_enum}) en EzKinds+Kotlin.swift")
    print(f"{swift_enum}: Kotlin {len(kt)} | Swift {len(cases)}")

# 3. Nombres de enum en texto fuera de EzKinds.swift.
# Los valores que no son nombres de enum (monedas "CUP", "USD") no están en el conjunto: solo se vigilan los nombres reales.
names = "|".join(sorted(all_kotlin_names, key=len, reverse=True))
PATTERNS = [
    (re.compile(rf'\bcase\s+"({names})"\s*[:,]'), 'un `case "X":` sobre el nombre de un enum'),
    (re.compile(rf'==\s*"({names})"'), 'una comparación `== "X"` con el nombre de un enum'),
    (re.compile(rf'"({names})"\s*:\s*[\["\w.]'), 'una clave de diccionario con el nombre de un enum'),
    (re.compile(r"\bswitch\b[^{\n]*\.name\b"), "un `switch` sobre `.name`"),
]
scanned = 0
for swift_file in sorted(IOS_APP.rglob("*.swift")):
    if swift_file in (KINDS, KINDS_KOTLIN):
        continue
    scanned += 1
    for number, line in enumerate(swift_file.read_text().splitlines(), 1):
        code = line.split("//")[0]
        for pattern, what in PATTERNS:
            found = pattern.search(code)
            if found:
                problems.append(f"{swift_file.relative_to(ROOT)}:{number}: {what} ({found.group(0).strip()})")
print(f"nombres de enum en texto: {scanned} archivos Swift revisados")

print("OK" if not problems else "\n".join("FALLA: " + p for p in problems))
sys.exit(1 if problems else 0)
