### A. Cuenta, identidad y contactos

| Endpoint (GET) | HTTP | Captura | Nota |
|---|---|---|---|
| `/account/v1.0.0/account?username=arodriguez53` | 200 | [`account-get-username.json`](account-get-username.json) | perfil; flags numéricos como string (`"1"`, `"true"`) |
| `/account/v1.0.0/account?username=zzzz_no_existe_000000` | 404 | [`account-get-inexistente-404-fault.json`](account-get-inexistente-404-fault.json) | envelope `fault` (minúscula), code 4008 |
| `/account/v1.0.0/account` | 500 | [`account-get-sin-username-500-fault.json`](account-get-sin-username-500-fault.json) | envelope alterno `Fault` (mayúscula) con null |
| `https://identity.enzona.net/oauth2/userinfo` | 200 | [`identity-userinfo.json`](identity-userinfo.json) | claims OIDC del token |
| `/contact/1.0/api/account/user-name?user_name=arodriguez53` | 200 | [`user-search-user_name.json`](user-search-user_name.json) | parámetro correcto `user_name`; búsqueda por subcadena (10 resultados) |
| `/contact/1.0/api/account/user-name?user_name=54674826` | 200 | [`user-search-user_name-phone.json`](user-search-user_name-phone.json) | por teléfono: coincidencia exacta |
| `/contact/1.0/api/account/user-name?userName=arodriguez53` | 500 | [`user-search-param-userName-camel-500-fault.json`](user-search-param-userName-camel-500-fault.json) | aviso del backend: falta `user_name` |
| `/contact/1.0/api/account/user-name?username=arodriguez53` | 500 | [`user-search-param-username-lower-500-fault.json`](user-search-param-username-lower-500-fault.json) | ídem |
| `/contact/1.0/api/account/contacts/0a49bf4153ae4ff6b9a1b268c400c11d` | 200 | [`contacts-by-account-uuid.json`](contacts-by-account-uuid.json) | envelope `contacts` (vacío con esta cuenta) |
| `/contact/1.0/api/account/contacts/arodriguez53` | 200 | [`contacts-by-username.json`](contacts-by-username.json) | acepta username además de uuid |
| `/contact/1.0/api/account/all-contact-request/0a49bf4153ae4ff6b9a1b268c400c11d` | 200 | [`contact-requests-by-account-uuid.json`](contact-requests-by-account-uuid.json) | envelope sendingRequest/receivedRequest |
| `/contact/1.0/api/account/all-contact-request/arodriguez53` | 200 | [`contact-requests-by-username.json`](contact-requests-by-username.json) | acepta username además de uuid |
| `/recipent/1.0.1/recipient/allByUserName?userName=arodriguez53` | 200 | [`recipients-list-userName.json`](recipients-list-userName.json) | envelope `Destinatarios`; devuelve PAN completo (¡privacidad!) |
| `/recipent/1.0.1/recipient/allByUserName?username=arodriguez53` | 500 | [`recipients-list-param-incorrecto-500-spring.json`](recipients-list-param-incorrecto-500-spring.json) | error Spring revela que el parámetro requerido es `userName` |
| `/coremz/1.0.2/help/contacts` | 200 | [`help-contacts.json`](help-contacts.json) | soporte: phones, whatsApp, email, socialNetworks |

### B. Tarjetas y saldo

