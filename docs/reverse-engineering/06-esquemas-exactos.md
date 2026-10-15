# 06 — Esquemas exactos (recuperados con Blutter)

> **Procedencia**: decompilación del snapshot Dart AOT (`libapp.so` arm64) con **Blutter**
> contra **Dart SDK 3.3.4** (snapshot `ee1eb666c76a5cb7746faf39d0b97547`). Los nombres de
> campo de abajo son los **literales JSON reales** cargados por los métodos `fromJson`/
> `toJson` de cada modelo (confianza **alta**). Los **tipos** se infieren del uso en el
> ensamblador y de la convención de dominio (confianza media). Muchos modelos manejan
> doble nomenclatura (snake_case en la API, camelCase interno): aquí se lista el nombre
> **de la API** (snake_case) cuando existe.

## Envelope de respuesta común

Las respuestas de operaciones envuelven el resultado con metadatos de estado:

```json
{
  "status_code": "0",
  "status_denom": "…",
  "fault": null,
  "...": "payload específico"
}
```

- `status_code` / `status_denom`: código y descripción del resultado.
- `fault`: error (null si OK).
- `transaction_signature`: firma de la transacción devuelta por el backend (presente en
  tarjetas, transferencias, facturas, QR y transacciones — es un campo transversal de integridad).

## Autenticación — `TokenModel`

Respuesta estándar OAuth2 de `identity.enzona.net/oauth2/token`:

```kotlin
@Serializable
data class TokenDto(
    @SerialName("access_token")  val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type")    val tokenType: String,   // "Bearer"
    @SerialName("expires_in")    val expiresIn: Long,
    @SerialName("id_token")      val idToken: String? = null,
    @SerialName("scope")         val scope: String? = null,
)
```

## Cuenta — `AccountModel`

> ✅ **Verificado contra el backend real (PoC macOS, HTTP 200, cobertura 77%).** Correcciones
> confirmadas: la respuesta viene **envuelta en `{"account": {...}}`**; los campos de fecha
> son **`created_at`/`updated_at`** (NO `create_at`/`update_at` como daba la extracción); el
> endpoint correcto es `GET /account/v1.0.0/account?username=<sub>` donde `<sub>` es el claim
> `sub` del id_token. `payment_password` se devuelve como booleano ("true" = configurada).
> No aparecen en la respuesta (condicionales): `account_id`, `reseted`, `totpCode`, `type`, `birthday`.

Campos JSON reales:

```
uuid, account_id, username, name, lastname, identification, gender, birthday,
address, phone, public_phone, verified_phone, email, public_email, avatar,
type, profile_level, primary_currency, offline_payment, merchant_account,
merchants, contact_code, receive_code, fingerprint, totpCode,
payment_password, old_payment_password, ocupation_code, ocupation_denom,
reseted, verified, create_at, update_at
```

