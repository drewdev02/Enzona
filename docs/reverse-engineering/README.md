# EnZona → KMP · Documentación de reimplementación

Ingeniería inversa **estática** de `cu.xetid.apk.enzona-v20003.apk` (Flutter) para
reconstruir una app equivalente en **Kotlin Multiplatform** (Android + iOS).

Analizado con REA + extracción de cadenas del snapshot Dart AOT (`libapp.so`).

## Documentos

1. [`00-overview.md`](00-overview.md) — Resumen, stack, dominios, permisos, funcionalidad.
2. [`01-arquitectura.md`](01-arquitectura.md) — Arquitectura original (GetX + Clean) y la propuesta KMP.
3. [`02-api-reference.md`](02-api-reference.md) — Endpoints, OAuth2/PKCE, por servicio.
4. [`03-casos-de-uso.md`](03-casos-de-uso.md) — 13 épicas, historias de usuario, casos de uso.
5. [`04-modelo-datos.md`](04-modelo-datos.md) — Entidades y DTOs.
6. [`05-kmp-mapping.md`](05-kmp-mapping.md) — Mapeo de dependencias y plan de hitos.
7. [`06-esquemas-exactos.md`](06-esquemas-exactos.md) — **Campos JSON reales** por modelo (recuperados con Blutter).
8. [`07-endpoints-request-mapping.md`](07-endpoints-request-mapping.md) — **Catálogo de ~114 endpoints**, verbos y cuerpos de request + flujo OAuth completo.
9. [`08-seguridad-y-cripto.md`](08-seguridad-y-cripto.md) — Auth, firma, cifrado RSA, secure storage, biometría: **no hace falta ningún secreto**.
9b. [`09-resumen-endpoints.md`](09-resumen-endpoints.md) — **Resumen rápido**: cada endpoint → qué envías / qué hace / qué recibes.
9c. [`10-metodologia.md`](10-metodologia.md) — **Cómo se extrajo todo**: herramientas y comandos paso a paso (reproducible por otros agentes).
9d. [`11-capturas-live.md`](11-capturas-live.md) — **Capturas en vivo (2026-10-15)**: 64 respuestas GET reales (`✅`), envelopes de error, tipos laxos y **discrepancias a revisar en tests y app**. Los `.json` crudos viven en [`sharedLogic/src/commonTest/resources/capturas-live/`](../../sharedLogic/src/commonTest/resources/capturas-live/_LEEME.md) ⚠ con datos personales reales: repo privado.
10. [`APENDICE-campos-por-modelo.txt`](APENDICE-campos-por-modelo.txt) — Volcado bruto de campos de **todos** los modelos.
11. [`APENDICE-endpoints-por-datasource.txt`](APENDICE-endpoints-por-datasource.txt) — Volcado bruto de endpoints por datasource.

## Qué NO está en este repositorio

- **Las pruebas de concepto** (un script Python y una app de macOS) con las que se verificaron los esquemas contra el
  backend real. Los documentos las citan como procedencia ("PoC macOS, HTTP 200…"), pero su código se queda fuera a
  propósito: consultan el backend de producción con una cuenta real y no deben versionarse aquí.
- **El APK original** (`cu.xetid.apk.enzona-v20003.apk`, 44 070 398 bytes, SHA-256
  `9125313c01eb522522c2a1e312c62ad09a81193bc5b427a36b34114c701a4c0c`): es una app de terceros; no se versionan
  binarios. El SHA-256 permite comprobar que se trabaja con la misma versión.
- **La salida de Blutter** (unos 320 MB): se regenera siguiendo [`10-metodologia.md`](10-metodologia.md).

## Cómo leer las marcas de verificación

| Marca | Significa |
|---|---|
| ✅ | Verificado en vivo contra el backend real. |
| 🔎 | Decodificado del APK por análisis estático; **no** validado en vivo. |
| ⚠ | Inferencia o duda abierta: se dice qué falta por confirmar. |

## Mantener estos documentos

Esta carpeta vive junto al código que la usa. Un hallazgo nuevo se documenta **en el mismo cambio** que el código que
depende de él, y una corrección del código que contradiga un documento corrige también el documento (el ejemplo
reciente: la tarjeta Bandec Tropical se elige por BIN, no por moneda; ver `06`).

## Alcance y límites

- **Sí**: arquitectura, módulos, endpoints, casos de uso, modelo de datos, plan de portado.
- **No** (fuera de alcance por diseño): secretos y claves de firma. Los campos JSON se recuperaron del código
  decompilado (ver `06`) y parte de ellos se verificó en vivo; lo que no, lleva la marca 🔎 o ⚠.
- **Legal/ético**: documentación para estudio y reimplementación propia. No reutilices la
  marca EnZona, su `client_id` OAuth ni operes contra el backend de producción sin
  autorización de EnZona/XETID. Las operaciones reales requieren credenciales de cliente
  emitidas por ellos.

## Cómo profundizar (cerrar gaps)

- **Blutter** / **reFlutter**: recuperan nombres de clases/funciones del snapshot Dart AOT.
- Proxy (mitmproxy) con **cuenta de pruebas propia**: captura los esquemas JSON reales.
- REA (`inspect-dart-aot`, `decode-android-resources`) para ampliar el inventario.

## Confianza de los datos (procedencia)

| Dato | Fuente | Confianza |
|---|---|---|
| Framework, ABIs, paquete, permisos | ELF + AndroidManifest | Alta (directa) |
| Lista de módulos, use cases, vistas, entidades | `package:apkenzona/*` URIs en AOT | Alta (nombres reales) |
| Endpoints / hosts / flujo OAuth | literales de cadena en AOT | Alta (literales reales) |
| Campos JSON de los DTOs | literales `fromJson`/`toJson` decompilados con Blutter (Dart 3.3.4) | **Alta** (literales reales) — ver `06` |
| Tipos de cada campo, paginación | inferencia por uso en el ASM | Media (a confirmar) |
