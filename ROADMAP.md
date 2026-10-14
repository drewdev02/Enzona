# Roadmap — EnZona KMP

Hitos incrementales. Cada uno deja algo **ejecutable y verificable** en Android e iOS.
Arquitectura y decisiones: ver [`ARCHITECTURE.md`](ARCHITECTURE.md). Esquemas y endpoints
verificados: `../tools/rea/docs/enzona-kmp/` (06, 07, 08).

Leyenda: ☐ pendiente · ◧ en curso · ☑ hecho.

---

## M0 · Base del proyecto  ✅ HECHO (commit `196646e`)
**Meta:** esqueleto limpio que compila en ambas plataformas, listo para features.

- ☑ Retirar `sharedUI` (Compose MP) de `settings.gradle.kts` y del árbol; `androidApp` depende de `sharedLogic`.
- ☑ Catálogo `libs.versions.toml`: Ktor (core, content-negotiation, logging, json, auth, OkHttp, Darwin), kotlinx-serialization-json, kotlinx-coroutines, kotlinx-datetime, multiplatform-settings, Napier. **(SKIE diferido a M1.)**
- ☑ `sharedLogic`: targets android/iosArm64/iosSimulatorArm64; plugin serialization aplicado.
- ☑ Paquetes base en `commonMain`: `core/{network,storage,di,result}` + `EnzonaConfig` (data/domain/feature se crean por feature desde M1).
- ☑ `core/result`: `EzResult<T>` + `Failure` (network, http, parse, auth, unknown).
- ☑ `core/network`: `HttpClientFactory` (Ktor) con Json laxo, base `api.enzona.net`, logging; engine `expect/actual` (OkHttp/Darwin).
- ☑ `SecureStorage` (interface + Android SharedPreferences / iOS NSUserDefaults). ⚠ TODO M1: endurecer a Keystore/Keychain.
- ☑ Koin: `coreModule` + `platformModule` (expect/actual) conectados a `initKoin()`; arranca en `EnzonaApp` (Android) e `iosApp`.
- ☑ Config: `clientId`, hosts, `redirectUri` en `EnzonaConfig` (usar **client_id propio**).
- ☐ **Pendiente movido a M1:** plugin **SKIE** (interop Flow→Swift).

**Done:** ✅ `./gradlew :androidApp:assembleDebug` y `:sharedLogic:compileKotlinIosSimulatorArm64` compilan; ambas apps arrancan con pantalla nativa que usa la lógica compartida.

---

## M1 · Autenticación (end-to-end)  ✅ HECHO — aceptado sin prueba del login real
**Meta:** login real con OAuth/PKCE + sesión persistente, UI nativa en cada plataforma.

- ⛔ ~~Plugin SKIE~~ → **no compatible con Kotlin 2.4.20/AGP 9**; se usa un **puente manual** (`core/util/FlowBridge.kt`: `Cancellable` + `StateFlow.watch`). Migrar a SKIE cuando soporte Kotlin 2.4.
- ☑ `core/crypto/Pkce`: S256 (okio). **Verificado contra el vector de la RFC 7636** (test, Android + iOS).
- ☑ `feature/auth/data/OAuthService`: authorize URL + exchange + refresh (form-urlencoded).
- ☑ `feature/auth/data/TokenStore` sobre `SecureStorage`.
- ☑ **Refresh automático ante 401**: interceptor `HttpSend` (no el plugin `Auth` de Ktor, que cachea tokens). Un cliente sin auth para el IdP y otro para la API; refresh serializado con mutex; un solo reintento; el token solo viaja al host de la API. 5 tests (Android + iOS).
- ☑ Sesión caducada: si el servidor rechaza el refresh se borran los tokens y se vuelve al login con aviso; un fallo de red **no** cierra la sesión.
- ☑ `AuthViewModel` + `AuthUiState` (StateFlow) + intents; restaurar sesión al arranque.
- ☑ Android: **Custom Tabs** + deep link `apk-callback`; pantallas Compose.
- ☑ iOS: **WKWebView** (ASWebAuthenticationSession no capta `http://`); pantallas SwiftUI. **El Swift compila** contra el framework real (`xcodebuild`).
- ☑ **SecureStorage endurecido**: Android = AES-256-GCM con clave en el **Android Keystore**; iOS = **Keychain** (`AfterFirstUnlockThisDeviceOnly`). `allowBackup=false`.
  - Verificado **dentro de la app real** (emulador Android y simulador iOS): guardar/leer con unicode, sobrescribir, borrar; en Android el valor no queda en claro en disco.
  - ⚠ No hay test automatizado del Keychain/Keystore: el Keychain no está disponible en el proceso de test de línea de comandos (`errSecNotAvailable`) y el Keystore necesita dispositivo. Se probó con un probe temporal, ya retirado.
- ◧ **Aceptado sin probar** (decisión del usuario): login real (portal OAuth + SMS OTP) en emulador/simulador con una cuenta propia. Nada del flujo completo contra el IdP se ha ejecutado aún en la app KMP; lo validado es cada pieza por separado y el flujo equivalente en el PoC de macOS.
- ☐ `client_id` propio (hoy se usa el del APK oficial como marcador).

**Done cuando:** login real en Android e iOS con token en Keychain/Keystore y re-arranque sin re-login.
(Login multi-paso con **SMS OTP**: se honra, no se evita.)

---

## M2 · Perfil / Cuenta  ✅ HECHO — UI no vista con sesión real
**Meta:** primera feature completa; valida el patrón "ViewModel compartido + UI nativa".

