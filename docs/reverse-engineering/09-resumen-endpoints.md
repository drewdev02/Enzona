# 09 — Resumen: endpoint → envías / hace / recibes

> Vista rápida de cada endpoint: **qué se envía**, **qué acción realiza** y **qué se recibe**.
> Todas las llamadas a `api.enzona.net` llevan `Authorization: Bearer <access_token>` (salvo el login).
> Detalle de campos en [`06-esquemas-exactos.md`](06-esquemas-exactos.md) y [`07-endpoints-request-mapping.md`](07-endpoints-request-mapping.md).

## Autenticación (`identity.enzona.net`)

| Endpoint | Envías | Acción | Recibes |
|---|---|---|---|
| `GET /oauth2/authorize` | `client_id`, `scope=openid`, `redirect_uri`, `code_challenge` (S256) | Login del usuario en WebView | redirect `apk-callback?code=…` |
| `POST /oauth2/token` (authorization_code) | `client_id`, `code`, `code_verifier`, `redirect_uri` | Canjea el code por token | `access_token`, `refresh_token`, `expires_in`, `id_token` |
| `POST /oauth2/token` (refresh_token) | `client_id`, `refresh_token` | Renueva sesión | nuevos tokens |

## Cuenta, perfil y verificación (`account/v1.0.0`)

| Endpoint | Envías | Acción | Recibes |
|---|---|---|---|
| `GET /account?username=` | username | Obtener perfil | `AccountModel` (uuid, name, email, phone, verified, fingerprint, merchants…) |
| `PUT /account` | AccountModel | Editar perfil | perfil actualizado |
| `PUT /account/password` | `payment_password`, `old_payment_password` (**cifrados**) | Cambiar contraseña de pago | estado |
| `POST /account/paymentpassword/reset` | datos de reset | Resetear contraseña de pago | estado |
| `POST /account/phone/code/number` | `phone` | Enviar SMS de verificación | estado |
| `POST /account/phone/verified` | `phone`, `code` | Verificar teléfono | estado |
| `POST /account/verify` | `code` | Verificar cuenta | estado |
| `POST /account/TOTP/init` · `GET /TOTP/validate` · `POST /TOTP/disable` | código TOTP | Activar/validar/desactivar 2FA | secreto TOTP / estado |

## Tarjetas (`coremz/1.0.2`, `cards/v1.0.0`)

| Endpoint | Envías | Acción | Recibes |
|---|---|---|---|
| `GET /coremz/1.0.2/card/username/{username}` ✅ | username | Listar tarjetas | `{"card":[…]}` (last4, expdate, cardholder, bank_name, currency, primary_source) |
| `POST` añadir tarjeta | `cadenaEncript` (pan+expdate **cifrados**) | Asociar tarjeta | tarjeta + `transaction_signature` |
| `GET /cards/v1.0.0/cards/default` | — | Tarjeta por defecto | CardModel |
| `POST /coremz/1.0.2/card/card/activate/pin` | PIN | Activar tarjeta | estado |
| `GET /coremz/1.0.2/api/balance/lastOperations/{funding_source_uuid}` 🔎 | uuid de la tarjeta | Saldo **y** últimas operaciones (un solo endpoint) | `balance`, `balance_available`, `currency`, `bank_account`, `operation[]` (ver `06 §Saldo`). Verificado por decompilación, no en vivo |

## Transferencias (`transfer/v1.0.0`, `coremz/1.0.2`)

| Endpoint | Envías | Acción | Recibes |
|---|---|---|---|
| `POST /transfer/account` | `funding_source_uuid`, `funding_source_amount`, `recipient_username`, `payment_password` (**cifrada**) | Transferir a cuenta EnZona | `id_transfer`, `transaction_code`, `transaction_signature`, `tipo_cambio` |
| `POST /transfer/card` | `funding_source_uuid`, `panRecipient`, monto, `payment_password` | Transferir a tarjeta | igual + `status_code`/`status_denom` |
| `POST /coremz/1.0.2/transfer/fincimex` | datos tarjeta Fincimex, monto | Transferir a tarjeta Fincimex | comprobante |
| `POST /coremz/1.0.2/qr/postFincimex` | datos QR Fincimex | Pago Fincimex por QR | comprobante |
| `GET /transfer?…` | filtros/fechas | Historial de transferencias | lista |

## QR y pago a comercio (`qr/v1.0.0`, `payment/v1.0.0`, `service-core`)

| Endpoint | Envías | Acción | Recibes |
|---|---|---|---|
| `GET /qr/v1.0.0/qr/account` | — | Generar **tu** QR de cobro | `qr_code`, `vendor_identity_code`, `merchant_uuid` |
| `POST /qr/v1.0.0/qr/…` | `qr_code`, `merchant_uuid`, `payment_password` | Pagar a comercio escaneado | `transaction_status`, `transaction_signature` |
| `GET/POST /payment/v1.0.0/payments/vendor/code` | `vendor_identity_code` | Resolver código de vendedor | datos del comercio (`merchant_alias`, `require_password`) |
| `POST service-core.enzona.net/atm` | `qr_code`, `cash_advance` | Extracción en ATM por QR | comprobante |

