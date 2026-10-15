# 11 — Capturas en vivo: endpoints de lectura (2026-10-15)

> **Qué es**: la primera pasada sistemática **en vivo** por el catálogo de *lectura*, con una
> sesión propia (`arodriguez53`) y **solo peticiones GET** con `Authorization: Bearer <access_token>`.
> Se capturaron **64 respuestas**: endpoints confirmados, errores de negocio/gateway y fallos
> transitorios (estos se conservan a propósito porque documentan el comportamiento del backend).
>
> **Dónde están**: [`sharedLogic/src/commonTest/resources/capturas-live/`](../../sharedLogic/src/commonTest/resources/capturas-live/)
> — un `.json` por respuesta y [`_index.json`](../../sharedLogic/src/commonTest/resources/capturas-live/_index.json)
> (índice máquina-legible: archivo → URL, método, estado, ms, bytes y nota). Las tablas de §1
> enlazan cada archivo.
>
> ⚠ **Contienen datos personales reales**: PAN completos de tarjetas de destinatarios, CI,
> teléfonos, emails, direcciones y nombres de terceros. El repositorio debe seguir siendo
> **privado**; no publiques ni compartas la carpeta de capturas. **No** contienen tokens ni
> credenciales: el access token nunca se escribió a disco.
>
> Marcas: ✅ verificado en vivo (2026-10-15) · ⚠ discrepancia o duda abierta.

## Cómo leer los nombres

- `…-transitorio`, `…-2intento`, `…-timeout`: fallos reales conservados (timeouts de 25 s, 5xx,
  JSON inválido) — el mismo endpoint suele responder 200 al reintentar.
- `…-tokenheader-401` / `…-raw-401`: pruebas de mecanismo de autenticación (ver §3).
- `…-param-userName-camel-…` / `…-param-username-lower-…`: variantes de nombre de parámetro
  que el backend rechaza; se conservan para fijar el nombre correcto en los tests.
- Un contenido `{"probe_error": "…timed out…"}` = la sonda no obtuvo respuesta HTTP.

---

## 1. Resultados por área

### A. Cuenta, identidad y contactos

| Endpoint (GET) | HTTP | Captura | Nota |
|---|---|---|---|
| `/account/v1.0.0/account?username=arodriguez53` | 200 | [`account-get-username.json`](../../sharedLogic/src/commonTest/resources/capturas-live/account-get-username.json) | perfil; flags numéricos como string (`"1"`, `"true"`) |
| `/account/v1.0.0/account?username=zzzz_no_existe_000000` | 404 | [`account-get-inexistente-404-fault.json`](../../sharedLogic/src/commonTest/resources/capturas-live/account-get-inexistente-404-fault.json) | envelope `fault` (minúscula), code 4008 |
| `/account/v1.0.0/account` | 500 | [`account-get-sin-username-500-fault.json`](../../sharedLogic/src/commonTest/resources/capturas-live/account-get-sin-username-500-fault.json) | envelope alterno `Fault` (mayúscula) con null |
| `https://identity.enzona.net/oauth2/userinfo` | 200 | [`identity-userinfo.json`](../../sharedLogic/src/commonTest/resources/capturas-live/identity-userinfo.json) | claims OIDC del token |
| `/contact/1.0/api/account/user-name?user_name=arodriguez53` | 200 | [`user-search-user_name.json`](../../sharedLogic/src/commonTest/resources/capturas-live/user-search-user_name.json) | parámetro correcto `user_name`; búsqueda por subcadena (10 resultados) |
| `/contact/1.0/api/account/user-name?user_name=54674826` | 200 | [`user-search-user_name-phone.json`](../../sharedLogic/src/commonTest/resources/capturas-live/user-search-user_name-phone.json) | por teléfono: coincidencia exacta |
| `/contact/1.0/api/account/user-name?userName=arodriguez53` | 500 | [`user-search-param-userName-camel-500-fault.json`](../../sharedLogic/src/commonTest/resources/capturas-live/user-search-param-userName-camel-500-fault.json) | aviso del backend: falta `user_name` |
| `/contact/1.0/api/account/user-name?username=arodriguez53` | 500 | [`user-search-param-username-lower-500-fault.json`](../../sharedLogic/src/commonTest/resources/capturas-live/user-search-param-username-lower-500-fault.json) | ídem |
| `/contact/1.0/api/account/contacts/0a49bf4153ae4ff6b9a1b268c400c11d` | 200 | [`contacts-by-account-uuid.json`](../../sharedLogic/src/commonTest/resources/capturas-live/contacts-by-account-uuid.json) | envelope `contacts` (vacío con esta cuenta) |
| `/contact/1.0/api/account/contacts/arodriguez53` | 200 | [`contacts-by-username.json`](../../sharedLogic/src/commonTest/resources/capturas-live/contacts-by-username.json) | acepta username además de uuid |
| `/contact/1.0/api/account/all-contact-request/0a49bf4153ae4ff6b9a1b268c400c11d` | 200 | [`contact-requests-by-account-uuid.json`](../../sharedLogic/src/commonTest/resources/capturas-live/contact-requests-by-account-uuid.json) | envelope sendingRequest/receivedRequest |
| `/contact/1.0/api/account/all-contact-request/arodriguez53` | 200 | [`contact-requests-by-username.json`](../../sharedLogic/src/commonTest/resources/capturas-live/contact-requests-by-username.json) | acepta username además de uuid |
| `/recipent/1.0.1/recipient/allByUserName?userName=arodriguez53` | 200 | [`recipients-list-userName.json`](../../sharedLogic/src/commonTest/resources/capturas-live/recipients-list-userName.json) | envelope `Destinatarios`; devuelve PAN completo (¡privacidad!) |
| `/recipent/1.0.1/recipient/allByUserName?username=arodriguez53` | 500 | [`recipients-list-param-incorrecto-500-spring.json`](../../sharedLogic/src/commonTest/resources/capturas-live/recipients-list-param-incorrecto-500-spring.json) | error Spring revela que el parámetro requerido es `userName` |
| `/coremz/1.0.2/help/contacts` | 200 | [`help-contacts.json`](../../sharedLogic/src/commonTest/resources/capturas-live/help-contacts.json) | soporte: phones, whatsApp, email, socialNetworks |