```kotlin
@Serializable
data class AccountDto(
    @SerialName("uuid")            val uuid: String,
    @SerialName("account_id")      val accountId: String? = null,
    @SerialName("username")        val username: String,
    @SerialName("name")            val name: String? = null,
    @SerialName("lastname")        val lastname: String? = null,
    @SerialName("identification")  val identification: String? = null,
    @SerialName("gender")          val gender: String? = null,
    @SerialName("birthday")        val birthday: String? = null,
    @SerialName("address")         val address: String? = null,     // ojo: también "adress"
    @SerialName("phone")           val phone: String? = null,
    @SerialName("public_phone")    val publicPhone: Boolean = false,
    @SerialName("verified_phone")  val verifiedPhone: Boolean = false,
    @SerialName("email")           val email: String? = null,
    @SerialName("public_email")    val publicEmail: Boolean = false,
    @SerialName("avatar")          val avatar: String? = null,       // media.enzona.net/images/user/avatar/<avatar>
    @SerialName("type")            val type: String? = null,
    @SerialName("profile_level")   val profileLevel: Int? = null,
    @SerialName("primary_currency")val primaryCurrency: String? = null,
    @SerialName("offline_payment") val offlinePayment: Boolean = false,
    @SerialName("merchant_account")val merchantAccount: Boolean = false,
    @SerialName("contact_code")    val contactCode: String? = null,
    @SerialName("receive_code")    val receiveCode: String? = null,
    @SerialName("fingerprint")     val fingerprint: Boolean = false,
    @SerialName("totpCode")        val totpCode: String? = null,
    @SerialName("ocupation_code")  val ocupationCode: String? = null,
    @SerialName("ocupation_denom") val ocupationDenom: String? = null,
    @SerialName("verified")        val verified: Boolean = false,
    @SerialName("payment_password")val paymentPassword: Boolean = false, // ✅ devuelto: true = configurada
    @SerialName("created_at")      val createdAt: String? = null,         // ✅ corregido (no "create_at")
    @SerialName("updated_at")      val updatedAt: String? = null,         // ✅ corregido (no "update_at")
)
// La respuesta real va envuelta: { "account": { ...campos arriba... } }
```
> `old_payment_password` es **de entrada** (al actualizar la contraseña de pago) y va
> **cifrado** (ver `cadenaEncript` en CardModel / core/encrypt). En la respuesta,
> `payment_password` es un **booleano** que indica si la cuenta tiene contraseña de pago.

## Tarjeta — `CardModel` / `AddCardModel`

Campos JSON reales:
```
pan, bin, last4, expdate, cardholder, currency, status, verified, deleted,
card_uuid, funding_source_id, funding_source_uuid, primary_source,
bank_code, bank_name, bank_certificate, cadenaEncript
```

> ✅ **Verificado contra el backend real (PoC macOS, HTTP 200, cobertura 68%).**
> - **Listar tarjetas**: `GET /coremz/1.0.2/card/username/{username}` → respuesta envuelta en
>   `{"card":[ ... ]}`. (El `/coremz/1.0.2/card` sin `/username/` da 404; `/cards/v1.0.0/cards/`
>   es para *delete*; `/cards/v1.0.0/cards/first` es la tarjeta principal; `/cards/v1.0.0/cards/default` la por defecto.)
> - Fechas: las tarjetas usan **`create_at`/`update_at`** (SIN la "d", al revés que `AccountModel`).
> - `status` y `verified` llegan como **número** (`1`), no string/bool → deserializa laxo.
> - En el listado NO vienen (condicionales/seguridad): `pan`, `card_uuid`, `funding_source_id`,
>   `deleted`, `bank_certificate`.

Campos reales en el listado: `funding_source_uuid`, `last4`, `cardholder`, `expdate`,
`create_at`, `update_at`, `status`, `currency`, `primary_source`, `bank_name`, `bank_code`,
`verified`, `bin`.

```kotlin
@Serializable
data class CardDto(
    @SerialName("funding_source_uuid") val fundingSourceUuid: String? = null,
    @SerialName("last4")               val last4: String? = null,
    @SerialName("bin")                 val bin: String? = null,
    @SerialName("expdate")             val expdate: String? = null,       // "MMYY"
    @SerialName("cardholder")          val cardholder: String? = null,
    @SerialName("currency")            val currency: String? = null,       // cup / usd
    @SerialName("status")              val status: Int? = null,            // 1 (número)
    @SerialName("verified")            val verified: Int? = null,          // 1 (número)
    @SerialName("primary_source")      val primarySource: Boolean = false,
    @SerialName("bank_name")           val bankName: String? = null,       // "Banco Metropolitano S.A", "FINCIMEX CLASICA"…
    @SerialName("bank_code")           val bankCode: String? = null,       // "95", "03", "06"…
    @SerialName("create_at")           val createAt: String? = null,       // ⚠ sin "d"
    @SerialName("update_at")           val updateAt: String? = null,       // ⚠ sin "d"
)
// Solo al AÑADIR tarjeta: pan, card_uuid, cadenaEncript (datos cifrados con la clave pública del banco).
```

