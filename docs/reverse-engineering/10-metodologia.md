# 10 — Metodología: cómo se extrajo la información del APK

> Guía reproducible para otros agentes/analistas. Describe, paso a paso, las herramientas y
> comandos usados para pasar de un `.apk` de Flutter a la documentación de arquitectura, API,
> modelos y seguridad de este repo. Entorno de referencia: macOS (Apple Silicon), pero los
> pasos son portables (Linux es el entorno más cómodo para Blutter).

## 0. Resumen del pipeline

```
APK (Flutter)
 ├─ unzip ............................ estructura, ABIs, assets
 ├─ strings libapp.so ................ endpoints, hosts, OAuth, package: URIs (triage rápido)
 ├─ parse AndroidManifest ............ permisos, componentes
 ├─ Blutter (Dart AOT → pseudo-src) .. clases, campos JSON reales, datasources, flujo cripto
 └─ (opcional) PoC contra el backend .. validar con una cuenta propia, solo lectura (su código NO está en este repo)
```

Dos niveles de confianza:
- **Triage con `strings`** (rápido, pero solo da literales sueltos).
- **Decompilación con Blutter** (lenta de montar, pero da estructura real: clases, campos,
  qué endpoint llama cada módulo). Es la que produce los esquemas exactos.

## 1. Prerrequisitos

- **Node.js** compatible con REA (`^22.19`, `^24.11`, o `>=26`). Gestiónalo con `fnm`/`nvm`.
- **REA** (opcional, para triage asistido): `npm i -g rea-agents` → `rea setup`.
- **Blutter** (clave para Flutter): https://github.com/worawit/blutter
- Toolchain de Blutter: `python3`, `cmake`, `ninja`, `pkg-config`, `icu4c`, `capstone`,
  y los módulos Python `pyelftools` y `requests`. Compilador C++20 (Clang≥16 / g++≥13).
- Utilidades estándar: `unzip`, `strings`, `python3`.

```bash
# macOS
brew install cmake ninja pkg-config icu4c capstone
pip3 install pyelftools requests
# Linux (Debian/Ubuntu con gcc>=13)
apt install python3-pyelftools python3-requests git cmake ninja-build \
    build-essential pkg-config libicu-dev libcapstone-dev
```

> ⚠️ **Higiene con artefactos no confiables**: extrae el APK en un directorio **vacío y
> aislado**, separado de tus scripts. No ejecutes intérpretes/builds *dentro* de ese
> directorio (un `json.py` plantado se cargaría con `import json`). Pasa rutas como
> argumentos. Nunca ejecutes binarios extraídos del APK.

## 2. Extraer y reconocer el APK

```bash
mkdir -p /ruta/aislada/extracted && cd /ruta/aislada/extracted
unzip -oq /ruta/al/app.apk

# ¿Es Flutter? Busca libapp.so + libflutter.so
unzip -l /ruta/al/app.apk | grep -E "libapp.so|libflutter|classes.*dex|AndroidManifest"
ls lib/            # ABIs: arm64-v8a, armeabi-v7a, x86_64
```

- `lib/<abi>/libapp.so` = snapshot Dart AOT (la lógica de la app).
- `lib/<abi>/libflutter.so` = engine.
- `classes.dex` = wrapper Android (normalmente genérico, poca señal de negocio).
- `assets/flutter_assets/` = assets, incluidos a veces **claves/certificados** (`.pem`, `.crt`).

## 3. Permisos y manifest

El `AndroidManifest.xml` del APK es binario. Para leer permisos rápido:
```bash
python3 - <<'EOF'
import re
d = open('AndroidManifest.xml','rb').read().decode('latin-1','ignore')
print('\n'.join(sorted(set(re.findall(r'android\.permission\.[A-Z_]+', d)))))
EOF
```
(Alternativas: `aapt2 dump badging app.apk`, o `apktool d` / REA `decode-android-resources`.)

## 4. Triage rápido con `strings` sobre `libapp.so`

Da una primera foto sin compilar nada. Trabaja sobre la ABI arm64.