## Pago de servicios — patrón único (`invoice/v1.0.0`; impuestos en `onat/v1.0.0`)

Reutilizado por **ETECSA/Nauta, electricidad, gas** (varía el `serviceUuid`/`service_code`):

| Endpoint | Envías | Acción | Recibes |
|---|---|---|---|
| `GET/POST /invoice/client-id` | `client_id`, `serviceUuid` | Guardar/listar id de cliente | ids guardados |
| `GET /invoice?…` · `/invoice/search?…` | filtros | Buscar/listar facturas | `InvoiceModel[]` (total_amount, period, status) |
| `GET /invoice/{id}` | — | Detalle de factura | InvoiceModel |
| `POST /invoice/payment` | `charged_invoice`, `client_id`, `importe_a_debitar`, `funding_source_uuid`, `payment_password` | **Pagar factura** | `transaction_code`, `transaction_signature`, `transaction_status` |
| `GET /onat/v1.0.0/tax-payer?` · `/tax` | `taxPayer`, `client_id` | Consultar/pagar impuesto ONAT | datos fiscales / comprobante |

## Cuentas bancaria/fiscal (`coremz/1.0.2/fiscal_account`)

| Endpoint | Envías | Acción | Recibes |
|---|---|---|---|
| `POST /fiscal_account` · `GET /all/` | datos cuenta | Crear/listar cuenta fiscal | cuentas |
| `GET /allProvince` · `/municipe?province_code=` · `/branch?code_dpa=` | códigos | Catálogos geográficos/bancarios | provincias/municipios/sucursales |
| `GET /is_contribuyente?account_uuid=` · `/consult_status?uuid=` | uuid | ¿Es contribuyente? / estado | booleano/estado |

## Destinatarios, amigos y contactos (`recipent/1.0.1`, `contact/1.0`)

| Endpoint | Envías | Acción | Recibes |
|---|---|---|---|
| `POST /recipent/recipient/add` · `/setRecipient` · `/remove` | datos destinatario | CRUD de destinatarios | estado |
| `GET /recipent/recipient/allByUserName?` | username | Listar destinatarios | lista |
| `POST /contact/contact-add` · `/contact-remove-request` | contacto | Agregar/eliminar amigo | estado |
| `GET /contact/contacts/…` · `/all-contact-request/…` · `/user-name?` | — / query | Listar amigos y solicitudes, buscar | `receivedRequest`, `sendingRequest` |

## Transacciones, giros, donaciones, regalos y social

| Endpoint | Envías | Acción | Recibes |
|---|---|---|---|
| `GET /operations/v1.0.0/operations/account?offset=&limit=` ✅ | `offset`,`limit` (+ filtros) | Historial de transacciones | `{"transaction":[…],"pagination":{…}}` |
| `POST /cubanemail/v1.0.0/cubanemail/reason` + `/invoice/payment` | CI, monto, motivo | Enviar giro (money order) | comprobante |
| `POST /donation/v1.0.0/donations` | monto, destino | Donar | comprobante |
| `GET/POST /giftapi/v1.0.0/gift` · `/gift/category` | `card_gift` | Enviar regalo / categorías | regalo/categorías |
| `POST /coremz/cotizacion/validateMilitant` · `/createPayment` · `/uidc/payment` | datos militante/UIC | Pago de cuota PCC/UIC | comprobante |

## Marketplace y plataforma

| Endpoint | Envías | Acción | Recibes |
|---|---|---|---|
| `GET /bulevar/1.0/tiendas/allTiendas` · `/order/1.0/order/getByUserName?` | — / username | Tiendas / mis compras | comercios / órdenes |
| `POST /claim/v1.0.0/createClaims` | `claim_data`, `V_motivo` | Crear reclamación | estado |
| `POST /coremz/api/merchants/reviews/` | `merchantUuid`, rating | Valorar comercio | estado |
| `GET /coremz/utils/check-updates` | `package_name` | Comprobar actualización | versión disponible |
| `GET /nyfy/1.0/ntfy/list/` · `POST /set-active/` · `DELETE /delete/` | — | Notificaciones (listar/leída/borrar) | `NotificationModel[]` |

---

## Patrón común en toda operación de dinero

- **Envías**: `funding_source_uuid` (origen) + monto + `payment_password` **cifrada con RSA**
  (clave pública empaquetada `enzona_pubkey.pem`).
- **Recibes**: envelope `status_code` / `status_denom` / `fault` + un `transaction_signature`
  (lo emite el servidor; es el comprobante de integridad, el cliente no lo calcula).

> Confianza: paths y campos son literales del binario (alta). Los **verbos HTTP** se
> infieren del patrón de cada datasource (`::post` dominante) — confírmalos con una captura
> real. Ningún secreto ni firma de request es necesario (ver [`08-seguridad-y-cripto.md`](08-seguridad-y-cripto.md)).
