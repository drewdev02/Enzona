# 02 — Referencia de API

> Endpoints descubiertos por análisis estático de `libapp.so`. Las versiones de path
> (`v1.0.0`, `1.0.2`, `1.0.1`, `1.0`) son las observadas en el binario. Los **cuerpos de
> request/response no se recuperan del AOT**: se infieren del nombre del endpoint, del use
> case que lo invoca y de las entidades del módulo (ver `04-modelo-datos.md`). Trátalos
> como contrato aproximado a confirmar contra el backend con credenciales válidas.
>
> ✅ Pasada de verificación en vivo (solo GET, 2026-10-15, sesión propia): 64 capturas
> y correcciones en [`11-capturas-live.md`](11-capturas-live.md).

## 0. Autenticación — OAuth2 Authorization Code + PKCE

IdP: **WSO2 Identity Server** en `https://identity.enzona.net`.

| Paso | Detalle observado |
|---|---|
| Authorize | `GET https://identity.enzona.net/oauth2/authorize?response_type=code&client_id=<CLIENT_ID>&scope=openid&redirect_uri=http://apk-callback&code_challenge=<...>&code_challenge_method=S256` |
| Redirect de captura | `http://apk-callback?code=<AUTH_CODE>` (interceptado en WebView) |
| Token | `POST https://identity.enzona.net/oauth2/token` (`grant_type=authorization_code`, `code`, `code_verifier`, `redirect_uri`, `client_id`) |
| Refresh | `POST .../oauth2/token` (`grant_type=refresh_token`) |
| Error | `.../authenticationendpoint/oauth2_error.do` |
| Privacidad | `.../authenticationendpoint/privacy_policy.do` |

- **PKCE** (`code_challenge_method=S256`) vía `package:pkce`.
- `scope=openid` (OIDC). El `client_id` es específico del cliente oficial — **usa el tuyo**.
- Token (`access_token`, `refresh_token`, `expires_in`, `id_token`) → `token_model.dart`,
  persistido en secure storage. Las llamadas a `api.enzona.net` van con `Authorization: Bearer`.

**KMP**: Custom Tabs (Android) + `ASWebAuthenticationSession` (iOS) para el authorize;
Ktor para token/refresh; plugin de Ktor `Auth`/`bearer` con renovación automática.

---

## 1. Servicio `account` — Cuenta, perfil, 2FA (`account/v1.0.0`)

| Método | Path | Use case / uso |
|---|---|---|
| GET | `/account/v1.0.0/account` | Obtener datos de la cuenta (`get_account_model_use_case`) |
| PUT | `/account/v1.0.0/account` | Actualizar cuenta/perfil (`update_account_use_case`) |
| POST | `/account/v1.0.0/account/authentication` | Autenticación adicional de cuenta |
| PUT | `/account/v1.0.0/account/password` | Cambiar contraseña de acceso (`change_account_pass_use_case`) |
| POST | `/account/v1.0.0/account/paymentpassword/reset` | Resetear contraseña de pago (`reset_payment_password_use_case`) |
| POST | `/account/v1.0.0/account/phone/code/number` | Enviar código al teléfono (`send_verify_phone_code_use_case`) |
| POST | `/account/v1.0.0/account/phone/verified` | Verificar teléfono (`verify_phone_use_case`) |
| POST | `/account/v1.0.0/account/verify` | Verificar cuenta (`verify_account_use_case`, `verify_code_use_case`) |
| POST | `/account/v1.0.0/account/TOTP/init` | Inicializar TOTP (`get_totp_code_use_case`, `get_verify_totp_use_case`) |
| POST | `/account/v1.0.0/account/TOTP/disable` | Desactivar TOTP (`disable_totp_use_case`) |

Relacionados (otros servicios usados por el módulo account):
- Contactos/destinatarios → `contact/1.0/...` y `recipent/1.0.1/...` (abajo).
- `send_verify_code_usecase`, `update_email_use_case`, `update_email_phone_usecase`.

## 2. Servicio `cards` / `coremz` — Tarjetas

| Método | Path | Uso |
|---|---|---|
| GET/PUT | `/cards/v1.0.0/cards/default` | Tarjeta por defecto |
| ... | `/coremz/1.0.2/card` | Listar/gestionar tarjetas |
| POST | `/coremz/1.0.2/card/card/activate/pin` | Activar tarjeta por PIN |
| POST | `/coremz/1.0.2/api/account/updateAnyToUser` | Asociar recurso a usuario |

## 3. Servicio `transfer` — Transferencias (`transfer/v1.0.0`, `coremz/1.0.2`)

