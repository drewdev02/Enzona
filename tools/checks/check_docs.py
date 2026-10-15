#!/usr/bin/env python3
"""
Comprueba la documentación de ingeniería inversa (`docs/reverse-engineering/`). Sale con código 1 si algo no cuadra.

    python3 tools/checks/check_docs.py

1. Todo enlace relativo de un `.md` apunta a un archivo que existe (y, si lleva `#ancla`, a un título que existe).
2. El README de la carpeta enlaza a TODOS los demás documentos (ninguno queda huérfano).
3. Higiene: ninguna ruta absoluta de una máquina (`/Users/...`), ni credenciales (JWT, `Bearer` con token, secretos
   OAuth), ni archivos binarios o demasiado grandes, ni código de las PoC (que no se versiona aquí).
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
DOCS = ROOT / "docs" / "reverse-engineering"
MAX_BYTES = 1_000_000

problems: list[str] = []

files = sorted(p for p in DOCS.rglob("*") if p.is_file())
if not files:
    problems.append(f"{DOCS.relative_to(ROOT)}: no hay documentos")


def slug(heading: str) -> str:
    """Ancla estilo GitHub: minúsculas, sin signos, espacios -> guiones."""
    text = re.sub(r"[`*_]", "", heading).strip().lower()
    text = re.sub(r"[^\w\- ]", "", text, flags=re.UNICODE)
    return text.replace(" ", "-")


def anchors(path: Path) -> set[str]:
    return {slug(m.group(1)) for m in re.finditer(r"^#{1,6}\s+(.+)$", path.read_text(), re.M)}


LINK = re.compile(r"\]\(([^)\s]+)\)")
linked_from_readme: set[Path] = set()
for md in (p for p in files if p.suffix == ".md"):
    for target in LINK.findall(md.read_text()):
        if re.match(r"^(https?:|mailto:)", target):
            continue
        path_part, _, anchor = target.partition("#")
        dest = (md.parent / path_part).resolve() if path_part else md
        if not dest.exists():
            problems.append(f"{md.relative_to(ROOT)}: el enlace «{target}» apunta a algo que no existe")
            continue
        if md.name == "README.md" and md.parent == DOCS:
            linked_from_readme.add(dest)
        if anchor and dest.suffix == ".md" and anchor.lower() not in anchors(dest):
            problems.append(f"{md.relative_to(ROOT)}: el enlace «{target}» apunta a un título que no existe")

readme = DOCS / "README.md"
if readme.exists():
    for doc in files:
        if doc == readme or doc.suffix not in {".md", ".txt"}:
            continue
        if doc.resolve() not in linked_from_readme:
            problems.append(f"{doc.relative_to(ROOT)}: el README de la carpeta no lo enlaza")
else:
    problems.append(f"{readme.relative_to(ROOT)}: falta el README de la carpeta")

HYGIENE = [
    (re.compile(r"/Users/[A-Za-z0-9._-]+/"), "ruta absoluta de una máquina"),
    (re.compile(r"eyJ[A-Za-z0-9_-]{15,}\.[A-Za-z0-9_-]{10,}"), "parece un JWT"),
    (re.compile(r"(?i)bearer\s+[A-Za-z0-9._-]{24,}"), "parece un token Bearer"),
    (re.compile(r"(?i)(client_secret|consumer_secret|api[_-]?key)\"?\s*[:=]\s*[\"'][A-Za-z0-9_-]{8,}"), "parece un secreto"),
    (re.compile(r"-----BEGIN (RSA |EC |OPENSSH |ENCRYPTED )?PRIVATE KEY-----"), "clave PRIVADA"),
]
for doc in files:
    if doc.stat().st_size > MAX_BYTES:
        problems.append(f"{doc.relative_to(ROOT)}: pesa más de {MAX_BYTES // 1000} KB")
    if doc.suffix not in {".md", ".txt"}:
        problems.append(f"{doc.relative_to(ROOT)}: solo se versionan .md y .txt en esta carpeta (¿binario o código de PoC?)")
        continue
    for number, line in enumerate(doc.read_text().splitlines(), 1):
        for pattern, what in HYGIENE:
            if pattern.search(line):
                problems.append(f"{doc.relative_to(ROOT)}:{number}: {what}")

print(f"documentos: {len(files)} | enlaces comprobados desde {sum(1 for p in files if p.suffix == '.md')} .md")
print("OK" if not problems else "\n".join("FALLA: " + p for p in problems))
sys.exit(1 if problems else 0)
