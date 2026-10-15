# 07 — Catálogo de endpoints y cuerpos de request

> Mapeo endpoint → verbo → request, reconstruido de los `*_remote_datasource` decompilados
> con Blutter (Dart 3.3.4). Paths y claves de body/query son **literales reales** del binario
> (confianza alta). El verbo se infiere del nombre del método y del patrón del datasource
> (`::post` dominante); confírmalo contra el backend. `{…}` = valor dinámico; `?a=&b=` = query.
>
> Volcado bruto por datasource: [`APENDICE-endpoints-por-datasource.txt`](APENDICE-endpoints-por-datasource.txt).

Base: `https://api.enzona.net` · IdP: `https://identity.enzona.net` · Auth: `Authorization: Bearer <access_token>`.

---

## 0. OAuth2 — flujo completo confirmado

**Service Provider** (WSO2): `admin_ppago-apk_PRODUCTION`, tenant `carbon.super`.
**client_id**: `ofr3Wz9nnfZaFd18OewdZYvuTaEa` *(el del cliente oficial — usa el tuyo)*.

### Authorize (WebView)
```
GET https://identity.enzona.net/oauth2/authorize
    ?response_type=code
    &client_id={client_id}
    &scope=openid
    &redirect_uri=http://apk-callback
    &code_challenge={challenge}
    &code_challenge_method=S256
```
Captura del redirect: `http://apk-callback/?code={authCode}`.
> Las claves PKCE se guardan localmente (`security_local_datasource`):
> `codeVerifier`, `codeChallenge`, `authCode`, `expTime`.

### Token (`Content-Type: application/x-www-form-urlencoded`)
```
POST https://identity.enzona.net/oauth2/token
  body: client_id={client_id}&grant_type=authorization_code
        &code={authCode}&code_verifier={verifier}
        &redirect_uri=http://apk-callback
```
Respuesta → `TokenModel` (`access_token`, `refresh_token`, `token_type`, `expires_in`,
`id_token`, `scope`). Ver `06-esquemas-exactos.md`.

### Refresh
```
POST https://identity.enzona.net/oauth2/token
  body: client_id={client_id}&grant_type=refresh_token&refresh_token={refresh_token}
```
> Hay también un `/oauth/token` referenciado en varios datasources (claim, notification,
> transaction) — es el endpoint de refresh usado internamente por esos módulos.

---

## 1. account (`account/v1.0.0`, `coremz/1.0.2`, `recipent/1.0.1`)

| Verbo | Path | Request |
|---|---|---|
| GET | `/account/v1.0.0/account?username={u}` | — (perfil) |
| PUT | `/account/v1.0.0/account` | AccountModel (incluye `socialNetworks`) |
| POST | `/account/v1.0.0/account/authentication` | credenciales |
| PUT | `/account/v1.0.0/account/password` | `{ old_payment_password?, payment_password }` (cifrado) |
| POST | `/account/v1.0.0/account/paymentpassword/reset` | reset pass de pago |
| POST | `/account/v1.0.0/account/phone/code/number` | `{ phone }` |
| POST | `/account/v1.0.0/account/phone/verified` | `{ phone, code }` |
| POST | `/account/v1.0.0/account/verify` | `{ code }` |
| POST | `/account/v1.0.0/account/TOTP/init` | — (inicia TOTP) |
| GET | `/account/v1.0.0/account/TOTP/validate?{...}` | valida TOTP |
| POST | `/account/v1.0.0/account/TOTP/disable` | desactiva TOTP |
| POST | `/coremz/1.0.2/api/account/updateAnyToUser` | upgrade a usuario |
| GET | `/coremz/1.0.2/api/account/sendCode?token={t}` | reenvía código |
| GET | `/coremz/1.0.2/help/contacts` | contactos de ayuda |
| POST | `/recipent/1.0.1/recipient/add` | nuevo destinatario |
| POST | `/recipent/1.0.1/recipient/setRecipient` | actualizar destinatario |
| POST | `/recipent/1.0.1/recipient/remove` | eliminar destinatario |
| GET | `/recipent/1.0.1/recipient/allByUserName?{...}` | destinatarios del usuario |