### B. Tarjetas y saldo

| Endpoint (GET) | HTTP | Captura | Nota |
|---|---|---|---|
| `/coremz/1.0.2/card/username/arodriguez53` | 200 | [`cards-list-username.json`](../../sharedLogic/src/commonTest/resources/capturas-live/cards-list-username.json) | create_at/update_at SIN 'd'; `bin` string; last4/expdate string |
| `/cards/v1.0.0/cards/first` | 200 | [`card-first.json`](../../sharedLogic/src/commonTest/resources/capturas-live/card-first.json) | created_at/updated_at ISO, last4/expdate NÚMEROS, bank_certificate, C1/C2/P1/P2 |
| `/cards/v1.0.0/cards/default` | 500 | [`card-default-get-500-fault.json`](../../sharedLogic/src/commonTest/resources/capturas-live/card-default-get-500-fault.json) | GET /cards/v1.0.0/cards/default → 500 genérico |
| `/cards/v1.0.0/cards/default` | 500 | [`card-default-get-500-fault-2intento.json`](../../sharedLogic/src/commonTest/resources/capturas-live/card-default-get-500-fault-2intento.json) | confirma el 500 en el reintento |
| `/coremz/1.0.2/api/balance/lastOperations/fe88416138144b72a6a41c3f335af104` | 200 | [`balance-tarjeta-1.json`](../../sharedLogic/src/commonTest/resources/capturas-live/balance-tarjeta-1.json) | saldo CUP; operations con `amount` string; `balance` número; `respCode` |
| `/coremz/1.0.2/api/balance/lastOperations/9ccdf87637604be9b46ba22a40f31dd5` | 200 | [`balance-tarjeta-2.json`](../../sharedLogic/src/commonTest/resources/capturas-live/balance-tarjeta-2.json) | saldo USD; `respCode` null |
| `/coremz/1.0.2/api/balance/lastOperations/c4e17b9b397f4b069fcea75bbc5ebc10` | 200 | [`balance-tarjeta-3.json`](../../sharedLogic/src/commonTest/resources/capturas-live/balance-tarjeta-3.json) | saldo CUP; canales ATM |
| `/coremz/1.0.2/api/balance/lastOperations/33691209efcc46a1832899257aca56ba` | 200 | [`balance-tarjeta-4.json`](../../sharedLogic/src/commonTest/resources/capturas-live/balance-tarjeta-4.json) | saldo USD; `respCode` "00" |
| `/coremz/1.0.2/api/balance/lastOperations/fe88416138144b72a6a41c3f335af104` | 500 | [`balance-tarjeta-1-suspended-500.json`](../../sharedLogic/src/commonTest/resources/capturas-live/balance-tarjeta-1-suspended-500.json) | transitorio del backend (fault 303001 SUSPENDED) |
| `/coremz/1.0.2/api/balance/lastOperations/9ccdf87637604be9b46ba22a40f31dd5` | 500 | [`balance-tarjeta-2-suspended-500.json`](../../sharedLogic/src/commonTest/resources/capturas-live/balance-tarjeta-2-suspended-500.json) | transitorio |
| `/coremz/1.0.2/api/balance/lastOperations/c4e17b9b397f4b069fcea75bbc5ebc10` | 500 | [`balance-tarjeta-3-suspended-500.json`](../../sharedLogic/src/commonTest/resources/capturas-live/balance-tarjeta-3-suspended-500.json) | transitorio |
| `/coremz/1.0.2/api/balance/lastOperations/33691209efcc46a1832899257aca56ba` | 500 | [`balance-tarjeta-4-suspended-500.json`](../../sharedLogic/src/commonTest/resources/capturas-live/balance-tarjeta-4-suspended-500.json) | transitorio |
| `/coremz/1.0.2/api/balance/lastOperations/9ccdf87637604be9b46ba22a40f31dd5` | timeout | [`balance-tarjeta-2-timeout.json`](../../sharedLogic/src/commonTest/resources/capturas-live/balance-tarjeta-2-timeout.json) | timeout 25 s antes del 200 final |
| `/coremz/1.0.2/api/balance/lastOperations/fe88416138144b72a6a41c3f335af104` | 401 | [`balance-tarjeta-1-tokenheader-401.json`](../../sharedLogic/src/commonTest/resources/capturas-live/balance-tarjeta-1-tokenheader-401.json) | fault 900902: la cabecera `token` se rechaza; solo Bearer |

