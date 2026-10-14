# Arquitectura — EnZona KMP (lógica compartida, UI nativa)

Reimplementación de una billetera estilo EnZona en **Kotlin Multiplatform**: **lógica
compartida** en Kotlin, **UI nativa** en cada plataforma (Jetpack Compose en Android,
SwiftUI en iOS). Los endpoints, esquemas y flujo de auth están verificados contra el
backend real (ver `../tools/rea/docs/enzona-kmp/`).

> ⚠️ Cliente interoperable para estudio/uso propio. Requiere un `client_id` OAuth propio y,
> para operar, un usuario autenticándose de verdad (con su SMS OTP). No se eluden controles
> de seguridad ni se embebe ningún secreto.

## 1. Principio rector

- **Comparte hasta el ViewModel.** No solo repos: también la presentación (UiState + intents).
  Así no se duplica lógica de carga/validación/error en Kotlin y Swift.
- **La UI nativa solo pinta estado y dispara intents.** Compose y SwiftUI renderizan el
  mismo `UiState` expuesto como `StateFlow`.

## 2. Módulos

Estado actual del scaffold: `sharedLogic`, `sharedUI` (Compose MP), `androidApp`, `iosApp`.

**Decisión: se retira `sharedUI`.** Como la UI es nativa, el módulo Compose Multiplatform no
aporta. `androidApp` (Compose) e `iosApp` (SwiftUI) dependen directamente de `sharedLogic`.

```
Enzona/
  sharedLogic/           # KMP: androidTarget + iosArm64 + iosSimulatorArm64
    src/commonMain/kotlin/org/andrew/enzona/
      core/        network · storage · crypto · di · result · logging
      data/        dto · datasource · repository (impl)
      domain/      entity · repository (interfaz) · usecase
      feature/<x>/ presentation (ViewModel + UiState + Intent) por feature
    src/androidMain/      actual: SecureStorage(Keystore), RsaEncryptor(JCA), httpEngine(OkHttp)
    src/iosMain/          actual: SecureStorage(Keychain), RsaEncryptor(Security), httpEngine(Darwin)
  androidApp/             Jetpack Compose — consume sharedLogic (collectAsState)
  iosApp/                 SwiftUI — consume el framework SharedLogic vía SKIE
```

> `sharedUI` → eliminar (o, si algún día quieres un design system Compose **solo-Android**
> reutilizable entre varias apps Android, renombrarlo a `designSystem` con target Android).

## 3. Capas (Clean Architecture por feature)

```
UI nativa (Compose / SwiftUI)
   │ observa UiState (StateFlow) · envía Intent
ViewModel (commonMain, por feature)
   │ llama UseCase
UseCase (domain)
   │ usa Repository (interfaz)
RepositoryImpl (data)
   │ RemoteDataSource (Ktor)      LocalDataSource (SecureStorage)
   ▼
api.enzona.net / identity.enzona.net
```

- `Result<Failure, T>` (sealed) atraviesa las capas (análogo al `Either/dartz` del original).
- Mapeo `Dto` (data) → `Entity` (domain) en el `RepositoryImpl`.

## 4. El puente Swift — puente manual (SKIE en pausa)

> ⚠️ **SKIE no es compatible con este toolchain**: soporta Kotlin hasta 2.1.10, y el proyecto
> usa Kotlin 2.4.20 (además AGP 9.1.1 fuerza ≥ 2.2.10). Usarlo exigiría bajar AGP a 8.x y
> reescribir el DSL Android KMP. Se adoptará cuando SKIE publique soporte para Kotlin 2.4.

Mientras tanto, un **puente manual ligero** expone los `StateFlow` a SwiftUI:
- `core/util/FlowBridge.kt`: `Cancellable` + `StateFlow.watch(onEach)`.
- El ViewModel expone `watchState(onEach:): Cancellable`, consumible directo desde Swift.
- SwiftUI envuelve el ViewModel en un `ObservableObject` que actualiza `@Published` desde
  el callback (ver `iosApp/AuthModel.swift`).
- Los `sealed interface` de Kotlin se chequean en Swift con `case is AuthUiStateLoggedIn`.

Migrar a SKIE después = cambiar solo esta capa de observación iOS; la lógica no se toca.

## 5. Autenticación (verificada contra producción)

- **OAuth2 Authorization Code + PKCE** (cliente público, sin secreto) contra WSO2
  (`identity.enzona.net`). `client_id` propio.