## Saldo + últimas operaciones de una tarjeta — `BalanceModel` / `OperationModel`

> 🔎 **Verificado por decompilación estática (Blutter, pantalla `ConsultarSaldoView`); NO validado
> en vivo.** Los tipos y formatos marcados con ⚠ se deben confirmar con una respuesta real.

**Un solo endpoint** sirve saldo y operaciones: `GET /coremz/1.0.2/api/balance/lastOperations/{funding_source_uuid}`
(sin paginación ni parámetros; cabecera `token`/Bearer). Los datasources de saldo y de operaciones
llaman a la misma URL y leen partes distintas de la misma respuesta. Un error llega como `{"fault":{…}}`.

Respuesta **plana** (`BalanceModel.fromJson(json)`):
```
operation: [ OperationModel… ]      # las últimas operaciones, en el orden recibido
balance, balance_available          # saldo contable / disponible. Número O string numérico (la app hace double.parse)
currency, bank_account              # strings (default "")
status_code (int|string), status_denom, transaction_signature
```
Cada `OperationModel` (todas las claves son **String**, default `""`; la app lanza si no lo son):
```
operationDate   # ⚠ clave camelCase. La app la muestra TAL CUAL (no hay DateFormat): probablemente ya viene "14/10/26"
action          # "DB" = débito, cualquier otro = crédito
channel         # "ENZ", "POS", "REV", "EV", "ATM", "COMERCIO"…
description     # código del servicio: "TRF","TRAN","TEL","RMV","GAS"… (ver tabla)
amount          # string, magnitud sin signo ("950.00"); el signo sale de `action`
codigo, avatar (URL https del comercio), alias (nombre del comercio)
```
**Presentación original** (`ConsultarSaldoView`):
- Saldo: `NumberFormat.simpleCurrency` + `replaceAll(",", "")` → `$3395.15`, y `currency` pequeño al lado.
- Fila: signo = `action == "DB" ? "-" : "+"`; subtítulo = `"Débito"` si DB, si no `"Crédito"`; importe + `currency`; fecha.
- "Tarjeta": `cardBank(bank_code)` = 02→AIS, 03→Clásica, 95/05→BANMET, 06→BANDEC, 12→BPA, 04→BICSA, otro→BANMET (⚠ el
  defecto "BANMET" es un vicio del original) + últimos 4 dígitos.
- Error: «Error en la operación, por favor intente más tarde.».

### Dos tarjetas Bandec: normal y Tropical / prepago (🔎 decompilación, `bank_card_widget.dart`)

El despachador de tarjetas del listado (`BankCardWidget`) elige el widget por `bank_code` (`02` AIS, `03` Clásica,
`95`/`05` Metropolitano, `06` Bandec, `04` BICSA, `12` BPA). **Solo con `06`** hay dos diseños: `BandecUsdBankCard` (la de
la playa, `tarjeta_bandec_usd.png`) y `BandecBankCard` (la roja). Lo que decide cuál **NO es la moneda**:
- Antes de despachar, la función guarda el campo `bin` de la tarjeta (lo registra en el log como `bin ------`).
- En la rama `06` lo compara con el entero **924006999** (en el binario aparece como el valor Smi `1848013998` = 924006999×2)
  y, si coincide, dibuja `BandecUsdBankCard`; si no, `BandecBankCard`. `bin` se lee como `int?` en `CardModel.fromJson`.
- 924006999 son los dígitos de `9240 0699 9`, el mismo prefijo que `add_card_controller` reconoce al teclear un número
  nuevo (y que abre los flujos `ActivationTropical` / "Telebanca asociada"): es el BIN de la tarjeta **Tropical / prepago**.
- Un `bin` ausente o distinto es una Bandec normal, aunque la tarjeta sea en USD.