| Endpoint (GET) | HTTP | Captura | Nota |
|---|---|---|---|
| `/coremz/1.0.2/card/username/arodriguez53` | 200 | [`cards-list-username.json`](cards-list-username.json) | create_at/update_at SIN 'd'; `bin` string; last4/expdate string |
| `/cards/v1.0.0/cards/first` | 200 | [`card-first.json`](card-first.json) | created_at/updated_at ISO, last4/expdate NÚMEROS, bank_certificate, C1/C2/P1/P2 |
| `/cards/v1.0.0/cards/default` | 500 | [`card-default-get-500-fault.json`](card-default-get-500-fault.json) | GET /cards/v1.0.0/cards/default → 500 genérico |
| `/cards/v1.0.0/cards/default` | 500 | [`card-default-get-500-fault-2intento.json`](card-default-get-500-fault-2intento.json) | confirma el 500 en el reintento |
| `/coremz/1.0.2/api/balance/lastOperations/fe88416138144b72a6a41c3f335af104` | 200 | [`balance-tarjeta-1.json`](balance-tarjeta-1.json) | saldo CUP; operations con `amount` string; `balance` número; `respCode` |
| `/coremz/1.0.2/api/balance/lastOperations/9ccdf87637604be9b46ba22a40f31dd5` | 200 | [`balance-tarjeta-2.json`](balance-tarjeta-2.json) | saldo USD; `respCode` null |
| `/coremz/1.0.2/api/balance/lastOperations/c4e17b9b397f4b069fcea75bbc5ebc10` | 200 | [`balance-tarjeta-3.json`](balance-tarjeta-3.json) | saldo CUP; canales ATM |
| `/coremz/1.0.2/api/balance/lastOperations/33691209efcc46a1832899257aca56ba` | 200 | [`balance-tarjeta-4.json`](balance-tarjeta-4.json) | saldo USD; `respCode` "00" |
| `/coremz/1.0.2/api/balance/lastOperations/fe88416138144b72a6a41c3f335af104` | 500 | [`balance-tarjeta-1-suspended-500.json`](balance-tarjeta-1-suspended-500.json) | transitorio del backend (fault 303001 SUSPENDED) |
| `/coremz/1.0.2/api/balance/lastOperations/9ccdf87637604be9b46ba22a40f31dd5` | 500 | [`balance-tarjeta-2-suspended-500.json`](balance-tarjeta-2-suspended-500.json) | transitorio |
| `/coremz/1.0.2/api/balance/lastOperations/c4e17b9b397f4b069fcea75bbc5ebc10` | 500 | [`balance-tarjeta-3-suspended-500.json`](balance-tarjeta-3-suspended-500.json) | transitorio |
| `/coremz/1.0.2/api/balance/lastOperations/33691209efcc46a1832899257aca56ba` | 500 | [`balance-tarjeta-4-suspended-500.json`](balance-tarjeta-4-suspended-500.json) | transitorio |
| `/coremz/1.0.2/api/balance/lastOperations/9ccdf87637604be9b46ba22a40f31dd5` | timeout | [`balance-tarjeta-2-timeout.json`](balance-tarjeta-2-timeout.json) | timeout 25 s antes del 200 final |
| `/coremz/1.0.2/api/balance/lastOperations/fe88416138144b72a6a41c3f335af104 (#cabecera token)` | 401 | [`balance-tarjeta-1-tokenheader-401.json`](balance-tarjeta-1-tokenheader-401.json) | fault 900902: la cabecera `token` se rechaza; solo Bearer |

### C. Transacciones y transferencias

| Endpoint (GET) | HTTP | Captura | Nota |
|---|---|---|---|
| `/operations/v1.0.0/operations/account?offset=0&limit=10` | 200 | [`operations-account-offset0-limit10.json`](operations-account-offset0-limit10.json) | paginado; `amount` numérico; campos no aplicables = false |
| `/transfer/v1.0.0/transfer?limit=10&offset=0` | 200 | [`transfers-list-limit10-offset0.json`](transfers-list-limit10-offset0.json) | envelope `avatars` + `transfer` |
| `/transfer/v1.0.0/transfer?limit=10&offset=0` | 202 | [`transfers-list-limit10-offset0-202-transitorio.json`](transfers-list-limit10-offset0-202-transitorio.json) | primer intento: 202 y JSON inválido (`"avatars":,`) |
| `/transfer/v1.0.0/transfer` | 500 | [`transfers-list-sin-parametros-500-exception.json`](transfers-list-sin-parametros-500-exception.json) | exige limit/offset; fuga `bpsTransfer?wsdl` |

### D. Servicios, facturas y giros

| Endpoint (GET) | HTTP | Captura | Nota |
|---|---|---|---|
| `/invoice/v1.0.0/invoice?offset=0&limit=10` | 200 | [`invoice-list-offset0-limit10.json`](invoice-list-offset0-limit10.json) | envelope `invoice` con un OBJETO, no lista |
| `/invoice/v1.0.0/client-id` | 404 | [`invoice-client-id-get-404-fault.json`](invoice-client-id-get-404-fault.json) | 404 del gateway |
| `/cubanemail/v1.0.0/cubanemail/ci/02033079604` | 200 | [`cubanemail-ci.json`](cubanemail-ci.json) | `response` con JSON en base64 dentro |
| `/onat/v1.0.0/tax-payer?taxPayer=02033079604` | 400 | [`onat-taxpayer-400-fault.json`](onat-taxpayer-400-fault.json) | fault 4128: servicio ONAT no disponible |
| `/onat/v1.0.0/tax-payer?taxPayer=02033079604&client_id=02033079604` | 400 | [`onat-taxpayer-con-client-id-400-fault.json`](onat-taxpayer-con-client-id-400-fault.json) | igual con client_id |

### E. Cuenta fiscal