```bash
L=lib/arm64-v8a/libapp.so

# Endpoints y hosts
strings -n 6 "$L" | grep -ioE "https?://[a-zA-Z0-9./:_-]+" | sort -u

# Rutas de API bajo un host concreto
strings -n 6 "$L" | grep -oE "https://api\.ejemplo\.net/[a-zA-Z0-9._/{}-]+" | sort -u

# Inventario de dependencias Dart y del propio código (package: URIs)
strings -n 8 "$L" | grep -oE "package:[a-zA-Z0-9_]+/" | sort -u          # librerías
strings -n 8 "$L" | grep -oE "package:<APP_PKG>/[a-zA-Z0-9_/]+\.dart" | sort -u > files.txt

# Estructura interna del código propio (módulos, use cases, vistas, modelos)
sed -E 's#package:<APP_PKG>/##' files.txt | awk -F/ '{print $1"/"$2"/"$3}' | sort -u
```

Con esto ya salen: hosts, endpoints sueltos, arquitectura (p. ej. GetX + Clean si ves
`modules/<x>/{data,domain,views}`), lista de módulos y de use cases. Es suficiente para el
mapa de features, pero **no** da los campos JSON ni qué endpoint llama cada método.

## 5. Identificar la versión exacta de Dart (necesaria para Blutter)

Blutter necesita el Dart SDK que coincide con el snapshot.

```bash
# El snapshot hash está ~20 bytes tras el magic f5f5dcdc; Blutter lo detecta solo:
python3 blutter-tool/extract_dart_info.py /ruta/extracted/lib/arm64-v8a
# → ('3.3.4', 'ee1eb666...hash...', [flags...], 'arm64', 'android')
```

## 6. Decompilar con Blutter (el paso que da los datos exactos)

Blutter **solo soporta `libapp.so` arm64**. Apúntalo al directorio de la ABI arm64 (que
contenga `libapp.so` + `libflutter.so`).

```bash
git clone --depth 1 https://github.com/worawit/blutter blutter-tool
python3 blutter-tool/blutter.py /ruta/extracted/lib/arm64-v8a /ruta/salida
```

La primera ejecución **clona las fuentes del Dart SDK** de esa versión y **compila** un
módulo del Dart VM (varios minutos; déjalo en background y espera con una condición, no con
sleeps encadenados). Ejecuciones posteriores reutilizan el build. Si el log corta tras
"Build files have been written", **re-ejecuta** el mismo comando: salta directo a decompilar.

### Salida de Blutter (`/ruta/salida`)
| Archivo/carpeta | Contenido |
|---|---|
| `asm/<libreria>/.../*.dart` | pseudo-código por librería (ensamblador anotado + estructura de clases) |
| `asm/<APP_PKG>/app/modules/...` | **el código propio de la app** organizado por módulo |
| `objs.txt` | objetos del heap con sus valores (enums, constantes) |
| `pp.txt` | object pool completo |
| `blutter_frida.js` | script Frida para instrumentación dinámica (si tuvieras el dispositivo) |
| `ida_script/` | scripts para IDA Pro |

## 7. Extraer los esquemas exactos (campos JSON)

Los métodos `fromJson`/`toJson` cargan los **nombres de campo JSON como literales de cadena**.
Se leen de los `.dart` de Blutter en `asm/<APP_PKG>/app/modules/*/data/model*/`.

```bash
cd /ruta/salida/asm/<APP_PKG>
f=./app/modules/card/data/models/card_model.dart
grep -E "^class " "$f"                                   # clases del archivo
grep -oE '\] "[^"]+"' "$f" | sed -E 's/\] "//; s/"$//' | sort -u   # literales = campos JSON
```

Para barrer **todos** los modelos a un archivo (ver `APENDICE-campos-por-modelo.txt`):
```bash
for f in $(find ./app/modules -path '*data/model*' -name '*.dart'); do
  echo "## $f"; grep -E "^class " "$f"
  grep -oE '\] "[a-zA-Z_][a-zA-Z0-9_]*"' "$f" | sed -E 's/\] "//; s/"$//' | sort -u
done
```

## 8. Mapear endpoints → request (qué envía cada método)

Los `*_remote_datasource.dart` contienen las URLs completas, los verbos y las claves de
body/query. Extráelos por datasource (ver `APENDICE-endpoints-por-datasource.txt`):