Nuestro modelo: `BANDEC_TROPICAL_BIN = "924006999"`, `skinFor(bankCode, bin)` y `BankCard.bin`; el skin se llama
`BANDEC_TROPICAL`. Antes se elegía por la moneda (inferencia errónea). ⚠ Sin validar con una tarjeta tropical real.

**Título de la operación** (`DescriptionOperations.descriptionDefine`, en este orden; `contiene` = `String.contains`):
| Condición | Título | Icono (`assets/images/…`) |
|---|---|---|
| `description` vacío y `channel == "REV"` | Reverso | `iconos/ic_tranferencia` |
| vacío, `channel == "EV"` y `action == "CR"` | Nómina | `iconos/ic_nomina_verde` (débito: `ic_nomina`) |
| vacío, `action == "DB"` y `channel == "POS"` | Pago en POS | `iconos/ic_pos` |
| vacío y `action == "DB"` | Extracción | `iconos/ic_extraccion` |
| vacío (resto) | Crédito | `iconos/ic_nomina_verde` |
| `== TRF` o `== TRAN` | Transferencia | `iconos/ic_tranferencia` |
| `channel == "ENZ"` y contiene `TR EZ` | Activación de Tarjeta | logo EZ (`ez.png`) |
| no es de servicios conocidos | `alias` si no es `""`/`"null"`; si no, Pago a comercio | `iconos/ic_pago_comercio` |
| `== GAS` | Pago al servicio de Gas | `iconos/ic_pago_gas` |
| `== "Correos de Cuba"` | Correos de Cuba | `correos.png` |
| contiene `ONAT`/`IMP` | Pago de impuesto | `iconos/ic_onat` |
| contiene `TEL` | Pago de servicio a ETECSA | `iconos/ic_pago_telefono` |
| contiene `MREC`/`MRG` | Micro Recarga Móvil | `iconos/ic_pago_telefono` |
| contiene `RECA`/`RMV`/`MOVI` | Recarga Móvil | `iconos/ic_pago_telefono` |
| contiene `NAU`/`RNT` | Recarga Nauta | `iconos/ic_pago_telefono` |
| contiene `PCU`/`RPP`/`PROP` | Recarga Propia | `iconos/ic_pago_telefono` |
| contiene `LUZ`/`ELE` | Pago al servicio de Electricidad | `iconos/ic_pago_electricidad` |
| contiene `AGU` | Pago al servicio de Agua | `iconos/ic_pago_agua` |
| contiene `MULT` | Pago de Multa | `iconos/ic_extraccion` |
| `== CIMX` | Pago en CIMEX | `iconos/ic_pos` |
| contiene `Hist` | Aporte al Patrimonio | `iconos/ic_extraccion` |
| contiene `PLI` | Pago en línea | `iconos/ic_pago_telefono` |
| contiene `CADT` | Transferencia con cambio de CADECA | `iconos/ic_tranferencia` |
| (otro código de servicio) | `action == "DB"` → Extracción, si no Crédito | ic_extraccion / ic_nomina_verde |
**Cómo se ramifica realmente (`_isPayEtecsa`)**: `_isPayEtecsa(description)` es `description.contains(...)` de solo
**11 códigos de telefonía**: `TEL, MREC, MRG, RECA, RMV, MOVI, NAU, RNT, PCU, RPP, PROP`. Tras descartar `TRF/TRAN`
y `ENZ + TR EZ`:
- si `description` **NO** contiene ninguno de esos 11 → título = `alias` (si no es `""`/`"null"`), si no «Pago a comercio»;
- si contiene alguno → se recorre la cadena de la tabla (CAD, GAS, Correos, ONAT/IMP, TEL, MREC/MRG, RECA/RMV/MOVI,
  NAU/RNT, PCU/RPP/PROP, LUZ/ELE, AGU, MULT, CIMX, Hist, PLI, CADT y por último DB→Extracción / resto→Crédito).
