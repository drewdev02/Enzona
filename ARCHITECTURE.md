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
  iosApp/                 SwiftUI — consume `SharedLogicSwift` vía Swift Export
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

## 4. El puente Swift — Swift Export (spike, Kotlin 2.5.0-Beta2)

> ⚠️ **Experimental**: Swift Export está en alpha y esta rama depende de Kotlin `2.5.0-Beta2`.
> SKIE no soporta este toolchain (Kotlin ≥ 2.2.10 por AGP 9.1.1), así que no es alternativa.

`sharedLogic` se exporta a Swift con `export { swift { ... } }` (módulo `SharedLogicSwift`); Xcode
lo integra con `:sharedLogic:embedSwiftExportForXcode`. `androidx.lifecycle.viewmodel` se marca
`SwiftExportVisibility.HIDDEN` porque su Swift generado no compila.

- Los ViewModels exponen `state: StateFlow`, que llega a Swift tipado (`KotlinTypedStateFlow`).
  SwiftUI lo observa con `for try await value in state.asAsyncSequence()` dentro de una `Task`
  que se cancela en `deinit` (ver `iosApp/Auth/LoginModel.swift`, `App/SessionModel.swift`).
- Los `sealed interface` se recorren con `switch state.sealedType()` (enum nativo, exhaustivo).
- `iosApp/Domain/KotlinAliases.swift` da nombres cortos a los tipos, que viven en
  `ExportedKotlinPackages.<paquete>`, y nombres planos a las subclases de los sealed.
- Cada pantalla tiene un holder **no genérico** (`LoginViewModelHolder`…, en `di/ViewModelHolder.kt`):
  Swift Export no exporta generics y el getter de `ViewModelHolder<T>` saldría como `fatalError()`.

Gotchas comprobados:
- Una propiedad llamada `description` no se exporta (choca con la de Swift): se añade un acceso
  `descriptionText`.
- Un `List<enum>` revienta en ejecución al convertirse en Swift: usar `Enum.allCases`.
- Los enums llegan como enums de Swift con casos en mayúsculas; el nombre es `.description`.
- Las funciones top-level son estáticas del namespace del paquete (`CardsDomain.bankBrandOf`).

### Ciclo de vida: quién es dueño de qué

Regla (guía de capa de datos de Android): el estado que **sobrevive a las pantallas** vive en la
capa de datos con vida de aplicación; el estado de **una pantalla** vive en un `ViewModel` que
muere con ella.

| Pieza | Vida | Por qué |
|---|---|---|
| `SessionRepository` (Koin `single`) | La app | Dueño de tokens, `SessionState` y el PKCE del login en curso. El redirect OAuth llega fuera de cualquier pantalla. |
| `LoginViewModel` | Pantalla de login (Android: la Activity, porque recibe el redirect) | Solo estado de UI: `Idle / Authenticating / Error`. |
| `AccountViewModel`, `CardsViewModel` | Su pantalla | Datos de un usuario: no deben sobrevivir al logout ni al cambio de usuario. |

- La raíz de cada app observa `SessionState` (`LoggedIn` / `LoggedOut(expired)`). Al salir de
  `LoggedIn` la zona autenticada deja de existir y sus ViewModels se destruyen con ella
  (`viewModelScope` cancela las cargas en vuelo).
- Los ViewModels son `androidx.lifecycle.ViewModel` (KMP) registrados con Koin `viewModel { }`.
  Android los resuelve con `koinViewModel()` por entrada de navegación.
- iOS no tiene `ViewModelStoreOwner`: `ViewModelHolder` (`di/ViewModelHolder.kt`) posee un
  `ViewModelStore`; el modelo Swift de la pantalla lo crea (`KoinHelper.*ViewModelHolder()`) y
  llama a `clear()` en `deinit`. `HomeView` sostiene los modelos de cuenta y tarjetas mientras
  haya sesión, así que mueren con ella.
- Los textos de cara al usuario (p. ej. "Tu sesión expiró") los decide la capa de UI a partir
  de hechos del dominio (`LoggedOut(expired = true)`), no el dominio.

### Logging (`core/logging`)

API inspirada en `Microsoft.Extensions.Logging`, con Kermit como motor (Logcat en Android,
`os_log` en iOS). El resto del código solo conoce `Logger`.

```kotlin
private val log = logger<SessionRepository>()          // categoría = nombre de la clase
log.info("Tarjetas cargadas: {Cantidad}", n)           // plantilla con nombre, por posición
log.error(e, "Falló {Operación}", op)                  // con excepción
log.with("requestId" to id).debug("→ {Path}", path)    // logger hijo (en vez de BeginScope)
log.info("token {T}", Sensitive(token))                // imprime ***
```