### C. Transacciones y transferencias

| Endpoint (GET) | HTTP | Captura | Nota |
|---|---|---|---|
| `/operations/v1.0.0/operations/account?offset=0&limit=10` | 200 | [`operations-account-offset0-limit10.json`](../../sharedLogic/src/commonTest/resources/capturas-live/operations-account-offset0-limit10.json) | paginado; `amount` numérico; campos no aplicables = false |
| `/transfer/v1.0.0/transfer?limit=10&offset=0` | 200 | [`transfers-list-limit10-offset0.json`](../../sharedLogic/src/commonTest/resources/capturas-live/transfers-list-limit10-offset0.json) | envelope `avatars` + `transfer` |
| `/transfer/v1.0.0/transfer?limit=10&offset=0` | 202 | [`transfers-list-limit10-offset0-202-transitorio.json`](../../sharedLogic/src/commonTest/resources/capturas-live/transfers-list-limit10-offset0-202-transitorio.json) | primer intento: 202 y JSON inválido (`"avatars":,`) |
| `/transfer/v1.0.0/transfer` | 500 | [`transfers-list-sin-parametros-500-exception.json`](../../sharedLogic/src/commonTest/resources/capturas-live/transfers-list-sin-parametros-500-exception.json) | exige limit/offset; fuga `bpsTransfer?wsdl` |

### D. Servicios, facturas y giros

| Endpoint (GET) | HTTP | Captura | Nota |
|---|---|---|---|
| `/invoice/v1.0.0/invoice?offset=0&limit=10` | 200 | [`invoice-list-offset0-limit10.json`](../../sharedLogic/src/commonTest/resources/capturas-live/invoice-list-offset0-limit10.json) | envelope `invoice` con un OBJETO, no lista |
| `/invoice/v1.0.0/client-id` | 404 | [`invoice-client-id-get-404-fault.json`](../../sharedLogic/src/commonTest/resources/capturas-live/invoice-client-id-get-404-fault.json) | 404 del gateway |
| `/cubanemail/v1.0.0/cubanemail/ci/02033079604` | 200 | [`cubanemail-ci.json`](../../sharedLogic/src/commonTest/resources/capturas-live/cubanemail-ci.json) | `response` con JSON en base64 dentro |
| `/onat/v1.0.0/tax-payer?taxPayer=02033079604` | 400 | [`onat-taxpayer-400-fault.json`](../../sharedLogic/src/commonTest/resources/capturas-live/onat-taxpayer-400-fault.json) | fault 4128: servicio ONAT no disponible |
| `/onat/v1.0.0/tax-payer?taxPayer=02033079604&client_id=02033079604` | 400 | [`onat-taxpayer-con-client-id-400-fault.json`](../../sharedLogic/src/commonTest/resources/capturas-live/onat-taxpayer-con-client-id-400-fault.json) | igual con client_id |

### E. Cuenta fiscal

