# 01 — Arquitectura

## 1. Arquitectura original (Flutter)

### 1.1 Capas (Clean Architecture por feature)

Cada módulo en `lib/app/modules/<feature>/` repite el mismo esqueleto:

```
<feature>/
  bindings/        → <feature>_binding.dart   (inyección de dependencias GetX)
  controllers/     → <feature>_controller.dart (presentación / estado reactivo)
  data/
    datasources/   → <feature>_remote_datasource.dart  (llamadas HTTP)
                     <feature>_local_datasource.dart   (cache/secure storage)
    models/        → *_model.dart   (DTO: fromJson/toJson)
    repositories/  → <feature>_repository_impl.dart
  domain/
    entities/      → *.dart         (entidad pura)
    repositories/  → <feature>_repository.dart (interfaz)
    usecase/       → *_use_case.dart (una acción de negocio)
  views/           → *_view.dart    (pantallas)
  widgets/         → componentes reutilizables del feature
```

Flujo de una acción:

```
View → Controller (GetX) → UseCase → Repository(interfaz)
     → RepositoryImpl → RemoteDataSource (dio/http) → api.enzona.net
                      → LocalDataSource (flutter_secure_storage)
```

### 1.2 Núcleo compartido (`lib/app/core/`)

| Submódulo | Ficheros clave | Rol |
|---|---|---|
| `services/` | `local_storage.dart`, `store_service.dart`, `permission_service.dart`, `read_write_service.dart`, `download_manager_service.dart`, `flutter_downloader_manager.dart` | Almacenamiento, permisos, descargas |
| `helpers/` | `network_info_service.dart`, `storage.dart`, `path_service.dart`, `card_type.dart`, `card_utils.dart`, `formatters.dart`, `mobile_info_utils.dart`, `functions.dart`, `extensions.dart`, `assets.dart` | Utilidades transversales |
| `encrypt/` | `encryptFromCrt.dart`, `extractPublicKeyFromCrt.dart` | Cifrado con clave pública extraída de un certificado (`.crt`) — para proteger datos sensibles (p. ej. contraseña de pago) antes de enviarlos |
| `constants/` | `main_constants.dart`, `app_colors.dart` | URLs base, colores |
| `patterns/` | `usecase.dart` | Clase base `UseCase<Type, Params>` (patrón) |
| `errors/` | `Failure`/`Exception` | Manejo de errores (dartz `Either<Failure, T>`) |

### 1.3 Rutas

- `lib/app/routes/app_pages.dart` — rutas nombradas GetX (`GetPage(name, page, binding)`).
- `lib/main.dart` — arranque, `GetMaterialApp`, localización (`flutter_localizations`, `intl`).
- Splash: `security/views/splash_view.dart` → decide a login (webview OAuth) o dashboard.

### 1.4 Seguridad / identidad

- **OAuth2 Authorization Code + PKCE** contra WSO2 Identity Server (`identity.enzona.net`).
  - Autorización embebida en **WebView** (`flutter_inappwebview`) que captura el redirect
    `http://apk-callback?code=...`.
  - `package:pkce` genera `code_verifier`/`code_challenge`.
  - Token guardado en `security/data/models/token_model.dart` y persistido en
    **`flutter_secure_storage`** (Keychain/Keystore).
- **Biometría**: `package:local_auth` para desbloquear la app y confirmar pagos.
- **Cifrado**: `encrypt` + `pointycastle` + `asn1lib`/`basic_utils` para RSA/AES;
  la contraseña de pago se cifra con la clave pública del servidor antes de enviarse.
- **TOTP**: segundo factor propio (`account/.../TOTP/init|disable`, `get_totp_code_use_case`).

## 2. Arquitectura propuesta en KMP (equivalente)

Mantener Clean Architecture; mover a `commonMain` toda la lógica, dejar en `androidMain`/
`iosMain` solo lo que toca plataforma (biometría, cámara QR, secure storage, notificaciones).

```
shared/ (Kotlin Multiplatform)
  commonMain/
    core/
      network/      → Ktor HttpClient + AuthПlugin (token refresh), interceptores
      storage/      → multiplatform-settings / expect SecureStorage
      crypto/       → expect RsaEncryptor (clave pública del .crt)
      di/           → Koin modules
      result/       → Result<Failure, T> (análogo a Either)
    feature/<feature>/
      data/ (dto, remoteDataSource, repositoryImpl)
      domain/ (entity, repository interface, usecase)
      presentation/ (ViewModel/StateFlow — reemplaza al GetX Controller)
  androidMain/ → SecureStorage(Keystore), BiometricPrompt, CameraX QR, FCM/ntfy
  iosMain/     → SecureStorage(Keychain), LAContext, AVFoundation QR, APNs

androidApp/  → Jetpack Compose (consume shared)
iosApp/      → SwiftUI (consume shared via KMP framework)
```

Equivalencias de patrón:

| Flutter (original) | KMP (propuesto) |
|---|---|
| GetX `Controller` + `Rx` | `ViewModel` + `StateFlow`/`MutableStateFlow` |
| GetX `Binding` | Koin `module { }` |
| GetX rutas nombradas | Navigation Compose / SwiftUI NavigationStack (o Decompose/Voyager multiplataforma) |
| `dio`/`http` | **Ktor client** (`ContentNegotiation` + `kotlinx.serialization`) |
| `dartz Either<Failure,T>` | sealed `Result`/`Either` propio o Arrow `Either` |
| `flutter_secure_storage` | `expect/actual SecureStorage` (Keystore / Keychain) |
| `local_auth` | `expect Biometrics` (BiometricPrompt / LAContext) |
| `qr_code_scanner` + `qr_flutter` | CameraX+MLKit / AVFoundation + lib QR; generación con zxing-kotlin |
| `flutter_inappwebview` (OAuth) | AndroidX Custom Tabs + ASWebAuthenticationSession (iOS) |
| `pkce` | AppAuth / implementación PKCE propia en commonMain |
| `encrypt`+`pointycastle` | `expect RsaEncryptor` o korlibs/krypto |
| `intl`/`flutter_localizations` | moko-resources / Compose i18n |
| `json_annotation` | `kotlinx.serialization` |

### UI
No compartir UI inicialmente (Flutter la tenía unificada; en KMP lo natural es Compose +
SwiftUI por separado, o **Compose Multiplatform** si se quiere UI única). La lógica
(`shared`) es 100% reutilizable en ambos casos.
