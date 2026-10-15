# 04 — Modelo de datos

> Inventario de entidades de dominio y modelos (DTO) por módulo.
>
> ⚠️ **Los campos JSON exactos ya fueron recuperados** con Blutter (Dart 3.3.4) — ver
> [`06-esquemas-exactos.md`](06-esquemas-exactos.md) para los nombres reales y los
> `data class` de KMP, y [`APENDICE-campos-por-modelo.txt`](APENDICE-campos-por-modelo.txt)
> para el volcado completo. Este documento (04) es el **mapa de qué entidades existen**;
> el 06 tiene **los campos**.

## Núcleo transversal

### `TokenEntity` / `TokenModel` (security)
Campos esperables: `accessToken`, `refreshToken`, `tokenType` (`Bearer`), `expiresIn`,
`idToken`, `scope`, `issuedAt`. → Secure storage.

### `Profile` / `ProfileModel` (security)
Identidad OIDC básica del usuario logueado (sub, nombre, username, avatar).

### `Account` / `AccountModel` (account)
Datos de la cuenta/billetera: id, username, nombre, email, teléfono, estado de
verificación, flags (TOTP habilitado, contraseña de pago configurada), avatar
(`media.enzona.net/images/user/avatar/<id>`).

### `Destinatario` / `DestinatarioModel` (account)
Beneficiario guardado: alias, tipo (cuenta/tarjeta), número, titular.

### `VerifyAccount` (account)
Datos del flujo de verificación: canal (phone/email), código, estado.

## Dinero y pagos

### `Card` / `CardModel` (card)
`number` (enmascarado), `type` (ver `card_type.dart`), `holder`, `bank` (`Bank`),
`balance` (`Balance`), `isDefault`, `coordinate` (`Cordenate`), `operations` (`Operation[]`).

### `Balance` (card)
`amount`, `currency` (CUP/MLC), `available`.

### `Transfer` / `TransferModel` (transfer)
`sourceCardOrAccount`, `destination`, `amount`, `currency`, `concept`, `date`, `status`.

### `PaymentFincimex` (transfer)
Pago/transferencia específica a red Fincimex: tarjeta destino, monto, merchant.

### `Transaction` / `TransactionModel` (transaction)
`id`, `type`, `amount`, `currency`, `date`, `status`, `source` (`Source`),
`recipient` (`Recipient`), `concept`, `reference`.

### `QRCode` / `QrcodeModel` + `QrFincimex` (scanear/common)
Carga del QR: tipo (comercio/persona/atm/fincimex/transfermovil), id vendedor/cuenta,
monto opcional. `AtmInfo` para extracción en cajero.

## Servicios y facturas (plantilla común)

Patrón repetido en etecsa / electricidad / gas / impuestos:

- **Cliente de servicio**: id de contrato + alias (p. ej. `etecsa.dart`, gas client, electric id).
- **Factura/Bill**: `period`, `amount`, `dueDate`, `status`, `concept`
  (`Invoice`/`InvoiceCharged`, `ElectricBill`/`ElectricMonthBill`/`ElectricPayedBill`,
  `GasBill`/`GasMonthBill`/`GasPayedBill`, `Tax`/`TaxCharged`/`TaxHistorical`).
- **Catálogos geográficos/bancarios** (tax/bank_account): `Province`, `Municipality`,
  `BankBranch`, `TaxAccount`, `TaxPayer`, `Contribuyente`, `TaxActivity`, `TaxInfoGeneral`.

## Social / marketplace

- **Bulevar**: `Merchant`, `Product`, `Order`/`OrderDetail`, `Beneficiary`, `AnyImage`.
- **Friends**: `Friends`, `AccountByUserName`.
- **Cuban email / giro**: `CubanEmail`, `Giro`, `CubanEmailAmount`, `MoneyOrderSent`.
- **Donation**: `Donation`, `SourceDonation`, `RecipientDonation`.
- **Gift**: `Gift`, `CategoryGift`, `SourceGift`, `RecipientGift`.
- **Social organizations (PCC/UIC)**: `MilitantPcc`, `CuotaPcc`, `PccPayment`, `UicPayment`,
  `OcUser`, `Price`.

## Otros modelos (common)

`InfoApkModel` (auto-update), `ClaimModel` (reclamación), `ClientServiceModel`,
`PayedBillModel`, `BankDebitDetailModel`, `ContactenosHelpModel`, `NotificationEntity`,
`EzItem` (ítems del dashboard/home).

---

### Convención de serialización en KMP

```kotlin
@Serializable
data class CardDto(
    @SerialName("number") val number: String,
    @SerialName("holder") val holder: String? = null,
    @SerialName("balance") val balance: BalanceDto? = null,
    @SerialName("is_default") val isDefault: Boolean = false,
    // ...confirmar nombres reales con el backend
)
```
Mapear `*Dto` (data) → entidad de dominio en el `RepositoryImpl`, como hacía el original
con `*Model` → entidad.
