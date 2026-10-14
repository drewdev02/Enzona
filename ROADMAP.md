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

## M1 · Autenticación (end-to-end)
**Meta:** login real con OAuth/PKCE + sesión persistente, UI nativa en cada plataforma.

- ☐ Añadir plugin **SKIE** a `sharedLogic` (interop Flow/suspend → Swift). *(movido desde M0)*
- ☐ Endurecer `SecureStorage`: Keystore (Android) / Keychain (iOS) antes de guardar tokens reales. *(movido desde M0)*
- ☐ `core/crypto`: `PKCE` (code_verifier + S256 challenge) en commonMain.
- ☐ `feature/auth/data`: `OAuthService` (authorize URL, exchange code, refresh) con `grant_type=authorization_code|refresh_token`, `application/x-www-form-urlencoded`.
- ☐ `feature/auth/data`: `TokenStore` sobre `SecureStorage` (access/refresh/id_token).
- ☐ `core/network`: plugin de auth con **refresh automático** ante 401.
- ☐ `feature/auth/presentation`: `AuthViewModel` + `AuthUiState` (StateFlow) + intents (login, logout).
- ☐ Android: **Custom Tabs** para abrir authorize; capturar redirect `apk-callback` (deep link / intent) → devolver `code` al ViewModel.
- ☐ iOS: **ASWebAuthenticationSession**; devolver `code` al ViewModel (vía SKIE).
- ☐ Restaurar sesión al arranque; renovar con refresh_token.

**Done cuando:** login real en Android e iOS, token en Keychain/Keystore, re-arranque sin re-login. (El login es multi-paso con **SMS OTP**: se honra, no se evita.)

---

## M2 · Perfil / Cuenta
**Meta:** primera feature completa; valida el patrón "ViewModel compartido + UI nativa".

- ☐ `domain`: `Account` entity + `AccountRepository` + `GetAccountUseCase`.
- ☐ `data`: `AccountDto` (campos verificados), `AccountRemoteDataSource` (`GET /account/v1.0.0/account?username=<sub>`, envelope `{"account":{}}`), `AccountRepositoryImpl`.
- ☐ `feature/account/presentation`: `AccountViewModel` + `AccountUiState`.
- ☐ Android: pantalla perfil en **Compose** (`collectAsState`).
- ☐ iOS: pantalla perfil en **SwiftUI** (observa StateFlow vía SKIE).

**Done cuando:** ambas apps muestran el perfil real del usuario logueado desde el mismo ViewModel.

---

## M3 · Tarjetas + Dashboard
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
