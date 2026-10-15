# 05 — Mapeo Flutter → KMP y plan de implementación

## 1. Dependencias: equivalente KMP para cada paquete Flutter observado

| Paquete Flutter (original) | Rol | Equivalente KMP |
|---|---|---|
| `get` | Estado, DI, rutas | ViewModel + StateFlow; **Koin** (DI); Voyager/Decompose (nav) |
| `dio`, `http`, `http_parser` | HTTP | **Ktor client** (+ `kotlinx.serialization`, `ContentNegotiation`) |
| `json_annotation` | JSON | `kotlinx.serialization` |
| `dartz` | `Either/Failure` | Arrow `Either` o `Result` sealed propio |
| `equatable` | `==`/hashCode | `data class` (gratis en Kotlin) |
| `flutter_secure_storage` | Secreto | `expect/actual SecureStorage` (Keystore / Keychain) |
| `local_auth` (+ `_android`) | Biometría | `expect Biometrics` (BiometricPrompt / LAContext) |
| `pkce` | OAuth PKCE | AppAuth-Android + AppAuth-iOS, o PKCE propio en commonMain |
| `encrypt`, `pointycastle`, `asn1lib`, `basic_utils`, `crypto` | RSA/AES/hash | `korlibs/krypto` o `expect RsaEncryptor` (Security framework / JCA) |
| `qr_code_scanner`, `scan` | Escanear QR | CameraX + MLKit Barcode (Android) / AVFoundation (iOS) |
| `qr_flutter`, `qr` | Generar QR | zxing-kotlin (common) o `expect QrRenderer` |
| `flutter_inappwebview` | WebView OAuth | Custom Tabs / ASWebAuthenticationSession |
| `flutter_local_notifications` | Notif. locales | `expect Notifier` (NotificationManager / UNUserNotificationCenter) |
| `connectivity_plus` | Red | `expect Connectivity` (ConnectivityManager / NWPathMonitor) |
| `device_info_plus`, `platform` | Info dispositivo | moko-device / `expect DeviceInfo` |
| `path_provider`, `path`, `media_store_plus`, `open_file`, `flutter_downloader` | FS/descargas | `expect FileStorage` + Ktor download; MediaStore / Files app |
| `image_picker`, `image_cropper`, `image_gallery_saver`, `cross_file` | Imágenes | `expect ImagePicker` (Photo Picker / PHPicker) |
| `flutter_native_contact_picker` | Contactos | `expect ContactPicker` (Contacts / CNContactPicker) |
| `permission_handler` | Permisos | `expect Permissions` (Accompanist / solicitudes nativas) |
| `url_launcher` | Enlaces | `expect UrlLauncher` (Intent / UIApplication.open) |
| `google_fonts`, `font_awesome_flutter`, `animate_do`, `flutter_rating_stars`, `vector_graphics` | UI | Compose/SwiftUI nativo; fuentes empaquetadas; Lottie/animaciones |
| `intl`, `flutter_localizations` | i18n/formato | moko-resources / kotlinx-datetime + formateadores |
| `web_socket_channel`, `web_socket` | WebSocket | Ktor WebSockets |
| `collection`, `async`, `convert`, `typed_data`, etc. | utilidades Dart | stdlib Kotlin / kotlinx |

## 2. Estructura de proyecto KMP sugerida

```
enzona-kmp/
  shared/
    src/commonMain/kotlin/cu/enzona/
      core/{network,storage,crypto,di,result,i18n}
      feature/{security,account,card,transfer,scanear,bankaccount,
               services/{etecsa,electricity,gas,tax},
               friends,bulevar,cubanemail,donation,gift,
               transaction,home,notification,social}
        <feature>/data/{dto,remote,local,repository}
        <feature>/domain/{entity,repository,usecase}
        <feature>/presentation/{viewmodel,state}
    src/androidMain/... (actual SecureStorage, Biometrics, QR, Notifier)
    src/iosMain/...     (actual Keychain, LAContext, AVFoundation, APNs)
  androidApp/  (Compose)
  iosApp/      (SwiftUI; consume el framework de shared)
```

## 3. Orden de implementación recomendado (hitos)

1. **M0 – Infra**: Ktor + auth OAuth/PKCE + secure storage + DI + `Result`. (EPIC-01)
2. **M1 – Cuenta**: login, perfil, verificación, contraseña de pago, TOTP. (EPIC-02/03)
3. **M2 – Núcleo de dinero**: tarjetas + transacciones + home/dashboard. (EPIC-04/11/12)
4. **M3 – Transferencias + QR**: transfer a cuenta/tarjeta/Fincimex, escanear/generar QR. (EPIC-05/06)
5. **M4 – Servicios**: ETECSA, electricidad, gas, impuestos (una plantilla, 4 instancias). (EPIC-08)
6. **M5 – Social y extras**: amigos, destinatarios, bulevar, giros, donaciones, gift,
   organizaciones sociales. (EPIC-09/10)
7. **M6 – Plataforma**: notificaciones, descargas de comprobantes, auto-update, rating, offline. (EPIC-13)

## 4. Cliente HTTP (esqueleto Ktor)

```kotlin
fun httpClient(tokenStore: TokenStore) = HttpClient {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(Auth) {
        bearer {
            loadTokens { tokenStore.load()?.let { BearerTokens(it.access, it.refresh) } }
            refreshTokens {
                // POST https://identity.enzona.net/oauth2/token  grant_type=refresh_token
                tokenStore.refresh()?.let { BearerTokens(it.access, it.refresh) }
            }
            sendWithoutRequest { it.url.host == "api.enzona.net" }
        }
    }
    defaultRequest { url("https://api.enzona.net/") }
}
```

## 5. Notas de seguridad para la reimplementación

- La **contraseña de pago** se cifra con la clave pública del servidor (patrón
  `core/encrypt`: cargar `.crt` → extraer public key → RSA encrypt) **antes** de enviarse.
  Replica ese cifrado en `expect RsaEncryptor`; nunca envíes la pass en claro.
- Guarda tokens y datos sensibles solo en Keystore/Keychain, nunca en logs ni en
  `multiplatform-settings` sin cifrar.
- Confirmación de operaciones de dinero: biometría y/o TOTP, igual que el original.
- Usa **tu propio `client_id` OAuth** y tu propio backend de pruebas. El `client_id`,
  los hosts y el flujo aquí documentados describen la app oficial para fines de
  interoperabilidad/estudio; no los uses para suplantar ni para operar contra producción
  sin autorización de EnZona/XETID.

## 6. Qué queda por confirmar (gaps del análisis estático)

- **Esquemas exactos** de request/response (nombres de campos, enums de estado, monedas).
- **Códigos de error** y su mapeo a `Failure`.
- **Headers** adicionales (api-key, device-id, firma de request) si los hubiera.
- **Paginación** de históricos.
- Para cerrarlos: capturar tráfico real con un proxy en una cuenta propia de pruebas, o
  decompilar funciones concretas de `libapp.so` con **Blutter/reFlutter** (recuperan nombres
  y, parcialmente, cuerpos del snapshot Dart AOT a partir del snapshot hash).
```