- ☑ `domain`: `Account` + `AccountRepository` + `GetAccountUseCase` (username = claim `sub` del id_token).
- ☑ `data`: `GET /account/v1.0.0/account?username=<sub>`, envelope `{"account":{}}`. DTO solo con los campos usados; los flags de tipo laxo (`"1"`, `"true"`, `"8"`, `false`) se leen como `String` y se normalizan en el mapper.
- ☑ El cuerpo de error nunca se propaga (puede llevar datos personales).
- ☑ `AccountViewModel` + `AccountUiState` (StateFlow) + `watchState` para Swift.
- ☑ **Navegación**: Android = `androidx.navigation:navigation-compose` **2.10.2** (rutas tipadas `@Serializable`); iOS = `NavigationStack`. (Navigation 3 queda como alternativa futura.)
- ☑ Android: Home → Mi perfil en **Compose**. iOS: Home → Mi perfil en **SwiftUI**.
- ☑ Fix transversal: `apiCall` y `OAuthService` ya no convierten `CancellationException` en error de red.
- ☑ Tests (20/20 en Android e iOS): forma real de la respuesta con datos inventados, tipos laxos, `false` como ausente, 404 sin filtrar cuerpo, JSON malformado, JWT, y **resolución del grafo de Koin** completo.
- ⚠ **Sin ver en pantalla con sesión real**: ambas UIs compilan y el grafo se resuelve en test, pero el perfil está detrás del login. La respuesta real solo se validó vía el PoC de macOS.
- ☐ Avatar remoto (`avatarUrl`): hoy se muestran iniciales; requiere librería de imágenes.

**Done cuando:** ambas apps muestran el perfil real del usuario logueado desde el mismo ViewModel.

---

## M3 · Tarjetas + Dashboard  ◧ estructura visual hecha
- ☑ **Estructura del dashboard** (según el mockup de la app original, con componentes nativos): cabecera con degradado + acciones Escanear/Recibir, hoja blanca con insignia, barra inferior de 4 pestañas (Inicio · Avisos · Amigos · Perfil). Android: Compose, píldora propia, iconos dibujados con Canvas. iOS: `TabView` nativo (cápsula flotante en iOS 26+), SF Symbols.
- ☑ Pantallas sin estado con `@Preview` (Android) y `#Preview` (iOS); verificadas visualmente en emulador y simulador con datos inventados.
- ☑ Perfil pasa a ser una pestaña y aloja "Cerrar sesión". Avisos y Amigos son pantallas "próximamente".
- ⚠ Escanear y Recibir **no hacen nada aún** (llegan con M5). La insignia es un **placeholder neutro** (tres barras): usar el logotipo propio.
- ⚠ No verificado en pantalla: que Perfil restaure los iconos oscuros de la barra de estado al salir del dashboard (la lógica lo hace, pero no lo vi).
- ☐ `CardDto` + `CardRepository` (`GET /coremz/1.0.2/card/username/{username}`, envelope `{"card":[]}`).
- ☐ Balance (`/coremz/1.0.2/api/balance/lastOperations/{uuid}`), tarjeta por defecto/principal.
- ☐ `HomeViewModel` + dashboard (saldo, tarjetas, accesos rápidos).
- ☐ UI nativa: lista de tarjetas + home en Compose y SwiftUI.

**Done cuando:** home funcional con tarjetas reales en ambas plataformas.

---

## M4 · Transacciones
- ☐ `TransactionDto` + `PaginationDto` (sub-objetos `source`/`recipient` anidados).
- ☐ `GET /operations/v1.0.0/operations/account?offset=&limit=` (paginado); filtros opcionales.
- ☐ `TransactionsViewModel` con paginación (offset/limit, carga incremental).
- ☐ UI nativa: historial + detalle.

**Done cuando:** historial paginado real en ambas plataformas.

---

## M5 · Transferencias + QR (núcleo de dinero)
- ☐ `expect/actual RsaEncryptor` (clave pública empaquetada) para cifrar contraseña de pago / datos de tarjeta.
- ☐ `expect/actual Biometrics` (BiometricPrompt / LAContext) para confirmar operaciones.
- ☐ Transferencia a cuenta (`/transfer/v1.0.0/transfer/account`) y a tarjeta (`/transfer/card`), con `payment_password` cifrada.
- ☐ QR: escáner nativo (CameraX+MLKit / AVFoundation) + generación (zxing / nativo).
- ☐ Pago a comercio por QR; flujo de confirmación.

**Done cuando:** una transferencia real de prueba se completa con confirmación biométrica. (Operaciones de dinero: probar con cuenta y montos propios.)

---

## M6 · Pago de servicios
- ☐ Plantilla `ServicePaymentFeature` parametrizada (patrón `invoice/v1.0.0` común).
- ☐ Instancias: ETECSA/Nauta, electricidad, gas, impuestos (ONAT/RC-05).
- ☐ Alta de id de cliente, búsqueda/listado de facturas, pago, histórico.

**Done cuando:** al menos un servicio (ETECSA) paga de punta a punta; los demás reutilizan la plantilla.

---

## Transversales (continuo)
- ☐ Manejo de errores uniforme (`Failure` → mensajes de UI).
- ☐ Notificaciones (ntfy `/nyfy/1.0/ntfy/*` + WebSocket) y push.
- ☐ i18n (es/en), formato de moneda/fecha.
- ☐ Tests: unit (domain/usecases), contract (DTOs vs esquemas verificados), auth flow.
- ☐ CI (build Android + framework iOS), lint/format.
- ☐ Observabilidad (Napier) sin filtrar datos sensibles.

## Orden recomendado
**M0 → M1** primero (prueban toda la arquitectura). Luego M2 como plantilla de feature;
M3–M6 repiten el patrón sobre endpoints ya verificados.