> ✅ **Correcciones verificadas en vivo contra el backend real (PoC macOS, HTTP 200):**
> - **Cuenta**: `GET /account/v1.0.0/account?username=<sub>` (el `<sub>` es el claim `sub` del
>   id_token). Respuesta envuelta en `{"account":{…}}`.
> - **Listar tarjetas**: `GET /coremz/1.0.2/card/username/{username}` → `{"card":[…]}`.
>   Otras rutas de tarjeta: `/cards/v1.0.0/cards/{uuid}/delete` (borrar),
>   `/cards/v1.0.0/cards/first` (principal), `/cards/v1.0.0/cards/default` (por defecto).
>   ⚠ `GET /coremz/1.0.2/card` (sin `/username/`) da **404**.
> - **Últimas operaciones de una tarjeta**: `GET /coremz/1.0.2/api/balance/lastOperations/{uuid}`.
> - **Historial de transacciones**: `GET /operations/v1.0.0/operations/account?offset=&limit=`
>   (paginado; filtros opcionales `start_date_filter`, `end_date_filter`, `status_filter`,
>   `transaction_type_filter`). Respuesta `{"transaction":[…], "pagination":{offset,limit,total,first,prev,next,last}}`.
> - ⚠ **Tipos laxos**: el backend devuelve `false` para campos no aplicables y mezcla
>   número/string/bool para flags. Deserializa con tolerancia (ver `06 §Tipos laxos`).
>
> ✅ **Segunda pasada en vivo (2026-10-15, solo GET)** — catálogo completo, capturas y discrepancias en
> [`11-capturas-live.md`](11-capturas-live.md): `user-name` exige `user_name` (devuelve `{"account":[…]}`);
> `contacts/{uuid|username}` → `{"contacts":[…]}`; `all-contact-request/{…}` →
> `{"sendingRequest":[],"receivedRequest":[]}`; destinatarios: parámetro `userName`, envelope `Destinatarios`;
> `/cards/v1.0.0/cards/first` 200 (con `created_at`/`updated_at`) y `/cards/v1.0.0/cards/default` 500;
> balance `lastOperations` 200 (ver `06 §Saldo`); `transfer?limit&offset` 200; `invoice?offset&limit` 200
> (`invoice` es un objeto); `fiscal_account/all/` 404; `uidc/info` y `utils/check-updates` 404;
> `qr/account` por GET → 404/fault 4016; `help/contacts`, `gift/*`, `ntfy/list/{username}`, `allTiendas` y
> los catálogos `nomenclator`/`fiscal_account` 200.

## 2. security
| POST | `/oauth2/token` (identity) | ver §0 |

## 3. transfer (`transfer/v1.0.0`, `coremz/1.0.2`, `payment/v1.0.0`)

| Verbo | Path | Request (campos clave) |
|---|---|---|
| POST | `/transfer/v1.0.0/transfer/account` | `funding_source_uuid`, `funding_source_amount`, `recipient_username`/`funding_recipient_uuid`, `payment_password`(cifrado) |
| POST | `/transfer/v1.0.0/transfer/card` | `funding_source_uuid`, `panRecipient`, `funding_source_amount`, `payment_password` |
| POST | `/coremz/1.0.2/transfer/fincimex` | transfer a tarjeta Fincimex |
| POST | `/coremz/1.0.2/qr/postFincimex` | pago Fincimex por QR |
| GET | `/transfer/v1.0.0/transfer?{...}` | histórico/filtro de transferencias |
| GET/POST | `/payment/v1.0.0/payments/vendor/code` | código de vendedor |

Respuesta: envelope `status_code`/`status_denom`/`fault` + `transaction_signature` + `pan` + `created_at`.

## 4. scanear / QR (`qr/v1.0.0`, `payment/v1.0.0`, `service-core`)

| Verbo | Path | Request |
|---|---|---|
| POST | `/qr/v1.0.0/qr/{…}` | pago por QR: `qr_code`, `merchant_uuid`, `vendor_identity_code` |
| POST | `/payment/v1.0.0/payments/vendor/code` | `vendor_identity_code` |
| POST | `https://service-core.enzona.net/atm` | extracción ATM: `qr_code`, `cash_advance` |

## 5. home
| POST | `/qr/v1.0.0/qr/account` | generar QR propio: `qr_code`, `merchant_uuid`, `vendor_identity_code` |

## 6. bank_account / fiscal (`coremz/1.0.2/fiscal_account`)

| Verbo | Path | Request |
|---|---|---|
| POST | `/coremz/1.0.2/fiscal_account` | crear cuenta fiscal |
| GET | `/coremz/1.0.2/fiscal_account/all/` | listar |
| GET | `/coremz/1.0.2/fiscal_account/allProvince` | provincias |
| GET | `/coremz/1.0.2/fiscal_account/municipe?province_code={c}` | municipios |
| GET | `/coremz/1.0.2/fiscal_account/branch?code_dpa={c}` | sucursales |
| GET | `/coremz/1.0.2/fiscal_account/consult_status?uuid={u}` | estado |
| GET | `/coremz/1.0.2/fiscal_account/is_contribuyente?account_uuid={u}` | ¿es contribuyente? |

## 7. Pago de servicios (patrón `invoice/v1.0.0` común a etecsa/luz/gas; tax usa `onat`)

