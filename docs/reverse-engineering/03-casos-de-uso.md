# 03 — Épicas, historias de usuario y casos de uso

Formato: cada **Épica** agrupa historias de usuario (`Como … quiero … para …`) con sus
**criterios de aceptación** (Given/When/Then resumido) y los **use cases** reales del
código original que las implementan. Esto es lo que portarás a `shared/feature/<x>` en KMP.

---

## EPIC-01 · Identidad y acceso (módulo `security` + `account`)

**HU-01.1 — Login OAuth**
Como usuario quiero iniciar sesión con mis credenciales EnZona para acceder a mi billetera.
- Given tengo cuenta, When abro la app y me autentico en el portal, Then recibo token y
  entro al dashboard.
- Flujo: Authorization Code + PKCE (ver `02-api-reference §0`). UC: carga token → secure storage.

**HU-01.2 — Desbloqueo biométrico**
Como usuario quiero abrir la app con huella/rostro para no escribir la contraseña cada vez.
- UC: `local_auth`; si falla huella → mensaje "configure nuevamente su huella o resetee…".

**HU-01.3 — Mantener sesión / refresh**
Como usuario quiero que mi sesión se renueve sola mientras el refresh token sea válido.
- UC: `refresh_token` automático en el cliente HTTP.

**HU-01.4 — Cerrar sesión / limpiar caché**
- UC: `security_controller`, `cache_manager`.

**Criterios transversales**: token nunca en logs; almacenado en Keychain/Keystore.

---

## EPIC-02 · Registro y verificación de cuenta (`account`)

**HU-02.1** Verificar cuenta → `verify_account_use_case`, `verify_code_use_case`
(`POST account/verify`).
**HU-02.2** Verificar teléfono con código SMS → `send_verify_phone_code_use_case` +
`verify_phone_use_case` (`phone/code/number`, `phone/verified`).
**HU-02.3** Actualizar email/teléfono → `update_email_use_case`, `update_email_phone_usecase`.
**Vistas**: `verify_account_form_view`, `ez_code_verification_view`,
`verification_code_phone_screen`.

---

## EPIC-03 · Perfil y configuración (`account`)

- **HU-03.1** Ver/editar perfil → `get_account_model_use_case`, `update_account_use_case`
  (`ez_edit_profile_view`, `ez_profile_info_view`).
- **HU-03.2** Cambiar contraseña de acceso → `change_account_pass_use_case`
  (`ez_account_change_password_view`).
- **HU-03.3** Gestionar contraseña de pago → `reset_payment_password_use_case`
  (`ez_payment_password_view`, `ez_profile_reset_payment_password_view`).
- **HU-03.4** 2FA TOTP → `get_totp_code_use_case`, `get_verify_totp_use_case`,
  `disable_totp_use_case` (`ez_profile_totp_view`, `ez_profile_totp_saved_view`).
- **HU-03.5** Configuración de seguridad y de pago → `ez_profile_configurations_*`.
- **HU-03.6** Mi QR de cobro → `my_qr_window_view`.
- **HU-03.7** Términos, ayuda, contáctenos → `contactenos_use_case`,
  `terminos_condiciones_view`, `ez_ayuda_apk_view`, `help_view`.

---

## EPIC-04 · Tarjetas (`card`)

- **HU-04.1** Asociar/ver tarjetas (`coremz/card`).
- **HU-04.2** Establecer tarjeta por defecto (`cards/default`).
- **HU-04.3** Activar tarjeta por PIN (`card/card/activate/pin`).
- Utilidades: `card_type.dart`, `card_utils.dart` (detección de tipo/validación tipo Luhn).

---

## EPIC-05 · Transferencias (`transfer`)

**HU-05.1 — Transferir a cuenta EnZona**
Como usuario quiero enviar dinero a otra cuenta EnZona para pagar a una persona.
- UC: `transfer_to_account_use_case` → `POST transfer/account`.
- Vistas: `transfer_account_view`, `find_account_view`, `transfer_to_account_detail_view`.

**HU-05.2 — Transferir a tarjeta** → `transfer_to_card_use_case` (`transfer/card`).
**HU-05.3 — Transferir a tarjeta Fincimex** → `transfer_to_card_fincimex_use_case`
(`coremz/transfer/fincimex`); vistas `transfer_to_card_fincimex_detail_view`,
`fincimex_payment_merchant_detail_view`.
**HU-05.4 — Elegir destinatario** desde amigos/agenda/QR → `destinatarios_picker`,
`list_friends_to_transfer_view`, `search_transfer_friends`.
**HU-05.5 — Historial y filtros** → `filter_transfer_use_case`, `historical_tranfer_view`,
filtros por fecha/estado (`transfer_historial_*_filter`).

Criterios: confirmar con **contraseña de pago** y/o **biometría**; mostrar detalle y
comprobante descargable.

---

## EPIC-06 · Pagos por QR (`scanear`)

