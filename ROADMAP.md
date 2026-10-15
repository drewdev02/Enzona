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
- ☑ `SessionRepository` (dueño de la sesión, vida de la app): `SessionState` (StateFlow), PKCE pendiente, restaurar sesión al construirse. `LoginViewModel` (pantalla) solo guarda el estado del intento de login.
- ☑ **Ciclo de vida**: `AccountViewModel` / `CardsViewModel` son ViewModels KMP por pantalla (`viewModelScope`, Koin `viewModel { }`); al cerrar sesión se destruyen y no queda nada del usuario anterior. Ver ARCHITECTURE §4. Tests: `ViewModelLifecycleTest`, `SessionRepositoryTest`, `LoginViewModelTest`.
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

## M3 · Tarjetas + Dashboard  ◧ lista y detalle de tarjeta hechos · faltan acciones y validar con datos reales
- ☑ **Estructura del dashboard** (según el mockup del original, con componentes nativos): cabecera con degradado + Escanear/Recibir, hoja blanca con insignia, barra inferior de 4 pestañas. Android: Compose, píldora propia. iOS: `TabView` nativo.
- ☑ **Cuadrícula de servicios** con los 14 iconos del original (`HomeService` compartido; `tools/extract_assets.py` los importa; `tools/check_assets.py` los verifica). Las casillas de funciones que no existen avisan "próximamente".
- ☑ **Tarjetas** (`feature/cards`): `GET /coremz/1.0.2/card/username/{username}` → `{"card":[…]}`; modelo `BankCard`, `CardsViewModel` (cargando / contenido / vacío / error). Orden: predeterminada primero y luego la más nueva (**inferido** de una captura del original).
- ☑ **Pantalla Balance** = la lista de tarjetas, cada una con el **skin de su banco** (como el original). Se abre desde la casilla "Balance"; sin barra inferior; atrás vuelve.
- ☑ **Skins**: 7 imágenes del original elegidas por `bank_code` (02 AIS · 03 Clásica · 04 BICSA · 05/95 Metropolitano · 06 Bandec · 12 BPA); Bandec usa el skin USD o CUP según la moneda (**inferido**: el original calcula la bandera fuera del despachador). Banco desconocido → fondo propio.
- ☑ **Color del texto por skin, medido sobre los píxeles** (`tools/check_card_skins.py`: texto 4.5:1, número 3:1 por ser texto grande, menú 3:1; modelado el velo de la Bandec Tropical). Se conserva el del original salvo donde no cumplía (Bandec: número negro sobre rojo ≈ 2.3:1). `tarjeta_clasica_ofa.png` no se importa: ningún widget del original la usa.
- ☑ Accesibilidad de la tarjeta: un único elemento con `spokenDescription()` (la imagen no se lee y el número quedaría en trozos); el menú es un botón aparte.
- ☑ Verificado en emulador Android y simulador iOS, claro y oscuro, con datos inventados que cubren los 7 skins y el genérico; y en Android la **navegación real** (Balance → atrás → aviso de otra casilla).
- ⚠ El **menú de tres puntos está dibujado pero es inerte**: muestra "próximamente". Sus acciones (PIN, Balance, Operaciones, Eliminar, Predeterminada) son operaciones nuevas.
- ⚠ **iOS**: la barra de Balance es azul plano, no degradado (la barra nativa no admite degradado), y la hora/iconos de la barra de estado salen negros (ni `toolbarColorScheme` lo cambia en iOS 26). Sobre el azul de marca cumplen contraste (~6:1); **en el inicio, sobre el azul marino, no**: defecto conocido sin resolver.
- ⚠ El margen izquierdo del número en BPA (0.115 del ancho) se fijó **mirando el render**: el triángulo dorado de ese skin no lo detecta ningún verificador. BICSA/BPA referencian `redsa.jpeg` en el original; no sé si lo dibujan encima (aquí no).
- ⚠ **Sin ver con datos reales**: ninguna tarjeta real se ha dibujado (está detrás del login). El mapeo se probó con la **forma** real de la respuesta (datos inventados).
- ☑ **Detalle de tarjeta** (al tocar una tarjeta de Balance): saldo disponible y contable, la fila "Tarjeta" (`BANMET 6856`) y "Últimas operaciones" con los iconos, títulos, signo y Débito/Crédito del original. **Un solo endpoint** `GET /coremz/1.0.2/api/balance/lastOperations/{funding_source_uuid}` da saldo y operaciones (respuesta plana: `balance`, `balance_available`, `currency`, `operation[]`); sacado de **decompilar el original** (`ConsultarSaldoView`, `DescriptionOperations`), documentado en `docs/enzona-kmp/06`. Compartido: `CardsRepository.getActivity`, `CardActivity`/`OperationKind`, `Money` (texto decimal → unidades menores, **sin `Double`**), `classifyOperation` (tabla del original) y `CardDetailViewModel` (uno por pantalla, con el id como parámetro de Koin). Los textos, iconos y el formato (`$3395.15`) viven en la UI de cada plataforma. Importes y datos que no se pueden leer no se ocultan: se muestra "—" o "No se pudieron mostrar N operaciones".
- ☑ Iconos de operación (`op_*`, 7) importados del APK con procedencia y sha256; `tools/check_assets.py` verifica que existen en las dos plataformas y que el mapeo tipo→icono cubre todos los `OperationKind` y es idéntico en Android e iOS.
- ☑ Verificado: 119 tests compartidos (Android + simulador iOS), 10 de presentación de Android, el cálculo de iOS con un script, y la UI en emulador/simulador en claro, oscuro, fuente grande (hasta 200 % / accesibilidad extra grande) y datos parciales. Pantallas con datos inventados vía `PreviewActivity` (Android, solo debug) y `-ezPreview <nombre>` (iOS, solo DEBUG).
- ⚠ **Sin ver con datos reales**: el esquema sale del análisis estático, no de una respuesta viva. **Por confirmar**: si `operationDate` ya viene como `14/10/26` (la app original no lo formatea; aquí se muestra tal cual), si `currency` llega en mayúsculas, el orden y el máximo de operaciones, y la forma de `fault`. Solo los códigos de telefonía (`TEL, RMV, RECA…`) se resuelven por código; GAS/ONAT/… muestran el `alias` del comercio (así lo decodifica el original; pendiente de confirmar con datos).
- ⚠ Al tocar una operación no pasa nada (el detalle de operación es M4) y no hay "tirar para refrescar" (solo reintentar tras un error).
- ☐ Menú de tarjeta: activar/cambiar PIN, eliminar, marcar predeterminada.
- ☐ Insignia de la marca: sigue siendo un placeholder neutro; usar el logotipo propio.