| Endpoint (GET) | HTTP | Captura | Nota |
|---|---|---|---|
| `/coremz/1.0.2/fiscal_account/all/` | 404 | [`fiscal-account-all-get-404-fault.json`](../../sharedLogic/src/commonTest/resources/capturas-live/fiscal-account-all-get-404-fault.json) | 404 del gateway |
| `/coremz/1.0.2/fiscal_account/all` | 404 | [`fiscal-account-all-sin-slash-404-fault.json`](../../sharedLogic/src/commonTest/resources/capturas-live/fiscal-account-all-sin-slash-404-fault.json) | sin barra final: 404 |
| `/coremz/1.0.2/fiscal_account/all/?userName=arodriguez53` | timeout | [`fiscal-account-all-query-timeout.json`](../../sharedLogic/src/commonTest/resources/capturas-live/fiscal-account-all-query-timeout.json) | timeout 25 s |
| `/coremz/1.0.2/fiscal_account/allProvince` | 200 | [`fiscal-account-allProvince.json`](../../sharedLogic/src/commonTest/resources/capturas-live/fiscal-account-allProvince.json) | envelope `Province`; nombres con relleno |
| `/coremz/1.0.2/fiscal_account/municipe?province_code=21` | 200 | [`fiscal-account-municipe-province-code-21.json`](../../sharedLogic/src/commonTest/resources/capturas-live/fiscal-account-municipe-province-code-21.json) | envelope `municipe`; clave `codDpa` |
| `/coremz/1.0.2/fiscal_account/branch?code_dpa=2101` | 200 | [`fiscal-account-branch-code-dpa-2101.json`](../../sharedLogic/src/commonTest/resources/capturas-live/fiscal-account-branch-code-dpa-2101.json) | envelope `branch` (branch_id/bank/code/name/dpa) |
| `/coremz/1.0.2/fiscal_account/is_contribuyente?account_uuid=0a49bf4153ae4ff6b9a1b268c400c11d` | timeout | [`fiscal-account-is-contribuyente-timeout-1.json`](../../sharedLogic/src/commonTest/resources/capturas-live/fiscal-account-is-contribuyente-timeout-1.json) | primer intento: timeout 25 s |
| `/coremz/1.0.2/fiscal_account/is_contribuyente?account_uuid=0a49bf4153ae4ff6b9a1b268c400c11d` | timeout | [`fiscal-account-is-contribuyente-timeout-2.json`](../../sharedLogic/src/commonTest/resources/capturas-live/fiscal-account-is-contribuyente-timeout-2.json) | segundo intento: timeout 25 s |
| `/coremz/1.0.2/fiscal_account/is_contribuyente?account_uuid=0a49bf4153ae4ff6b9a1b268c400c11d` | 401 | [`fiscal-account-is-contribuyente-tokenheader-401.json`](../../sharedLogic/src/commonTest/resources/capturas-live/fiscal-account-is-contribuyente-tokenheader-401.json) | cabecera `token` rechazada |
| `/coremz/1.0.2/fiscal_account/is_contribuyente?account_uuid=0a49bf4153ae4ff6b9a1b268c400c11d` | 401 | [`fiscal-account-is-contribuyente-raw-401.json`](../../sharedLogic/src/commonTest/resources/capturas-live/fiscal-account-is-contribuyente-raw-401.json) | Authorization sin `Bearer` rechazado |

### F. Comercio, QR y social

| Endpoint (GET) | HTTP | Captura | Nota |
|---|---|---|---|
| `/bulevar/1.0/tiendas/allTiendas` | 200 | [`bulevar-tiendas-allTiendas.json`](../../sharedLogic/src/commonTest/resources/capturas-live/bulevar-tiendas-allTiendas.json) | envelope Spring Page |
| `/giftapi/v1.0.0/gift/category` | 200 | [`gift-categories.json`](../../sharedLogic/src/commonTest/resources/capturas-live/gift-categories.json) | uuid de relleno 'qwerty…' en producción |
| `/giftapi/v1.0.0/gift?userName=arodriguez53` | 200 | [`gift-list-param-userName-camel.json`](../../sharedLogic/src/commonTest/resources/capturas-live/gift-list-param-userName-camel.json) | envelope `avatars` + `gift` |
| `/giftapi/v1.0.0/gift?username=arodriguez53` | 200 | [`gift-list-param-username-lower.json`](../../sharedLogic/src/commonTest/resources/capturas-live/gift-list-param-username-lower.json) | idéntico a userName |
| `/order/1.0/order/getByUserName?userName=arodriguez53&offset=0&limit=10` | 200 | [`orders-by-userName.json`](../../sharedLogic/src/commonTest/resources/capturas-live/orders-by-userName.json) | envelope `orders` (vacío) |
| `/order/1.0/order/getByUserName?username=arodriguez53&offset=0&limit=10` | 400 | [`orders-by-username-400.json`](../../sharedLogic/src/commonTest/resources/capturas-live/orders-by-username-400.json) | `username` no es el parámetro |
| `/qr/v1.0.0/qr/account` | 404 | [`qr-account-get-404-fault.json`](../../sharedLogic/src/commonTest/resources/capturas-live/qr-account-get-404-fault.json) | GET da fault 4016 (probablemente sea POST) |
| `/qr/v1.0.0/qr/account` | 404 | [`qr-account-get-404-fault-2intento.json`](../../sharedLogic/src/commonTest/resources/capturas-live/qr-account-get-404-fault-2intento.json) | confirma el fault 4016 |
| `/coremz/1.0.2/uidc/info` | 404 | [`uidc-info-404-fault.json`](../../sharedLogic/src/commonTest/resources/capturas-live/uidc-info-404-fault.json) | 404 del gateway |
| `/coremz/1.0.2/utils/check-updates?package_name=cu.xetid.apk.enzona` | 404 | [`check-updates-404-fault.json`](../../sharedLogic/src/commonTest/resources/capturas-live/check-updates-404-fault.json) | 404 del gateway |