**HU-06.1 — Pagar a comercio escaneando QR** → `add_payment_qr_code_use_case`,
`pay_to_comercio_by_qr_scanned_use_case`; vistas `ez_qr_scan_view`,
`ez_qr_comercio_payment_scanned_view`, `transfer_to_comercio_details_view`.
**HU-06.2 — Pago Fincimex por QR** → `fincimex_qr_comercio_payment_scanned_view`.
**HU-06.3 — Transferir a persona por QR** → `ez_transfer_account_qr_scanned_view`,
`find_friend_by_qr_view`, `transfer_card_by_qr_transfermovil_view` (compatibilidad Transfermóvil).
**HU-06.4 — Extracción en ATM por QR** → `add_atm_extraction` (`service-core/atm`),
`ez_qr_atm_scanned`.
**HU-06.5 — Cobrar: generar QR con monto** → `qr_especificar_monto_view`,
`qr_recieve_code_view`, `get_vendor_id_code_use_case`.

Requiere permiso `CAMERA`. KMP: CameraX+MLKit (Android) / AVFoundation (iOS); QR propio con zxing.

---

## EPIC-07 · Cuentas bancaria y fiscal (`bank_account`)

- **HU-07.1** Vincular/ver cuenta bancaria → `bank_account_controller`, `bank_account_view`.
- **HU-07.2** Cuenta fiscal (ONAT) → `fiscal_account_controller`,
  `request_tax_account_view`, `fiscal_account_view`.

---

## EPIC-08 · Pago de servicios

**08.A ETECSA / Nauta (`etecsa`)**
- Añadir/editar/eliminar id de cliente → `add/delete/list_client_id_use_case`.
- Consultar factura mensual → `get_fact_mensual_etecsa_use_case`.
- Pagar factura → `pay_fact_etecsa_use_case`; recargar tarjeta propia →
  `recargar_tarjeta_propia_view`; servicio Nauta → `pay_etecsa_nauta_service_view`.
- Histórico/filtros → `filter_invoices_use_case`.

**08.B Electricidad (`electricity_service`)** — añadir id cliente, buscar/listar factura,
pagar, **pago automático** (`automatic_pay_view`), histórico.

**08.C Gas (`gas_service`)** — configurar cliente, buscar/listar factura de gas, pagar, histórico.

**08.D Impuestos ONAT / RC-05 (`tax_service`)** — alta RC-05, cuenta fiscal, contribuyente
(`tax_payer_view`), buscar/pagar impuesto, histórico.

Patrón común (plantilla reutilizable en KMP): `ServicioCliente(id)` → `buscarFactura` →
`detalleFactura` → `pagar(factura, medioPago, passPago)` → `comprobante` + `historial`.

---

## EPIC-09 · Social: amigos, destinatarios, organizaciones, bulevar

- **Amigos (`friends`)**: añadir/eliminar, solicitudes, buscar por username →
  `add/delete_friend_use_case`, `get_solicitudes_use_case`,
  `filter_account_by_username_use_case`.
- **Destinatarios (`account`)**: CRUD → `add/update/delete/get_destinatarios_use_case`.
- **Bulevar (`bulevar`)**: listar tiendas, ver comercio, mis compras, detalle de orden →
  `list_merchant_use_case`, `filter_order_use_case`.
- **Organizaciones sociales (`social_organizations`)**: datasource `so_remote_datasource_impl`.

---

## EPIC-10 · Giros, donaciones y regalos

- **Giro / money order (`cuban_email`)**: enviar giro (`send_giro_view`),
  configurar destinatarios, histórico y detalle (`money_order_*`), pagar (`cuban_email_pay_view`).
- **Donaciones (`donation`)**: `donar_view`, `donation_view`.
- **Regalos (`gift`)**: `gift_controller`.

---

## EPIC-11 · Transacciones (`transaction`)

- **HU-11.1** Ver historial de transacciones → `transaction_list_page`.
- **HU-11.2** Ver detalle de una transacción → `transaction_detail_page`.
- Controllers: `transaction_controllers`, `transaction_controller_new`.

---

## EPIC-12 · Home / Dashboard (`home`)

- Dashboard con saldo, accesos rápidos, contactos → `dashboard_view`, `home_view`,
  `profile_view`, `ez_contacts_view`.
- Punto de entrada tras login; orquesta navegación a los demás módulos.

---

## EPIC-13 · Notificaciones y plataforma (`notification_service`, core)

- **HU-13.1** Recibir push (ntfy) → `notification_controller`, `notification_page`.
- **HU-13.2** Notificaciones locales (`flutter_local_notifications`).
- **HU-13.3** Descargar comprobantes/archivos (`download_manager_service`).
- **HU-13.4** Auto-actualización de la app (`update_apk_remote_datasource`).
- **HU-13.5** Reclamaciones y rating (`claim_remote_datasource`, `rating_remote_datasource`).
- **HU-13.6** Estado de red / offline (`network_info_service`).

---

### Matriz módulo → épica (checklist de portado KMP)

| Módulo original | Épica(s) |
|---|---|
| security | 01, 13 |
| account | 02, 03, 05(destinatarios) |
| card | 04 |
| transfer | 05 |
| scanear | 06 |
| bank_account | 07 |
| etecsa / electricity_service / gas_service / tax_service | 08 |
| friends / bulevar / social_organizations | 09 |
| cuban_email / donation / gift | 10 |
| transaction | 11 |
| home | 12 |
| notification_service / common | 13 |