| Método | Path | Use case |
|---|---|---|
| POST | `/transfer/v1.0.0/transfer/account` | Transferir a cuenta EnZona (`transfer_to_account_use_case`) |
| POST | `/transfer/v1.0.0/transfer/card` | Transferir a tarjeta (`transfer_to_card_use_case`) |
| POST | `/coremz/1.0.2/transfer/fincimex` | Transferir a tarjeta Fincimex (`transfer_to_card_fincimex_use_case`) |
| POST | `/coremz/1.0.2/qr/postFincimex` | Pago Fincimex por QR (`add_payment_fincimex_use_case`) |
| — | (local) | Filtrar transferencias (`filter_transfer_use_case`) |
| — | (via scanear) | Pagar a comercio por QR escaneado (`pay_to_comercio_by_qr_scanned_use_case`) |

## 4. Servicio `qr` + `payment` + `scanear` — QR y pagos a comercio

| Método | Path | Use case |
|---|---|---|
| POST | `/qr/v1.0.0/qr/account` | Generar QR de cobro propio (⚠ GET da 404/fault 4016) |
| GET | `/payment/v1.0.0/payments/vendor/code` | Obtener código de vendedor (`get_vendor_id_code_use_case`) |
| POST | `/invoice/v1.0.0/invoice/payment` | Pagar factura/orden |
| GET | `/invoice/v1.0.0/client-id` | Client-id de factura |
| POST | (scanear) | Pago por QR (`add_payment_qr_code_use_case`) |
| POST | `service-core.enzona.net/atm` | Extracción por QR en ATM (`add_atm_extraction`) |

## 5. Servicio `recipent` / `contact` — Destinatarios y contactos

| Método | Path | Use case |
|---|---|---|
| POST | `/recipent/1.0.1/recipient/add` | Añadir destinatario (`add_destinatario_use_case`) |
| POST | `/recipent/1.0.1/recipient/setRecipient` | Actualizar destinatario (`update_destinatarios_use_case`) |
| POST | `/contact/1.0/api/account/contact-add` | Añadir contacto/amigo (`add_friend_use_case`) |
| POST | `/contact/1.0/api/account/contact-remove-request` | Eliminar contacto (`delete_friend_use_case`, `delete_destinatario_use_case`) |

(Listado/filtros de amigos: `get_solicitudes_use_case`, `filter_friend_use_case`,
`filter_account_by_username_use_case`, `get_destinatarios_use_case`.)

## 6. Servicio `nomenclator` — Catálogos

| Método | Path | Uso |
|---|---|---|
| GET | `/nomenclator/v1.0.0/nomenclator/banks` | Lista de bancos |
| GET | `/nomenclator/v1.0.0/nomenclator/coordinates` | Coordenadas/ubicaciones |

## 7. Servicio `bulevar` — Tiendas / marketplace

| Método | Path | Use case |
|---|---|---|
| GET | `/bulevar/1.0/tiendas/allTiendas` | Listar tiendas/comercios (`list_merchant_use_case`) |
| — | (local) | Filtrar órdenes (`filter_order_use_case`) |

## 8. Pago de servicios (ETECSA, electricidad, gas, impuestos)

Estos módulos reutilizan el patrón factura (`invoice`) y catálogos. Use cases observados:

| Servicio | Use cases |
|---|---|
| `etecsa` (Nauta/móvil) | `list_client_id_use_case`, `add_client_id_use_case`, `delete_client_id_use_case`, `get_fact_mensual_etecsa_use_case`, `filter_invoices_use_case`, `pay_fact_etecsa_use_case` |
| `electricity_service` | (análogo: añadir id cliente, listar/buscar facturas, pagar, pago automático) |
| `gas_service` | (análogo: configurar cliente, listar/buscar facturas de gas, pagar) |
| `tax_service` (ONAT/RC-05) | add RC-05, cuenta fiscal, buscar/pagar impuesto, histórico |

> Estos pagos de factura van contra `invoice/v1.0.0/invoice/payment` y endpoints de
> nomenclador; cada servicio añade su "id de cliente" (número de contrato/servicio).

## 9. Otros

| Área | Endpoint / nota |
|---|---|
| `help` / soporte | `/coremz/1.0.2/help/contacts` (`contactenos_use_case`) |
| Fiscal / bancaria | `fiscal_account_remote_datasource` (vincular cuenta fiscal) |
| Avatares / media | `https://media.enzona.net/images/user/avatar/<id>` |
| Push | `POST http://nyfy-push.enzona.net/ntfy/send` (ntfy) |
| Auto-update | `GET http://util.enzona.net/enzona.apk` (`update_apk_remote_datasource`) |
| Rating | `https://api.apklis.cu/` (`rating_remote_datasource`) |
| Reclamaciones | `claim_remote_datasource` |
| Giros | `cuban_email_remote_datasource` (money order / giro) |
| Donaciones | `donation_remote_datasource` |

## Convenciones generales (a validar)

- **Base**: `https://api.enzona.net`. **Auth**: `Authorization: Bearer <access_token>`.
- Content-Type `application/json` (DTOs con `json_annotation`).
- Operaciones de dinero requieren **contraseña de pago** (cifrada con RSA, ver core/encrypt)
  y/o **biometría** / **TOTP** como confirmación.
- Errores mapeados a `Failure` (dartz) en cada repositorio.