- Niveles: `TRACE, DEBUG, INFO, WARN, ERROR, CRITICAL`. Las plantillas solo se renderizan si el
  nivel está activo; no hay sobrecarga con lambda a propósito (interpolar strings se saltaría
  `Sensitive`). No hay `BeginScope`: es ambiental por hilo y no sirve con corrutinas.
- `installLogging(debug)` se llama una vez, **antes de Koin** (Android: bandera `debuggable`;
  iOS: `#if DEBUG`). Debug = desde `Debug`; release = desde `Info`. Sin configuración no se
  escribe nada. `LogConfig` admite un nivel mínimo distinto por categoría.
- `CRITICAL` viaja como `Error` con prefijo: el `Assert` de Kermit llama a `Log.wtf`, que según
  el dispositivo puede terminar el proceso.
- iOS: en debug los mensajes son públicos; en release `os_log` los oculta (`<private>`) fuera
  del depurador, a propósito. Ver `log show --info --debug --predicate 'subsystem == "org.andrew.enzona"'`.
- La categoría usa `T::class.simpleName`: si algún día se activa R8/minify se ofusca; entonces
  hay que añadir una regla keep o usar `logger("Nombre")`.

**Qué NUNCA se registra** (lo comprueba `SessionLogRedactionTest` y `NetworkLoggingTest`):
tokens (`access`, `refresh`, `id`), `code`, `code_verifier` / `challenge`, el **username**,
número/titular/contenido de tarjetas, cuerpos de respuesta, cabeceras ni la query de una URL.

- Red (`RequestLogging`, categoría `Network`): en Info solo `método host → estado en N ms`
  (+ `requestId`); la **ruta** solo en Debug, porque puede llevar el username
  (`/card/username/{username}`).
- Fallos sin respuesta (`apiCall`): el **tipo** de la excepción en Warn; su mensaje (puede traer
  la URL) solo en Debug.
- Los fallos se registran por su tipo (`Failure::class.simpleName`), no por su mensaje.
- `Failure.Auth` del IdP ya no incluye el cuerpo de la respuesta: lo ve el usuario y lo leen los
  logs, y un error del IdP puede repetir datos de la solicitud.

### Qué es compartido y qué es de cada plataforma (ejemplo: detalle de tarjeta)

El dominio guarda **hechos**; cada plataforma decide cómo se ven (habrá un rediseño distinto en iOS y
Android, así que nada visual se comparte).

| Compartido (`sharedLogic`) | Por plataforma (UI) |
|---|---|
| `CardsRepository.getActivity`, `CardActivity`, `CardOperation` | Textos ("Transferencia", "Débito"…) |
| `OperationKind` / `OperationDirection` (enums) y `classifyOperation` (tabla decodificada del original) | Tabla tipo → icono (`OperationIcons.kt` / `OperationPresentation.swift`) |
| `Money` (unidades menores en `Long`, sin `Double`), `parseMinorUnits` | Formato (`$3395.15`, signo, moneda en mayúsculas) |
| Fecha como el **texto recibido** (`dateText`) | Cómo se muestra (hoy tal cual) |
| `CardDetailViewModel(cardId)` (uno por pantalla; el id va como parámetro de Koin) | Distribución, apilado con fuente grande, accesibilidad |

- `tools/checks/check_assets.py` comprueba que los iconos `op_*` existen en ambas plataformas con el mismo contenido y que
  las dos tablas tipo → icono cubren todos los `OperationKind` y son idénticas.
- **Los valores del backend se tipan una vez, en la frontera.** El mapper (`data/`) convierte el texto del servidor a
  tipos del dominio (`OperationAction`, `OperationChannel`, `TransferStatus`, `BankBrand`…), conservando el valor
  original cuando no se reconoce (`Other(raw)`). El dominio y la UI nunca comparan nombres ni códigos en texto.
  Cada concepto tiene UNA tabla (códigos de banco: `bankBrandOfCode`; monedas: `sameCurrency`; clasificación del
  resultado de una transferencia: `TransferResultKind`).
- **iOS convierte cada enum de Kotlin a su espejo Swift una sola vez** (`Domain/EzKinds.swift`, sin dependencias del
  framework; la conversión está en `EzKinds+Kotlin.swift`, el único sitio que lee `.name`). `tools/checks/check_enum_parity.py`
  falla si un espejo y su enum Kotlin se separan, si falta una conversión o si algún `.swift` vuelve a comparar nombres
  de enum en texto (`case "X":`, `== "X"`, clave `"X":`, `switch … .name`).
- `tools/checks/check_transfer_presentation.sh` compila y ejecuta los casos de la presentación de iOS sin Xcode ni Kotlin.
- Lo que no se puede leer no se oculta: un saldo ilegible es `null` (se muestra "—") y una operación ilegible se
  cuenta en `skippedOperations` ("No se pudieron mostrar N operaciones").