Patrón reutilizable (ETECSA, electricidad, gas):
| Verbo | Path | Request |
|---|---|---|
| GET | `/invoice/v1.0.0/client-id` | ids de cliente guardados |
| POST | `/invoice/v1.0.0/client-id` | añadir id de cliente (`client_id`, `serviceUuid`/`service_code`) |
| GET | `/invoice/v1.0.0/client-id/search?{...}` | buscar id |
| GET | `/invoice/v1.0.0/invoice?{...}` | listar facturas |
| GET | `/invoice/v1.0.0/invoice/search?{...}` | buscar factura |
| GET | `/invoice/v1.0.0/invoice/{id}` | detalle |
| POST | `/invoice/v1.0.0/invoice/payment` | pagar: `charged_invoice`, `client_id`, `payment_password`, `funding_source_uuid`, `importe_a_debitar` |
| POST | `/invoice/v1.0.0/.../delete` | eliminar id cliente |

**Impuestos (`onat/v1.0.0`)**:
| GET | `/onat/v1.0.0/tax-payer?{...}` | contribuyente (`taxPayer`) |
| GET/POST | `/onat/v1.0.0/tax` | impuesto |
| GET | `/core/1.0/fiscal_account/allProvince` | provincias |

## 8. cuban_email / giros (`cubanemail/v1.0.0` + `invoice`)
| GET | `/cubanemail/v1.0.0/cubanemail/ci/{ci}` | datos por carné |
| GET | `/cubanemail/v1.0.0/cubanemail/amount/{...}` | monto |
| POST | `/cubanemail/v1.0.0/cubanemail/reason` | motivo del giro |
| + | `/invoice/v1.0.0/invoice/payment` | pago (`importe_a_debitar`, `charged_invoice`) |

## 9. friends / contactos (`contact/1.0`)
| POST | `/contact/1.0/api/account/contact-add` | agregar |
| POST | `/contact/1.0/api/account/contact-remove-request` | eliminar |
| GET | `/contact/1.0/api/account/contacts/{...}` | lista (`receivedRequest`, `sendingRequest`) |
| GET | `/contact/1.0/api/account/all-contact-request/{...}` | solicitudes |
| GET | `/contact/1.0/api/account/user-name?{...}` | buscar por username |

## 10. bulevar (`bulevar/1.0`, `order/1.0`)
| GET | `/bulevar/1.0/tiendas/allTiendas` | tiendas |
| GET | `/order/1.0/order/getByUserName?{...}` | mis órdenes |

## 11. donation / gift
| POST | `/donation/v1.0.0/donations` | donar |
| GET | `/giftapi/v1.0.0/gift` · `/gift/category` | regalos y categorías (`card_gift`) |

## 12. social_organizations (`coremz/1.0.2`)
| POST | `/coremz/1.0.2/api/cotizacion/validateMilitant` | validar militante PCC |
| POST | `/coremz/1.0.2/api/cotizacion/createPayment` | pago de cuota PCC |
| GET | `/coremz/1.0.2/uidc/info` | info UJC/UIC |
| POST | `/coremz/1.0.2/uidc/payment` | pago UIC (`paymentUICResponseDTO`) |

## 13. transaction (`operations/v1.0.0`)
| GET | `/operations/v1.0.0/operations/account?{...}` | historial, con `start_date_filter` (+ paginación `PaginationModel`) |

## 14. common / plataforma
| POST | `/claim/v1.0.0/createClaims` | reclamación (`claim_data`, `V_motivo`) |
| POST | `/coremz/1.0.2/api/merchants/reviews/` | rating de comercio (`merchantUuid`) |
| GET | `/coremz/1.0.2/utils/check-updates` | auto-update (`package_name`) + `https://api.apklis.cu/` |

## 15. notification_service (ntfy `nyfy/1.0`)
| GET | `/nyfy/1.0/ntfy/list/{...}` | listar notificaciones |
| POST | `/nyfy/1.0/ntfy/set-active/{...}` | marcar activa/leída |
| DELETE | `/nyfy/1.0/ntfy/delete/{...}` | eliminar |
| WS | `websocket_notification_datasource` | notificaciones en tiempo real (WebSocket) |

---

### Para KMP
- Un solo `HttpClient` Ktor con base `api.enzona.net` + `Auth` bearer (refresh contra
  `identity.enzona.net/oauth2/token`).
- El pago de servicios (etecsa/luz/gas) es **una sola implementación** parametrizada por
  `serviceUuid`/`service_code`; evita duplicar 4 veces.
- Operaciones de dinero: `payment_password` **cifrada** (RSA con clave pública del
  certificado del banco) + confirmación biométrica/TOTP.
- Históricos: paginados → expón `page`/`size` (confirmar nombres reales).