**Done cuando:** saldo y operaciones por tarjeta validados contra la cuenta real, y las acciones del menú funcionando.

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
- ☐ **Icono de la app** (aplazado): hoy Android usa el robot por defecto e iOS no tiene icono. El original solo trae PNG de 192 px (`mipmap/launcher_icon`, nombres ofuscados en el APK) y 257 px (`ic_enzona_new.png`), sin vector ni adaptive icon. Plan: redibujarlo en vector (círculo con degradado + 5 barras "EZ" medidas sobre el PNG), con icono adaptativo + monocromo en Android (primer plano dentro de la zona segura de ~66 dp) y PNG 1024×1024 opaco para iOS (+ variantes oscura y tintada ya declaradas en `Contents.json`, generadas con un script Swift/CoreGraphics porque no hay `rsvg`/`magick`). Registrar la procedencia en `tools/assets_manifest.json` y verificar con capturas del launcher en emulador/simulador. Es marca de EnZona/XETID: no publicar en las tiendas con ella.
- ☑ Observabilidad: logger estilo .NET (`core/logging`, Kermit debajo) con `Sensitive<T>`, logging de red sin ruta ni cabeceras y tests de redacción (ver ARCHITECTURE §4). Pendiente: sink a archivo / "compartir logs", y la regla keep si se activa R8.

## Orden recomendado
**M0 → M1** primero (prueban toda la arquitectura). Luego M2 como plantilla de feature;
M3–M6 repiten el patrón sobre endpoints ya verificados.