Consecuencia (⚠ a confirmar con datos reales): en la práctica solo las filas de telefonía y `TRF/TRAN/TR EZ` se resuelven
por código; GAS, ONAT, LUZ, AGU, MULT, CIMX… solo se alcanzarían si el código ADEMÁS contuviera uno de los 11, así que
esos pagos muestran el `alias` del comercio. Evidencia de la polaridad: en la captura real «Recarga Móvil» aparece como
título (con la polaridad inversa saldría el alias o «Pago a comercio»). `description == "CAD"` devuelve título vacío.

## Transferencia — `TransferModel`

Campos JSON reales:
```
id_transfer, transaction_uuid, transaction_code, transaction_signature,
funding_source_uuid, funding_source_amount, funding_recipient_uuid,
recipient_username, merchant_uuid, tipo_cambio, bank_debit_detail, payment_password
```

```kotlin
@Serializable
data class TransferRequestDto(
    @SerialName("funding_source_uuid")    val fundingSourceUuid: String,
    @SerialName("funding_source_amount")  val amount: String,       // monto
    @SerialName("funding_recipient_uuid") val recipientUuid: String? = null,
    @SerialName("recipient_username")     val recipientUsername: String? = null,
    @SerialName("merchant_uuid")          val merchantUuid: String? = null,
    @SerialName("payment_password")       val paymentPassword: String,  // CIFRADO
)
@Serializable
data class TransferResultDto(
    @SerialName("id_transfer")           val idTransfer: String? = null,
    @SerialName("transaction_uuid")      val transactionUuid: String? = null,
    @SerialName("transaction_code")      val transactionCode: String? = null,
    @SerialName("transaction_signature") val transactionSignature: String? = null,
    @SerialName("tipo_cambio")           val exchangeRate: String? = null,
    @SerialName("bank_debit_detail")     val bankDebitDetail: String? = null,
)
```
`transfer_remote_datasource` confirma: **POST** `application/json`, respuesta con
`status_code`/`status_denom`/`fault`, `pan`, `token`, `transaction_signature`.

## Pago Fincimex — `PaymentFincimexModel` / `QrFincimex`
```
merchant_id, merchant_name, id_transaccion
```

## QR — `QrcodeModel`

Campos JSON reales:
```
qr_code, vendor_identity_code, require_password, merchant_uuid, merchant_name,
merchant_alias, merchant_avatar, id_province, denom_province, id_zone, denom_zone,
transaction_uuid, transaction_status, transaction_signature
```
> `require_password` indica si el pago a ese comercio exige contraseña de pago.

## Factura de servicio — `InvoiceModel` (ETECSA; patrón común a gas/electricidad/impuestos)
```
invoice_ez, invoice_number, client_id, payment_service_id, payment_password,
total_amount, bank_code, funding_source_uuid, bank_debit_detail,
merchant_alias, merchant_avatar,
transaction_id, transaction_uuid, transaction_code, transaction_signature,
transaction_status, transaction_description
```

## Transacción — `TransactionModel` + `PaginationModel`

> ✅ **Verificado contra el backend real (PoC macOS, HTTP 200, cobertura 84%).**
> - Endpoint: `GET /operations/v1.0.0/operations/account?offset=0&limit=20` (paginado).
> - Respuesta envuelta: `{"transaction":[ ... ], "pagination":{...}}`.
> - La extracción estática solo había capturado ~13 campos; el backend real devuelve **muchos
>   más** (abajo). Tipos **laxos**: campos opcionales llegan como `false` (p. ej. `total_amount:false`,
>   `client_id:false`, `invoice:false`) cuando no aplican, y algunos códigos como número.