- Con fuente grande (Android ≥ 1.3 de `fontScale`; iOS tamaños de accesibilidad) las filas se apilan: **un importe
  nunca se trunca ni se parte** (en iOS `EzAmount` se encoge antes de partirse).

### Revisar pantallas sin iniciar sesión (solo debug)

El login real necesita SMS, así que hay un anfitrión de previews que **no entra en release**:
- Android: `adb shell am start -n org.andrew.enzona/.debug.PreviewActivity --es screen detail`
  (`src/debug`; nombres en el comentario de la clase). Modo oscuro y fuente: `adb shell cmd uimode night yes`,
  `adb shell settings put system font_scale 2.0`.
- iOS: `xcrun simctl launch booted org.andrew.enzona.Enzona -ezPreview detail` (`DebugPreview.swift`, `#if DEBUG`).
  Modo oscuro y tamaño: `xcrun simctl ui booted appearance dark`, `xcrun simctl ui booted content_size accessibility-extra-large`.

Y con sesión iniciada, el perfil (Android e iOS) trae **solo en debug** un botón que copia
`access_token`, `refresh_token` e `id_token` en texto plano al portapapeles, para depurar con
curl/Postman. Los tokens se leen al pulsar (nunca antes) y **no pasan por los logs**; en release
la sección no se muestra (Android: bandera `debuggable`; iOS: el archivo entero es `#if DEBUG`).

## 5. Autenticación (verificada contra producción)

- **OAuth2 Authorization Code + PKCE** (cliente público, sin secreto) contra WSO2
  (`identity.enzona.net`). `client_id` propio.
- **Login multi-paso con MFA** (usuario/contraseña → **SMS OTP** → paso custom): lo renderiza
  el IdP como HTML → se abre en el **navegador del sistema**:
  - Android: **Custom Tabs**.
  - iOS: **WKWebView** embebido (ASWebAuthenticationSession no capta el redirect `http://`).
- PKCE S256 se genera en `sharedLogic`; la UI nativa abre el navegador y devuelve el `code`
  del redirect `http://apk-callback?code=…` al `OAuthService` compartido, que canjea el token.
- Token en **SecureStorage** (Keystore/Keychain) + **refresh_token** → sesión persistente;
  el navegador aparece una sola vez.
- El resto de la API es HTTP puro en `sharedLogic` (sin navegador).

> No es posible (ni deseable) saltarse el SMS OTP: ROPC (`grant_type=password`) no lo soporta
> y el SP no lo expone. UI 100% nativa de login solo sería viable si EnZona/XETID habilitara
> "API-based authentication" en su WSO2 para tu `client_id` (gestión con ellos).

### Navegador persistente y dispositivo de confianza (abierto)

- El WebView de iOS usa el almacén **persistente** de la app (`WKWebsiteDataStore.default()`);
  antes era efímero, así que cada login empezaba sin cookies. Android usa Custom Tabs, que
  comparten las cookies del navegador del usuario.
- **Sin verificar**: cómo recuerda el IdP un dispositivo (¿cookie de `identity.enzona.net`?).
  Se comprueba con un login real: el segundo login no debería pedir el SMS.
- **Riesgo**: con cookies persistentes la sesión del IdP también sobrevive. Hoy "Cerrar sesión"
  solo borra los tokens locales, así que quien toque "Iniciar sesión" después podría entrar sin
  credenciales. Hay que decidir el cierre de sesión del IdP (`end_session_endpoint` de OIDC, si
  este WSO2 lo expone; no se pudo consultar, el host no respondió) antes de auto-abrir el login.

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
| Swift interop | **Swift Export** (Kotlin 2.5.0-Beta2, experimental) |
| Auth UI | Custom Tabs (Android) · WKWebView (iOS; ASWebAuthenticationSession no capta `http://`) |
| Navegación | Android: `navigation-compose` 2.10.2 (rutas tipadas) · iOS: `NavigationStack` |
| QR | CameraX + MLKit (Android) · AVFoundation (iOS) |
| Biometría | BiometricPrompt (Android) · LAContext (iOS) |

## 8. Roadmap

| Hito | Entrega | Contenido |
|---|---|---|
| **M0 · Base** | Esqueleto limpio que compila | Retirar `sharedUI`; añadir Ktor/serialization/coroutines/SKIE/settings al catálogo; paquetes `core/data/domain/feature`; Koin en ambos apps |
| **M1 · Auth** | Login real end-to-end (Android + iOS) | PKCE + OAuthService + token store + refresh + `SessionRepository` / `LoginViewModel`; Custom Tabs / ASWebAuthenticationSession; sesión persistente |
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