### G. Catálogos y notificaciones

| Endpoint (GET) | HTTP | Captura | Nota |
|---|---|---|---|
| `/nomenclator/v1.0.0/nomenclator/banks` | 200 | [`nomenclator-banks.json`](../../sharedLogic/src/commonTest/resources/capturas-live/nomenclator-banks.json) | envelope `banks` |
| `/nomenclator/v1.0.0/nomenclator/coordinates` | 200 | [`nomenclator-coordinates.json`](../../sharedLogic/src/commonTest/resources/capturas-live/nomenclator-coordinates.json) | envelope `coordinates` |
| `/nyfy/1.0/ntfy/list/arodriguez53` | 200 | [`notifications-list-username.json`](../../sharedLogic/src/commonTest/resources/capturas-live/notifications-list-username.json) | array directo de ntfy |
| `/nyfy/1.0/ntfy/list/?username=arodriguez53` | timeout | [`notifications-list-query-timeout.json`](../../sharedLogic/src/commonTest/resources/capturas-live/notifications-list-query-timeout.json) | variante query: timeout |
| `/nyfy/1.0/ntfy/list/?username=arodriguez53` | 401 | [`notifications-list-query-tokenheader-401.json`](../../sharedLogic/src/commonTest/resources/capturas-live/notifications-list-query-tokenheader-401.json) | cabecera `token` rechazada |
| `/nyfy/1.0/ntfy/list/?username=arodriguez53` | 401 | [`notifications-list-query-raw-401.json`](../../sharedLogic/src/commonTest/resources/capturas-live/notifications-list-query-raw-401.json) | Authorization sin `Bearer` rechazado |

---

## 2. Esquemas confirmados (extractos reales)

> Los volcados completos están en la carpeta; aquí solo las claves que importan para los DTO.

**Cuenta** (`account-get-username.json`) — nótese que los flags y números llegan como **string**:

```json
{"account":{
  "uuid":"0a49…c11d","username":"arodriguez53","email":"…","avatar":"images/user/avatar/arodriguez53.png",
  "name":"Andrey","lastname":"…","phone":"…","gender":"M",
  "created_at":"2019-07-24T13:21:20.000-04:00","updated_at":"2025-01-01T13:39:54.000-05:00",
  "identification":"…","public_phone":"false","public_email":"true","offline_payment":"false",
  "payment_password":"true","merchant_account":"false","contact_code":"…","receive_code":"…",
  "address":"…","profile_level":"8","ocupation_denom":"Informático","ocupation_code":"1300",
  "primary_currency":"cup","merchants":[{}],"fingerprint":"true","verified":"1","verified_phone":"1"
}}
```

**Tarjetas — listado** (`cards-list-username.json`; 4 tarjetas). Tipos laxos: `last4`/`expdate`/`bin`
son **string** aquí, `status`/`verified` números:

```json
{"card":[{"funding_source_uuid":"fe88…","last4":"5800","cardholder":"…","expdate":"0232",
  "create_at":"2022-03-01 14:42:43.0","update_at":"2022-03-01 14:42:54.0","status":1,
  "currency":"cup","primary_source":false,"bank_name":"Banco de Crédito y Comercio (BANDEC)",
  "bank_code":"06","verified":1,"bin":"922706999"}, "…"]}
```

**Tarjeta — `/cards/first`** (`card-first.json`): mismas tarjeta, **otra forma**: `created_at`/`updated_at`
ISO, `last4`/`expdate` **números** (dígitos invertidos: `3202`), `bank_certificate` (PEM en base64) y
`C1`/`C2`/`P1`/`P2` (coordenadas):

```json
{"card":{"funding_source_uuid":"fe88…","last4":5800,"expdate":3202,
  "created_at":"2022-03-01T14:42:43.000-05:00","updated_at":"2022-03-01T14:42:54.000-05:00",
  "status":1,"primary_source":false,"currency":"cup","bank_code":"06","bank_certificate":"LS0t…",
  "verified":1,"C1":"I02","C2":"E07","P1":2,"P2":1}}
```