Campos reales de cada transacción (verificados):
```
transaction_uuid, transaction_code, transaction_status, transaction_status_code,
transaction_description, transaction_denom, transaction_signature, transaction_created_at,
amount, total_amount, currency, bank_debit_detail,
merchant_alias, merchant_avatar, merchant_name,
client_id, invoice, invoice_number, owner, period, barcode, rc04, privuse2, metadata,
donation_title,
refund_invoice_number, refund_id_commerce, refund_idopcommerce,
source: { funding_source_amount, funding_source_username, funding_source_verified,
          funding_source_last4, funding_source_bank },
recipient: { funding_recipient_avatar, funding_recipient_name, funding_recipient_lastname,
             funding_recipient_username, funding_recipient_bank, funding_recipient_verified, pan }
```

Envelope de paginación `pagination`:
```
offset, limit, total, first, prev, next, last
```

```kotlin
@Serializable
data class OperationsResponse(
    @SerialName("transaction") val items: List<TransactionDto> = emptyList(),
    @SerialName("pagination")  val pagination: PaginationDto? = null,
)
@Serializable
data class PaginationDto(
    val offset: Int? = null, val limit: Int? = null, val total: Int? = null,
    val first: String? = null, val prev: String? = null,
    val next: String? = null, val last: String? = null,
)
// TransactionDto: ver lista de campos arriba. source/recipient son sub-objetos anidados.
// ⚠ Deserializa laxo: campos no aplicables llegan como `false` en vez de null/ausente.
```

---

### ⚠ Tipos laxos del backend (CRÍTICO para KMP) — verificado en vivo
El backend **mezcla tipos** para el mismo campo según el contexto:
- Campos no aplicables llegan como **`false`** (booleano) en vez de `null` o ausentes
  (p. ej. `total_amount:false`, `client_id:false`, `invoice:false`).
- Flags llegan como **número** (`verified:1`, `status:1`) o como **string** (`"verified":"1"`,
  `"fingerprint":"true"` en AccountModel) según el endpoint.

Implicación: **no uses deserialización estricta**. En KMP (kotlinx.serialization) usa
`Json { isLenient = true; ignoreUnknownKeys = true; coerceInputValues = true }` y, para
campos de tipo inestable, serializers personalizados que acepten bool/number/string, o
modela esos campos como `JsonElement`/`String?` y normalízalos en el mapper a dominio.
(El PoC de macOS resolvió esto renderizando desde el JSON crudo en vez de `Codable` estricto.)

### Nota sobre tipos monetarios
Los montos (`amount`, `funding_source_amount`, `total_amount`, `balance_available`) se manejan
como **String** (se formatean con `intl`); en KMP consérvalos como `String`/`BigDecimal` y
nunca como `Double` para dinero.

## Transferencias (módulo `transfer`) — 🔎 decompilación estática, NO validado en vivo

**Pantallas** (`transfer/views`): `transfer_options` (menú + "Recientes"), `transfer_card_view` ("A una tarjeta"),
`transfer_my_cards_view` ("A una de mis tarjetas"), `transfer_account_view`/`find_account_view` ("A una cuenta"),
`list_friends_to_transfer_view` ("A un amigo"), `transfer_historial_view` + filtros, y las vistas `*_detail_view`
(resultado: "Detalles de la transferencia", `No.Operación`, `Descripción`, `Móvil a confirmar`).

**Endpoints y cuerpo** (todos con la cabecera/token de siempre):
| Operación | Endpoint | Cuerpo |
|---|---|---|
| A una tarjeta | `POST /transfer/v1.0.0/transfer/card` | `pan` (destino), `funding_source_uuid` (origen), `amount`, `currency`, `description`, `phone` ("Móvil a confirmar"), `fingerprint` o `payment_password` (cifrada) |
| A una de mis tarjetas / a una cuenta | `POST /transfer/v1.0.0/transfer/account` | `funding_source_uuid`, `funding_recipient_uuid` (tarjeta destino propia), `amount`, `currency`, `description`, `fingerprint` o `payment_password` |
| Entre tarjetas Fincimex (ambas) | `POST /coremz/1.0.2/transfer/fincimex` | como `card` pero con `pin` (`checkBothFincimex` decide) |
| Recientes | `GET /transfer/v1.0.0/transfer?limit=10&offset=0` | — |
| Historial | `GET /transfer/v1.0.0/transfer?…` con `start_date`, `end_date`, `status_filter` (+ `limit`/`offset`) | — |

