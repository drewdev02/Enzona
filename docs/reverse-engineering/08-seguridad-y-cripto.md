# 08 — Seguridad y criptografía

> Resultado del análisis del binario decompilado (Blutter, Dart 3.3.4). Responde a la
> pregunta clave para la reimplementación: **¿hace falta algún token secreto o firma
> especial para hablar con el backend?** → **No.** Toda la autenticación es OAuth2 estándar
> del usuario; no hay API key, ni client_secret, ni firma HMAC de requests, ni certificate
> pinning. Lo único criptográfico del lado cliente usa una **clave pública** (no secreta).

## TL;DR

| ¿Se necesita…? | Respuesta | Evidencia |
|---|---|---|
| API key / subscription key | **No** | Único header auth: `Authorization: Bearer` (13 usos); no hay `X-Api-Key`, `Ocp-Apim-*`, etc. |
| client_secret OAuth | **No** | Flujo **PKCE público** (`code_verifier`/`code_challenge` S256) |
| Firma HMAC de requests | **No** | No hay header `Signature`/`hmac` ni lógica de firma de petición |
| Certificate pinning | **No** | Sin `badCertificate`/`CertificatePinning`/pins sha256 en el binario |
| Secreto embebido | **No** | Lo único empaquetado es una **clave pública** RSA 2048 |
| Token del usuario (Bearer) | **Sí** | Se obtiene con login OAuth real |
| Cifrar con clave pública datos de tarjeta/pass | **Sí** | RSA con `enzona_pubkey.pem` (clave pública) |

## 1. Autenticación: OAuth2 Authorization Code + PKCE

- IdP: WSO2 Identity Server (`identity.enzona.net`), SP `admin_ppago-apk_PRODUCTION`, tenant `carbon.super`.
- **PKCE público**: no hay secreto de cliente. El cliente genera `code_verifier` y
  `code_challenge` (método `S256`). Ver flujo completo en `07-endpoints-request-mapping.md §0`.
- La API de negocio (`api.enzona.net`) autoriza **solo** con `Authorization: Bearer <access_token>`.
- Renovación con `grant_type=refresh_token` contra `/oauth2/token` (y `/oauth/token` interno).

**Implicación para KMP**: cualquier cliente con un `client_id` válido y un usuario que se
autentique de verdad obtiene acceso. No hay llave maestra que extraer ni que puedas "robar"
del APK — y eso es correcto por diseño.

## 2. `transaction_signature` — la firma la emite el SERVIDOR

Aparece como **campo de respuesta** en tarjetas, transferencias, facturas, QR y
transacciones. Es un comprobante de integridad que **genera y devuelve el backend**; el
cliente solo lo recibe, lo guarda y lo muestra en el comprobante. **No** hay que calcularlo
ni firmarlo en el cliente. (Confianza alta: nunca se construye en los datasources, solo se
lee del JSON de respuesta.)

## 3. Cifrado de datos sensibles (`cadenaEncript`) — con clave PÚBLICA

Clase `RSAEncrypt` (`core/encrypt/encryptFromCrt.dart`) + `ExtractPublicKeyFromCert`.

- **Clave**: `assets/flutter_assets/assets/raw/enzona_pubkey.pem` — **RSA 2048 pública**
  (`-----BEGIN PUBLIC KEY-----`). Es pública: no es un secreto, va en claro dentro del APK.
- **Qué se cifra**:
  - Al **asociar tarjeta**: `pan` + `expdate` (datos de la tarjeta) → `cadenaEncript`.
  - Al **pagar/transferir**: la **contraseña de pago** (`payment_password`) → se envía cifrada.
  - También se usa en donación, giro, regalo y reset de contraseña de pago.
- **Esquema**: RSA con el paquete Dart `encrypt` (usa `RSAKeyParser` para leer la PEM).
  ✅ **Padding confirmado por decompilación: PKCS1 v1.5** (`cifrar()` en `core/helpers/functions.dart` construye
  el cifrador con el valor `RSAEncoding.PKCS1` del object pool; no es OAEP) y devuelve el resultado en **base64**.
  Que el servidor lo acepte sigue sin verificarse en vivo (solo se verifica con una operación real).