**Saldo + operaciones** (`balance-tarjeta-1.json`): el esquema decodificado de `06` se confirma
entero, incluido `respCode`; `balance` es número y el `amount` de cada operación string;
`operationDate` mezcla años de 2 y 4 dígitos (`30/04/26`, `25/09/2026`) y `currency` alterna
`cup`/`usd` (minúscula) y `USD` (mayúscula):

```json
{"operation":[{"operationDate":"30/04/26","action":"CR","channel":"IO","description":"",
  "amount":"0.05","codigo":"","avatar":"","alias":""}, "…"],
 "status_code":"1111","status_denom":"Aceptada","transaction_signature":"BXf5LCWomnxk",
 "currency":"cup","balance":0.05,"balance_available":0.05,"respCode":null}
```

**Transacciones** (`operations-account-offset0-limit10.json`): `amount` **numérico** (`1400.0`)
y campos no aplicables con `false`; `pagination` real trae solo `first/prev/next/last/total`
(y URLs absolutas al host interno `api.enzona.xetid.cu`, con otro path `/transactions`):

```json
{"pagination":{"first":null,"prev":null,
  "next":"https://api.enzona.xetid.cu/operations/v1.0.0/operations/transactions?offset=10&limit=10",
  "last":"https://api.enzona.xetid.cu/operations/v1.0.0/operations/transactions?offset=160&limit=10",
  "total":169}}
```

...y un campo sucio real: `"funding_recipient_verified": "\n\t\t\t\t\t\t\t\t\t\t\t0\n\t\t\t\t\t\t\t\t\t\t"`.

**Transferencias — listado** (`transfers-list-limit10-offset0.json`): envelope con el mapa
`avatars` (anidado y null) y `transfer[]` con subobjetos `source`/`recipient` reducidos:

```json
{"avatars":{"avatars":null},"transfer":[{"uuid":"f737…","transaction_code":1004,
  "status_code":1111,"status_denom":"Aceptada","description":null,"amount":1400.0,
  "created_at":"2026-10-03T13:41:38.000-04:00","currency":"cup",
  "source":{"username":"arodriguez53","funding_source_amount":1400.0,"verified":1,"last4":6856,"bank":95},
  "recipient":{"last4":4646,"bank":95}}, "…"]}
```

El primer intento del día devolvió **202** con un cuerpo **JSON inválido** (`"avatars":,`) — la app
original tolera/repone ese caso; sin `limit/offset` responde **500** con `{"Exception":"…bpsTransfer?wsdl…"}`.

**Facturas** (`invoice-list-offset0-limit10.json`): el envelope `invoice` es un **objeto** (no un
array) y `pagination.total` = 1; campos clave:

```json
{"invoice":{"payment_service_id":26981563,"transaction_id":10000641939058,"amount":237.5,
  "total_amount":250.0,"invoice_ez":"EZ151949","period":"2/19/2025","last4":6856,"currency":"cup",
  "transaction_status_code":1112,"transaction_status":"Fallida","transaction_code":2103,
  "transaction_denom":"Pago de servicio Nauta ETECSA","merchant_alias":"ETECSA RECARGA NAUTA GRANMA",
  "bank_code":95,"client_id":"brok3r@nauta.com.cu","funding_source_uuid":"c4e1…","rc04":null,
  "transaction_signature":"1z312op7k3Qb"},"pagination":{"total":1}}
```

**Destinatarios** (`recipients-list-userName.json`): envelope **`Destinatarios`** (español,
mayúscula) — y sí, `pan` llega **completo**:

```json
{"Destinatarios":[{"uuid":"b999…","userName":"arodriguez53","createdAt":"08-04-2026 03:06:56",
  "updatedAt":"08-04-2026 03:06:56","pan":"9204069999909880","description":"…","phone":"…",
  "last4":"9880","bankCode":"06","currency":"cup"}, "…"]}
```

**Búsqueda de cuentas** (`user-search-user_name.json`; 10 resultados por subcadena):

```json
{"account":[{"userName":"arodriguez53","avatar":"images/user/avatar/arodriguez53.png",
  "uuid":"0a49…","name":"Andrey Rodríguez Osorio","verified":"1"}, "…"]}
```

**Amistades** — envelopes confirmados: `contacts` → `{"contacts":[]}`;
`all-contact-request` → `{"sendingRequest":[],"receivedRequest":[]}` (vacíos con esta cuenta).

**Notificaciones** (`notifications-list-username.json`): **array directo** (sin envelope):

```json
[{"idNtfy":"1851…","icon":null,"message":"…Transferencia: Aceptada…","user":"arodriguez53",
  "priority":0,"createdAt":"2026-08-27 17:57:46","updatedAt":"2026-08-27 17:57:46","active":false}]
```