**Flujo de "A una tarjeta"**: validar (importe sin espacios, > 0, "Introduzca un monto válido") → diálogo «¿Está seguro que
desea transferir X CUR de la tarjeta … a la tarjeta …?» (añade «aplicando la tasa de cambio vigente» si cambian las
monedas) → autorización (huella con `local_auth`, o contraseña de pago cifrada con RSA PKCS1) → POST → pantalla de detalle
(o "Transferencia fallida"). Los importes se envían como texto (`$` y `,` quitados). El tipo de cambio lo calcula el
servidor (`tipo_cambio`, `discount`, `totalAmount`, `montoReal` en la respuesta).
**Respuesta**: `status_code`, `status_denom`, `transaction_signature` (la emite el servidor), `transaction_code`,
`pan`, `created_at`, `fault` si falla.
**Lista (Recientes/Historial)** — forma decodificada de `TransferList.fromJson` / `TransferModel.fromJson` (la app NO lee
paginación; pide `limit=10` y al llegar al final pide más con `offset += limit`, `getMoreInvoice`):
```
{ "transfer": [ TransferModel… ],          # el array
  "avatars":  { … },                       # mapa (la app lo exige: si falta, el cast a Map lanza); semántica sin confirmar
  "fault":    { … } }                      # solo en error
TransferModel: id_transfer, uuid, transaction_code, transaction_signature, status_code, status_denom, created_at,
  source{…}, recipient{…}, recipient_username, tipo_cambio, amount, totalAmount, discount, pan, funding_source_uuid,
  description, currency, transaction_uuid, status, bank_debit_detail (String), updated_at, barcode, merchant_uuid
source:    funding_source_amount, avatar, username, verified, name, lastname, bank, last4
recipient: last4, bank, avatar, name, lastname, phone, username, verified, funding_recipient_uuid, currency_recipient_denom
```
Presentación original de cada fila (`GenericListTranferWidget`, `setTitle/setAmount/setAvatar`):
- **Título** = destinatario (`BANMET(4646)` = `recipient.bank` + `(` + `recipient.last4` + `)`; si es una persona, nombre y
  apellido); **subtítulo** = el origen (`source.bank` + `(` + `source.last4` + `)` → `BANMET(6856)`).
- **Importe**: `amount` es un **string con signo** (`-1400.00` al enviar, `+…` al recibir): la app comprueba
  `startsWith("+")`, lo parsea, lo formatea con `NumberFormat.simpleCurrency` y le quita `$` y `,`; `currency` va aparte.
- **Estado**: `status_denom` comparado con `"Aceptada"` / `"Pendiente"` y pintado como etiqueta (ACEPTADA, en verde).
- **Fecha**: `created_at` se **recorta con `substring`** antes de parsearse con `DateTime`, así que la app muestra la hora
  tal cual la da el servidor (sin convertir zona) como `dd/MM/yyyy` + hora `hh:mm a`.
- **Avatar**: de usuario en `https://media.enzona.net/<avatar>`; de tarjeta, el logo del banco por código
  (`getBankPngByCode`).
**Detección de tarjeta**: `CardUtils.getCardTypeFrmNumber` (solo para el icono del tipo de tarjeta al escribir el PAN).

**"A una cuenta" y "A un amigo"** (🔎 decompilación; las dos convergen en el mismo POST):
- *A una cuenta*: `find_account_view` ("Buscar una cuenta") → campo de búsqueda (sin espacios; aparece el prefijo `+53`,
  así que admite usuario o teléfono) → `BUSCAR` → resultado con avatar y nombre → botón `TRANSFERIR` → `transfer_account_view`
  ("Transferir a una cuenta": `Importe`, `Descripción (Opcional)`, tarjeta de origen, `ACEPTAR`).
  Búsqueda: `GET /contact/1.0/api/account/user-name?…` → `AccountByUserName { userName, avatar, uuid, name, contact, account }`.