- **Login multi-paso con MFA** (usuario/contraseña → **SMS OTP** → paso custom): lo renderiza
  el IdP como HTML → se abre en el **navegador del sistema**:
  - Android: **Custom Tabs**.
  - iOS: **ASWebAuthenticationSession**.
- PKCE S256 se genera en `sharedLogic`; la UI nativa abre el navegador y devuelve el `code`
  del redirect `http://apk-callback?code=…` al `OAuthService` compartido, que canjea el token.
- Token en **SecureStorage** (Keystore/Keychain) + **refresh_token** → sesión persistente;
  el navegador aparece una sola vez.
- El resto de la API es HTTP puro en `sharedLogic` (sin navegador).

> No es posible (ni deseable) saltarse el SMS OTP: ROPC (`grant_type=password`) no lo soporta
> y el SP no lo expone. UI 100% nativa de login solo sería viable si EnZona/XETID habilitara
> "API-based authentication" en su WSO2 para tu `client_id` (gestión con ellos).

## 6. Decisiones técnicas confirmadas por la ingeniería inversa

- **Tipos laxos del backend**: devuelve `false` en campos no aplicables y mezcla
  número/string/bool para flags. → `Json { isLenient = true; ignoreUnknownKeys = true;
  coerceInputValues = true }` y serializers tolerantes para campos inestables.
- **Dinero como `String`/`BigDecimal`**, nunca `Double`.
- **Contraseña de pago / datos de tarjeta** se cifran con **RSA (clave pública empaquetada)**
  antes de enviarse → `expect/actual RsaEncryptor`.
- **Endpoints verificados** (ver docs 06/07): cuenta `GET /account/v1.0.0/account?username=<sub>`
  (envelope `{"account":{}}`); tarjetas `GET /coremz/1.0.2/card/username/{username}`
  (`{"card":[]}`); transacciones `GET /operations/v1.0.0/operations/account?offset=&limit=`
  (`{"transaction":[],"pagination":{}}`).

## 7. Stack de dependencias

| Área | Librería |
|---|---|
| HTTP | Ktor client (core, content-negotiation, logging; OkHttp/Darwin engines) |
| JSON | kotlinx-serialization-json (laxo) |
| Async | kotlinx-coroutines |
| Fechas | kotlinx-datetime |
| DI | Koin (ya presente) |
| Storage | multiplatform-settings o `expect SecureStorage` |
| Logs | Napier |
| Swift interop | **SKIE** (plugin) |
| Auth UI | Custom Tabs (Android) · WKWebView (iOS; ASWebAuthenticationSession no capta `http://`) |
| Navegación | Android: `navigation-compose` 2.10.2 (rutas tipadas) · iOS: `NavigationStack` |
| QR | CameraX + MLKit (Android) · AVFoundation (iOS) |
| Biometría | BiometricPrompt (Android) · LAContext (iOS) |

## 8. Roadmap

| Hito | Entrega | Contenido |
|---|---|---|
| **M0 · Base** | Esqueleto limpio que compila | Retirar `sharedUI`; añadir Ktor/serialization/coroutines/SKIE/settings al catálogo; paquetes `core/data/domain/feature`; Koin en ambos apps |
| **M1 · Auth** | Login real end-to-end (Android + iOS) | PKCE + OAuthService + token store + refresh + `AuthViewModel`; Custom Tabs / ASWebAuthenticationSession; sesión persistente |
| **M2 · Perfil** | Pantalla cuenta en ambas apps | `AccountRepository` + `AccountViewModel`; Compose + SwiftUI sobre el mismo estado (valida el patrón UI nativa) |
| **M3 · Tarjetas + Dashboard** | Home funcional | Cards + balance + home |
| **M4 · Transacciones** | Historial paginado | `operations` + `PaginationModel` + lista nativa |
| **M5 · Transferencias + QR** | Núcleo de dinero | transfer a cuenta/tarjeta + RsaEncryptor + QR nativo + biometría |
| **M6 · Servicios** | Pago de facturas | ETECSA/luz/gas/impuestos (una plantilla, 4 instancias) |

**Arranque recomendado: M0 + M1.** Es el hito que prueba toda la arquitectura
(shared logic + SKIE + UI nativa + auth real). El resto es repetir el patrón por feature.

## 9. Referencias

- Documentación de ingeniería inversa: `../tools/rea/docs/enzona-kmp/`
  (00–10 + apéndices + PoCs de validación).
- Esquemas verificados: `06-esquemas-exactos.md`. Endpoints: `07-endpoints-request-mapping.md`.
  Seguridad/cripto: `08-seguridad-y-cripto.md`.