> 🔎 **La rama de huella NO es un simple booleano** (`getSignature`, `core/helpers/functions.dart`): lee `username` y
> `pvKey` del almacenamiento seguro y llama a un **canal nativo `getFingerPrint`** que **firma con una clave privada
> del dispositivo** (registrada al activar la huella). Las transferencias mandan `fingerprint` y, si no hay huella,
> `payment_password` cifrada. Replicar la huella exige enrolar un par de claves en el dispositivo; la contraseña de
> pago no.

**Implicación para KMP**: reproduce RSA-encrypt con la clave pública. No necesitas la clave
privada (la tiene el servidor para descifrar). Si levantas tu **propio backend**, generas tu
propio par y empaquetas tu clave pública igual.

## 4. Almacenamiento seguro (lo que persiste el cliente)

`flutter_secure_storage` (Keystore/Keychain), vía `core/services/local_storage.dart` y
`core/helpers/storage.dart`. Claves observadas en `security_local_datasource`:

| Clave | Contenido |
|---|---|
| `token` | TokenModel (access/refresh/…) |
| `profile` | perfil OIDC del usuario |
| `codeVerifier` | PKCE verifier (durante el login) |
| `codeChallenge` | PKCE challenge |
| `authCode` | código de autorización capturado del redirect |
| `expTime` | expiración del token |

## 5. Biometría y 2FA

- **Biometría** (`local_auth`, permisos `USE_BIOMETRIC`/`USE_FINGERPRINT`): desbloqueo de
  app y confirmación de operaciones. El flag `fingerprint` en `AccountModel` indica si el
  usuario lo tiene activo.
- **TOTP** propio: `account/v1.0.0/account/TOTP/init|validate|disable` (segundo factor).
- **Contraseña de pago** independiente de la de acceso, cifrada antes de enviarse (§3).

## 6. Transporte

- HTTPS a `api.enzona.net` / `identity.enzona.net` (TLS del sistema; **sin pinning**).
- `Content-Type: application/json` en la API; `application/x-www-form-urlencoded` en el token OAuth.

---

## 7. Checklist de seguridad para el cliente KMP

- [ ] OAuth2 Authorization Code + **PKCE S256** (Ktor + Custom Tabs / ASWebAuthenticationSession).
- [ ] Guardar `access/refresh token` y PKCE en **Keystore/Keychain** (`expect SecureStorage`), nunca en claro ni en logs.
- [ ] Refresh automático del token en el plugin `Auth` de Ktor.
- [x] **Contraseña de pago**: `PaymentPasswordEncryptor` (`core/crypto`): RSA PKCS#1 v1.5 con la clave pública del APK, base64. La PEM se interpreta una vez (`RsaPublicKey`: SPKI para Android, PKCS#1 para iOS); `expect fun rsaEncryptPkcs1v15` usa `Cipher` (Android) y `SecKeyCreateEncryptedData` (iOS). Probado con ida y vuelta contra la clave privada en ambas plataformas y comprobando que NO es OAEP. Pendiente de verificar contra el servidor real.
- [ ] Cifrar `pan`+`expdate` (otros flujos) con el mismo mecanismo si hace falta.
- [x] Clave pública empaquetada como constante (`ENZONA_PUBLIC_KEY_PEM`, copiada del APK v20003).
- [ ] Biometría (`expect Biometrics`: BiometricPrompt / LAContext) para confirmar pagos.
- [ ] Soporte TOTP para 2FA.
- [ ] No implementar firma de request ni pinning salvo que tu backend lo exija (el original no lo hace).

## 8. Nota ética

No se extrajo ni se incluye aquí ninguna clave privada, credencial ni secreto: el único
material criptográfico embebido en el APK es una clave **pública**, que por definición no es
confidencial. Para operar contra producción se necesita un usuario real autenticándose en
EnZona; esta documentación no elude esa autenticación ni la de pago. Usa tu propio `client_id`
y, preferiblemente, tu propio backend de pruebas.