- *A un amigo*: `list_friends_to_transfer_view` ("Mis amigos"; vacío: «No tiene amigos añadidos»; los amigos se guardan en un
  almacén local `friendStore`) → mismo formulario (`transfer_friend_view`).
  Amigos: `GET /contact/1.0/api/account/contacts/` → `contacts[]` con `uuid, username, name, avatar, created_at, phone, email,
  primaryCurrency, account_uuid, contact_uuid, deleted`; solicitudes `GET …/all-contact-request/`; alta `POST …/contact-add`,
  baja `POST …/contact-remove-request`.
- **Cuerpo** de `POST /transfer/v1.0.0/transfer/account`: `amount`, `funding_recipient_username`, `funding_source_uuid`,
  `description`, `currency`, `funding_recipient_uuid`, `type_transfer`, y `fingerprint` o `payment_password` (cifrada).
- **Confirmación**: «¿Está seguro que desea transferir X de la tarjeta … (last4) a la cuenta de {nombre}?»; si las monedas
  difieren: «¿Desea transferir X de la tarjeta … a una tarjeta en {USD/CUP} aplicando la tasa de cambio vigente?».
- ✅ **De dónde sale el destinatario** (resuelto por decompilación: se mapeó cada campo del objeto con el `toJson` de su modelo):
  - *A una cuenta*: la búsqueda usa `GetAccountModelUseCase` (la misma consulta de cuenta del perfil,
    `GET /account/v1.0.0/account?username=<buscado>`). `funding_recipient_username` = `AccountModel.username` y
    **`funding_recipient_uuid` = `AccountModel.uuid`** (el uuid de la CUENTA del destinatario, no una tarjeta).
  - *A un amigo*: `funding_recipient_username` = `FriendModel.username` y **`funding_recipient_uuid` = `FriendModel.uuid`**
    (el `FriendModel` también trae `account_uuid` y `contact_uuid`, que NO se envían).
  - El flujo de pago por QR a cuenta (`scanear/ez_transfer_account_qr_scanned_view`) usa exactamente los mismos campos.
  - `funding_source_uuid` = uuid de la tarjeta de origen elegida y **`currency` = la moneda de esa tarjeta de origen** (no la del destino).
  - `type_transfer` se envía SIEMPRE como cadena vacía `""` (el literal aparece una sola vez en cada vista y nada lo reasigna);
    `fingerprint` también arranca como `""` y solo cambia en la rama de huella.
  - ⚠ Queda una inferencia: que `FriendModel.uuid` sea el uuid de la cuenta del amigo (lo sugiere que la misma clave del
    servidor recibe `AccountModel.uuid` en el otro flujo), pero no se ha visto una respuesta real de `contacts`.
- "**Destinatarios**" (`recipent/1.0.1/recipient/{add,setRecipient,remove}` + listado) son beneficiarios de tarjeta guardados
  (`pan, userName, phone, last4, bankCode, currency, description, uuid`): el icono de tarjeta junto a "No. Tarjeta" abre ese selector.

⚠ Sin verificar: forma real de la lista y de `fault`, qué valores admite `status_filter`, si la contraseña mal escrita
bloquea la cuenta, y límites de importe.

### Archivo bruto
La extracción completa de campos por cada modelo está en
[`APENDICE-campos-por-modelo.txt`](APENDICE-campos-por-modelo.txt) (todos los módulos) y la de endpoints en
[`APENDICE-endpoints-por-datasource.txt`](APENDICE-endpoints-por-datasource.txt). Las decompilaciones por librería
(`asm/`) y el object pool (`pp.txt`) son la salida local de Blutter, que no se versiona: ver `10-metodologia.md`, 10b.