```bash
cd /ruta/salida/asm/<APP_PKG>
for f in $(find ./app/modules -name '*remote*datasource*.dart'); do
  echo "== $f"
  grep -oE '\] "(https://[^"]+|/[a-zA-Z0-9][a-zA-Z0-9._/?=&{}%-]+)"' "$f" | sed -E 's/\] "//; s/"$//' | sort -u
  grep -oE '\] "[a-zA-Z_][a-zA-Z0-9_]{1,30}"' "$f" | sed -E 's/\] "//; s/"$//' | sort -u   # claves body/query
done
```

Señales útiles que aparecen en los datasources:
- `::post` / `::get` y `HttpClient` → verbo probable.
- Literales `grant_type=...&client_id=...` → flujo OAuth completo (incluido el `client_id`).
- Headers (`Authorization`, `Content-Type`) → esquema de auth.
- Strings de log del desarrollador → pistas semánticas de qué hace cada método.

## 9. Analizar seguridad y criptografía

```bash
cd /ruta/salida/asm
# Headers de auth / firma / api-key
grep -rhoE '\] "(X-[A-Za-z-]+|api[_-]?key|Authorization|Signature|hmac)"' . | sort | uniq -c
# Certificate pinning
grep -rl "badCertificate\|CertificatePinning" .
# Módulo de cifrado propio
grep -rln "RSAEncrypt\|encryptFromCrt\|pointycastle" <APP_PKG>
# Claves/certs empaquetados en assets
find /ruta/extracted/assets -iname '*.pem' -o -iname '*.crt' -o -iname '*.cer'
openssl pkey -pubin -in <clave>.pem -noout -text   # confirmar si es pública
```
Regla: una clave **pública** empaquetada no es un secreto. Busca (y **no** publiques) claves
privadas, tokens o credenciales embebidas.

## 10. (Opcional) Validar contra el backend real

Para confirmar que lo extraído coincide con la realidad, sin mover dinero. Las pruebas de concepto usadas (un script
Python y una app de macOS) **no se versionan en este repositorio**: consultan el backend de producción con una cuenta
real. El método es siempre el mismo y se puede rehacer: PKCE S256 → login del usuario en navegador → canje del `code`
→ GET de lectura → comparar las claves JSON recibidas con las documentadas. **Solo lectura, con cuenta propia.** Lo
verificado así lleva la marca ✅ en `06` y `07`.

## 10b. Reproducir el análisis desde el APK

1. Consigue `cu.xetid.apk.enzona-v20003.apk` (no se versiona) y comprueba su huella:
   `shasum -a 256 <apk>` debe dar `9125313c01eb522522c2a1e312c62ad09a81193bc5b427a36b34114c701a4c0c`.
2. Sigue las secciones 1–9 de este documento. La salida de Blutter ocupa unos 320 MB: guárdala FUERA del repo
   (por ejemplo en `~/Library/Caches/`) o en una carpeta ignorada por git.
3. Las referencias a `objs.txt`, `pp.txt` y `asm/` de otros documentos son de esa salida local, no de archivos del repo.

## 11. Límites del método

- Blutter da **estructura y nombres** con alta fidelidad, pero el **cuerpo lógico** de cada
  función queda en ensamblador anotado (no Dart legible). Para lógica fina, lee el asm o usa
  Frida con el dispositivo.
- Los **verbos HTTP** y **tipos de campo** se infieren del nombre/uso; confírmalos con tráfico real.
- Solo se decompila **arm64**; las otras ABIs se asumen equivalentes.
- `strings` produce falsos positivos (literales de librerías, no de la app): cruza siempre
  con el `package:` propio.

## 12. Herramientas usadas (referencia)

| Herramienta | Para qué | Fuente |
|---|---|---|
| `unzip` / `strings` | Extracción y triage | estándar |
| Python (`re`, `pyelftools`) | Parseo de manifest y ELF | estándar / pip |
| **Blutter** | Decompilar snapshot Dart AOT | github.com/worawit/blutter |
| `cmake` + `ninja` + Clang | Compilar el Dart VM de Blutter | brew/apt |
| `openssl` | Inspeccionar claves/certs | estándar |
| REA (`rea-agents`) | Triage asistido / MCP para agentes | github.com/morluto/rea |
| `requests` (Python) | PoC de validación (fuera de este repo) | pip |
| (alt.) reFlutter, apktool, jadx, Ghidra, Frida | Otras rutas de análisis | respectivos repos |