**Giros** (`cubanemail-ci.json`): doble envelope — el campo `response` lleva **JSON en base64**:

```json
{"response":"eyJ0b3RhbCI6MCwiZGF0YSI6W119"}   → decodifica a  {"total":0,"data":[]}
```

**Fiscal** — `allProvince` → `{"Province":[{"provinceId":1,"code":21,"name":"PINAR DEL RIO  …"}]}`
(16 provincias, nombres con relleno de espacios); `municipe` → `{"municipe":[{"municipeId":1,
"codDpa":"2101","municipe":"SANDINO  …"}]}`; `branch` → `{"branch":[{"branch_id":764,"bank":"06",
"code":104,"name":"…","dpa":"2101"}]}`.

**Catálogos** — `banks` → 7 bancos `{name, code, certificate(PEM base64)}`;
`coordinates` → `{"coordinates":{"C1":"F07","C2":"A01","P1":1,"P2":3}}`; `tiendas` → envelope
Spring Page (`content/pageable/totalPages/totalElements/last/size/number/sort`), 608 tiendas
`{uuid, nombreTienda, url, provincia}`; `help/contacts` → phones/whatsApp/email/socialNetworks;
`gift/category` → 9 categorías `{uuid, denom, description, status, avatar}` (uuid de relleno
`qwerty…` en producción); `identity/oauth2/userinfo` → claims OIDC (incluye `birthday`,
`address`, `zone`, `folio`, `tomo`, `secondFactor`).

---

## 3. Errores, envelopes y autenticación

| Envelope | Cuándo | Ejemplo |
|---|---|---|
| `{"fault":{"code":…,"message":…}}` | Errores de negocio (¡y 404 de gateway!) | `4008` «La identidad no es reconocida por el sistema.»; `4016` «La Moneda no está configurada…»; `4128` ONAT; `5001` «…intente más tarde» |
| `{"fault":{"code":404,"type":"Status report","description":"No matching resource found for given API Request"}}` | Ruta inexistente/verbo erróneo | `fiscal_account/all/`, `invoice/client-id`, `uidc/info`, `check-updates` |
| `{"fault":{"code":900902,…"Missing Credentials"}}` | Autenticación mal formada | Cabecera `token` o `Authorization` sin `Bearer` → **solo Bearer** |
| `{"Fault":null}` (¡F mayúscula!) | 500 por parámetro requerido ausente | `GET /account` sin `username` |
| `{"cod":50000,"message":"Required request parameter '…' …","backendMessage":"…stacktrace…"}` | 500 de Spring (filtra stacktrace) | descubrió que en destinatarios el parámetro es `userName` |
| `{"Exception":"The endpoint reference (EPR) … /services/bpsTransfer?wsdl …"}` | 500 del backend SOAP/CXF legacy | `GET /transfer` sin `limit/offset` |
| `{"fault":{"code":303001,…"State : SUSPENDED"}}` | 5xx **transitorio** del gateway (coremz) | balance de tarjetas la primera vez; el reintento da 200 |
| `{"probe_error":"…timed out…"}` (captura, no backend) | Sin respuesta en 25 s | `is_contribuyente` (×2), `notifications?query`, `fiscal/all?userName` |

- **Autenticación**: única aceptada `Authorization: Bearer <access_token>`. Las variantes
  «cabecera `token`» y «Authorization sin esquema» dan `401`/`900902`/`Missing Credentials`.
- **Fallos transitorios**: varias llamadas devolvieron 5xx/timeouts y **el mismo GET** al
  reintentar dio 200 (balance, transfers, municipios). Al portar: contemplar reintento.
- ⚠ El `id_token` de la sesión trae `iss: https://localhost:9443/oauth2/token` (issuer interno
  del IdP WSO2); el backend lo acepta igualmente. Anotado por si afecta a validaciones futuras.

---

## 4. ⚠ Discrepancias con la documentación (revisar en tests y en la app)

Cada punto = **lo que dice la doc** → **lo que muestran las capturas**. Pensados para
cerrar con tests de DTOs (hay fixtures listos) y un repaso de la app:

- [ ] **06 §Tarjeta (`create_at`/`update_at`)** — correcto **solo para el listado**
  (`…/card/username/{u}`, con `"2022-03-01 14:42:43.0"`); `/cards/v1.0.0/cards/first` usa
  `created_at`/`updated_at` ISO. Revisar que ningún parser asuma un único formato.
  → `cards-list-username.json`, `card-first.json`