| Endpoint (GET) | HTTP | Captura | Nota |
|---|---|---|---|
| `/coremz/1.0.2/fiscal_account/all/` | 404 | [`fiscal-account-all-get-404-fault.json`](fiscal-account-all-get-404-fault.json) | 404 del gateway |
| `/coremz/1.0.2/fiscal_account/all` | 404 | [`fiscal-account-all-sin-slash-404-fault.json`](fiscal-account-all-sin-slash-404-fault.json) | sin barra final: 404 |
| `/coremz/1.0.2/fiscal_account/all/?userName=arodriguez53` | timeout | [`fiscal-account-all-query-timeout.json`](fiscal-account-all-query-timeout.json) | timeout 25 s |
| `/coremz/1.0.2/fiscal_account/allProvince` | 200 | [`fiscal-account-allProvince.json`](fiscal-account-allProvince.json) | envelope `Province`; nombres con relleno |
| `/coremz/1.0.2/fiscal_account/municipe?province_code=21` | 200 | [`fiscal-account-municipe-province-code-21.json`](fiscal-account-municipe-province-code-21.json) | envelope `municipe`; clave `codDpa` |
| `/coremz/1.0.2/fiscal_account/branch?code_dpa=2101` | 200 | [`fiscal-account-branch-code-dpa-2101.json`](fiscal-account-branch-code-dpa-2101.json) | envelope `branch` (branch_id/bank/code/name/dpa) |
| `/coremz/1.0.2/fiscal_account/is_contribuyente?account_uuid=0a49bf4153ae4ff6b9a1b268c400c11d` | timeout | [`fiscal-account-is-contribuyente-timeout-1.json`](fiscal-account-is-contribuyente-timeout-1.json) | primer intento: timeout 25 s |
| `/coremz/1.0.2/fiscal_account/is_contribuyente?account_uuid=0a49bf4153ae4ff6b9a1b268c400c11d` | timeout | [`fiscal-account-is-contribuyente-timeout-2.json`](fiscal-account-is-contribuyente-timeout-2.json) | segundo intento: timeout 25 s |
| `/coremz/1.0.2/fiscal_account/is_contribuyente?account_uuid=0a49bf4153ae4ff6b9a1b268c400c11d` | 401 | [`fiscal-account-is-contribuyente-tokenheader-401.json`](fiscal-account-is-contribuyente-tokenheader-401.json) | cabecera `token` rechazada |
| `/coremz/1.0.2/fiscal_account/is_contribuyente?account_uuid=0a49bf4153ae4ff6b9a1b268c400c11d` | 401 | [`fiscal-account-is-contribuyente-raw-401.json`](fiscal-account-is-contribuyente-raw-401.json) | Authorization sin `Bearer` rechazado |

### F. Comercio, QR y social

| Endpoint (GET) | HTTP | Captura | Nota |
|---|---|---|---|
| `/bulevar/1.0/tiendas/allTiendas` | 200 | [`bulevar-tiendas-allTiendas.json`](bulevar-tiendas-allTiendas.json) | envelope Spring Page |
| `/giftapi/v1.0.0/gift/category` | 200 | [`gift-categories.json`](gift-categories.json) | uuid de relleno 'qwerty…' en producción |
| `/giftapi/v1.0.0/gift?userName=arodriguez53` | 200 | [`gift-list-param-userName-camel.json`](gift-list-param-userName-camel.json) | envelope `avatars` + `gift` |
| `/giftapi/v1.0.0/gift?username=arodriguez53` | 200 | [`gift-list-param-username-lower.json`](gift-list-param-username-lower.json) | idéntico a userName |
| `/order/1.0/order/getByUserName?userName=arodriguez53&offset=0&limit=10` | 200 | [`orders-by-userName.json`](orders-by-userName.json) | envelope `orders` (vacío) |
| `/order/1.0/order/getByUserName?username=arodriguez53&offset=0&limit=10` | 400 | [`orders-by-username-400.json`](orders-by-username-400.json) | `username` no es el parámetro |
| `/qr/v1.0.0/qr/account` | 404 | [`qr-account-get-404-fault.json`](qr-account-get-404-fault.json) | GET da fault 4016 (probablemente sea POST) |
| `/qr/v1.0.0/qr/account` | 404 | [`qr-account-get-404-fault-2intento.json`](qr-account-get-404-fault-2intento.json) | confirma el fault 4016 |
| `/coremz/1.0.2/uidc/info` | 404 | [`uidc-info-404-fault.json`](uidc-info-404-fault.json) | 404 del gateway |
| `/coremz/1.0.2/utils/check-updates?package_name=cu.xetid.apk.enzona` | 404 | [`check-updates-404-fault.json`](check-updates-404-fault.json) | 404 del gateway |

### G. Catálogos y notificaciones

| Endpoint (GET) | HTTP | Captura | Nota |
|---|---|---|---|
| `/nomenclator/v1.0.0/nomenclator/banks` | 200 | [`nomenclator-banks.json`](nomenclator-banks.json) | envelope `banks` |
| `/nomenclator/v1.0.0/nomenclator/coordinates` | 200 | [`nomenclator-coordinates.json`](nomenclator-coordinates.json) | envelope `coordinates` |
| `/nyfy/1.0/ntfy/list/arodriguez53` | 200 | [`notifications-list-username.json`](notifications-list-username.json) | array directo de ntfy |
| `/nyfy/1.0/ntfy/list/?username=arodriguez53` | timeout | [`notifications-list-query-timeout.json`](notifications-list-query-timeout.json) | variante query: timeout |
| `/nyfy/1.0/ntfy/list/?username=arodriguez53` | 401 | [`notifications-list-query-tokenheader-401.json`](notifications-list-query-tokenheader-401.json) | cabecera `token` rechazada |
| `/nyfy/1.0/ntfy/list/?username=arodriguez53` | 401 | [`notifications-list-query-raw-401.json`](notifications-list-query-raw-401.json) | Authorization sin `Bearer` rechazado |
