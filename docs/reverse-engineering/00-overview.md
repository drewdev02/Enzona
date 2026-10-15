# EnZona — Documentación de ingeniería inversa para reimplementación en KMP

> **Objetivo**: reconstruir una app equivalente a EnZona (pasarela de pago cubana) con
> **Kotlin Multiplatform** (iOS + Android), partiendo del análisis estático de la APK
> `cu.xetid.apk.enzona-v20003.apk` (versión 20003).
>
> Esta documentación describe **cómo está construida la app y qué hace**, a nivel de
> arquitectura, API y casos de uso. **No** incluye secretos, claves de firma ni material
> que permita atacar o suplantar el servicio real de EnZona. Los endpoints se documentan
> como contrato funcional: para operar realmente necesitas credenciales de cliente OAuth
> emitidas por EnZona/XETID. Reutilizar la marca, el `client_id` observado o el backend de
> producción requiere autorización del titular.

## Índice de documentos

| Archivo | Contenido |
|---|---|
| `00-overview.md` | Este documento: resumen, stack original, decisiones de diseño |
| `01-arquitectura.md` | Arquitectura original (GetX + Clean) y la propuesta KMP equivalente |
| `02-api-reference.md` | Todos los endpoints descubiertos, auth OAuth2/PKCE, agrupados por servicio |
| `03-casos-de-uso.md` | Épicas, historias de usuario y casos de uso por módulo |
| `04-modelo-datos.md` | Entidades de dominio y modelos de datos |
| `05-kmp-mapping.md` | Mapeo dependencia-a-dependencia Flutter → KMP y plan de módulos |

## Identidad del artefacto analizado

- **Paquete Android**: `cu.xetid.apk.enzona`
- **Versión**: 20003 (`enzona-v20003`)
- **Framework**: Flutter (Dart AOT) — confirmado por `lib/<abi>/libapp.so` + `libflutter.so`
- **ABIs**: `arm64-v8a`, `armeabi-v7a`, `x86_64`
- **Capa nativa Android**: `cu.xetid.apk.enzona.MainActivity` (wrapper Flutter estándar)
- **Gestión de estado**: **GetX** (`package:get`) con bindings/controllers/rutas nombradas
- **Arquitectura interna**: **Clean Architecture** por feature (data / domain / presentation)
- **Nº de módulos de feature**: 20
- **Nº de ficheros Dart propios**: ~775 (`package:apkenzona/...`)

> Nota de método: los nombres de ficheros, módulos, casos de uso y endpoints provienen
> de las cadenas (`package:` URIs y literales de texto) retenidas en el snapshot Dart AOT
> de `libapp.so`. La lógica interna de cada función **no** se recupera del AOT; se infiere
> del nombrado (que sigue convenciones Clean/GetX muy explícitas) y del contrato de API.

## Backend y dominios observados

| Host | Rol |
|---|---|
| `https://api.enzona.net` | API REST principal (todos los servicios de negocio) |
| `https://identity.enzona.net` | Identity Provider OAuth2/OIDC (WSO2 Identity Server) |
| `https://media.enzona.net` | CDN de imágenes (avatares de usuario, etc.) |
| `https://buscador.enzona.net` | Buscador (comercios / contenido) |
| `https://service-core.enzona.net` | Servicios de core (p. ej. ATM) |
| `http://nyfy-push.enzona.net/ntfy/send` | Push notifications (ntfy) |
| `http://util.enzona.net/enzona.apk` | Distribución del APK (auto-update) |
| `https://api.apklis.cu/` | Tienda Apklis (verificación/rating de la app) |

## Permisos Android declarados (relevantes para KMP)

`INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CAMERA` (escaneo QR),
`READ_CONTACTS` (seleccionar destinatarios desde la agenda), `USE_BIOMETRIC` /
`USE_FINGERPRINT` (login/pago con huella), `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`,
`READ/WRITE_EXTERNAL_STORAGE` (descargas/comprobantes), `READ_PHONE_STATE`, `VIBRATE`,
`WAKE_LOCK`.

## Funcionalidad de alto nivel (resumen)

EnZona es una billetera/pasarela de pago que permite:

1. **Cuenta e identidad** — registro/verificación, perfil, contraseña de acceso y
   **contraseña de pago** independiente, 2FA TOTP, verificación por teléfono/SMS.
2. **Tarjetas** — asociar y administrar tarjetas, tarjeta por defecto, activación por PIN.
3. **Transferencias** — a cuenta EnZona, a tarjeta, a tarjeta Fincimex, pago a comercio por QR.
4. **Cuentas bancarias / fiscales** — vincular cuenta, cuenta fiscal (ONAT).
5. **QR** — escanear para pagar a comercio/ATM/persona, y generar QR propio para cobrar.
6. **Pago de servicios** — ETECSA/Nauta, electricidad, gas, impuestos (ONAT/RC-05).
7. **Giros / Cuban email money order**, **donaciones**, **regalos (gift)**.
8. **Social** — amigos/contactos, destinatarios, organizaciones sociales, bulevar (tiendas).
9. **Transacciones** — historial, detalle, filtros.
10. **Seguridad y soporte** — biometría, caché, notificaciones, contáctenos, auto-update.