- [ ] **06 §Tarjeta (tipos)** — en el listado `last4`/`expdate`/`bin` son **string**
  (`"5800"`, `"0232"`, `"922706999"`); en `/cards/first` `last4`/`expdate` son **número**
  (`5800`, `3202`) y `bin` no viene. Los BIN reales observados (`922706999`, `923506999`,
  `922795987`, `976003122`) **no** coinciden con el BIN Tropical `924006999` de `06`:
  el skin tropical sigue sin poder validarse.
- [ ] **06 §Saldo** — decía «NO validado en vivo»: ✅ ahora sí, esquema exacto (incluido
  `respCode`). Además: `currency` alterna `cup`/`usd`/`USD`; `operationDate` mezcla `dd/MM/yy`
  y `dd/MM/yyyy`; la cabecera `token` **no** vale (401), solo Bearer; el backend da
  fallos transitorios → reintentar. → `balance-tarjeta-*.json`
- [ ] **06 §Transacción** — `pagination` real no trae `offset`/`limit`, solo
  `first/prev/next/last/total`; las URLs del paginador apuntan al host interno
  `api.enzona.xetid.cu` con path `/operations/transactions`; `amount` es **número**
  (y en saldo, string); `funding_recipient_verified` llega con espacios/saltos.
  Revisar `PaginationDto` y el parseo de montos en la app.
- [ ] **06/07 §Transferencias (listado)** — envelope real `{"avatars":{"avatars":null},"transfer":[…]}`;
  `amount` numérico (no string con signo) en este caso; el listado exige `limit/offset`
  (sin ellos: 500 CXF) y a veces responde **202 con JSON inválido** (transitorio).
- [ ] **07 §1 (búsqueda de cuentas)** — el parámetro es **`user_name`** (con guion bajo);
  `userName`/`username` → 500. Devuelve `{"account":[…]}` (por subcadena, hasta 10;
  por teléfono, exacto). → `user-search-*.json`
- [ ] **07 §1 (destinatarios)** — el parámetro es **`userName`** (camelCase) y el envelope
  es **`Destinatarios`**; fechas `dd-MM-yyyy HH:mm:ss`; `pan` viene completo (privacidad).
- [ ] **07 §5 (QR propio)** — `GET /qr/v1.0.0/qr/account` da **404/fault 4016**: es POST
  (la doc `07` ya lo listaba como POST; `02` lo tenía como GET). Queda pendiente probarlo.
- [ ] **07 §6 (fiscal)** — `GET …/fiscal_account/all/` y `/all` dan 404 (revisar en la app
  cómo lista cuentas fiscales); `is_contribuyente` cuelga (timeout ×2). Los catálogos sí
  funcionan (con clave `codDpa` camelCase en municipios).
- [ ] **07 §10/§12/§14 (404 del gateway)** — `uidc/info`, `utils/check-updates` y
  `invoice/v1.0.0/client-id` responden 404 «No matching resource»: revisar verbos/paths
  contra la app antes de portarlos.
- [ ] **06 §Cuenta** — los flags llegan como string (`"verified":"1"`, `"fingerprint":"true"`,
  `"profile_level":"8"`), `avatar` es ruta relativa y `merchants` llega `[{}]`. Asegurar que
  la deserialización laxa KMP cubre **string↔bool/int** en estos campos.
- [ ] **09 (resumen)** — notificaciones devuelve **array directo** (no envelope); giros
  devuelven JSON en base64 dentro de `response`; `gift` devuelve `avatars`+`gift`.
- [ ] **Pendientes por no ser GET** (fuera de esta pasada): `qr/account` (POST),
  `payment/v1.0.0/payments/vendor/code`, `invoice/search`, `client-id/search` y los POST de
  escritura.

---

## 5. Cómo se obtuvieron

- Fecha: **2026-10-15**; sesión propia (usuario `arodriguez53`), app oficial + export de la
  sesión desde el perfil (debug). Token de acceso efímero; **no** se guardó en ningún archivo.
- Herramienta: un script Python de una sola pasada (urllib, timeout 25 s) que hace **solo GET**
  y guarda cuerpo + estado + ms por endpoint; los scripts de sondeo no se versionan (misma
  política que las PoC: consultan producción con una cuenta real).
- Cabeceras por petición: `Authorization: Bearer …`, `Accept: application/json`. Se probaron
  además las variantes `token:` y `Authorization` sin esquema solo para confirmar que se rechazan (§3).
- Los encadenados (saldo por tarjeta, provincias→municipios→sucursales) se resolvieron con los
  identificadores de las respuestas previas del mismo día.
- Un reintento manual aclaró los transitorios (202/500/timeouts → 200).

> Para repetir: exporta la sesión desde la app (herramienta de debug del perfil), apunta el
> script a `api.enzona.net`, y **revisa que no queden datos que no quieras versionar** antes
> de copiar las capturas a `capturas-live/`.
