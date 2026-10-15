import Foundation
var failures = 0
func eq<T: Equatable>(_ a: T, _ b: T, _ what: String) { if a != b { failures += 1; print("FALLA \(what): esperaba \(b), obtuve \(a)") } }

// ---- Espejos de los enums del dominio ----
eq(EzTransferStatus(kotlinName: "ACCEPTED"), .accepted, "nombre Kotlin conocido")
eq(EzOperationKind(kotlinName: "CARD_ACTIVATION"), .cardActivation, "nombre con guion bajo")
eq(EzCardSkin(kotlinName: "BANDEC_USD"), .bandecUsd, "skin con moneda")
eq(EzNotSentReason(kotlinName: "ANOTHER_TRANSFER_IN_PROGRESS"), .anotherTransferInProgress, "motivo largo")
eq(EzResultKind(kotlinName: "NOT_SENT"), .notSent, "clasificación del dominio")
eq(Set(EzBankBrand.allCases.map { $0.rawValue }).count, EzBankBrand.allCases.count, "valores crudos únicos")

let T = TransferPresentation.self
eq(T.partyLabel(name: nil, username: nil, bank: "BANMET", last4: "6856"), "BANMET(6856)", "banco y dígitos")
eq(T.partyLabel(name: nil, username: nil, bank: nil, last4: "6856"), "Tarjeta (6856)", "solo dígitos")
eq(T.partyLabel(name: nil, username: "ana", bank: "BPA", last4: nil), "ana", "usuario antes que banco")
eq(T.partyLabel(name: "Ana Pérez", username: "ana", bank: nil, last4: nil), "Ana Pérez", "nombre")
eq(T.partyLabel(name: nil, username: nil, bank: "BPA", last4: nil), "BPA", "solo banco")
eq(T.partyLabel(name: " ", username: nil, bank: nil, last4: nil), "—", "nada")
eq(T.amountText(direction: .outgoing, minorUnits: 140_000), "-1400.00", "saliente")
eq(T.amountText(direction: .incoming, minorUnits: 140_000), "+1400.00", "entrante")
eq(T.amountText(direction: .unknown, minorUnits: 140_000), "1400.00", "sin sentido")
eq(T.amountTone(direction: .outgoing), .negative, "tono saliente"); eq(T.amountTone(direction: .incoming), .positive, "tono entrante")
eq(T.amountTone(direction: .unknown), .neutral, "tono sin sentido")
eq(T.wallClock(year: 2026, month: 3, day: 5, hour: 0, minute: 7), "05/03/2026 12:07 AM", "medianoche")
eq(T.wallClock(year: 2026, month: 3, day: 5, hour: 11, minute: 59), "05/03/2026 11:59 AM", "11:59")
eq(T.wallClock(year: 2026, month: 3, day: 5, hour: 12, minute: 0), "05/03/2026 12:00 PM", "mediodía")
eq(T.wallClock(year: 2026, month: 10, day: 14, hour: 16, minute: 30), "14/10/2026 04:30 PM", "tarde")
eq(T.statusLabel(status: .accepted, statusText: nil), "Aceptada", "aceptada")
eq(T.statusLabel(status: .pending, statusText: nil), "Pendiente", "pendiente")
eq(T.statusLabel(status: .failed, statusText: nil), "Fallida", "fallida")
eq(T.statusLabel(status: .other, statusText: "Reversada"), "Reversada", "otro con texto")
eq(T.statusLabel(status: .other, statusText: nil), "Sin estado", "otro sin texto")
eq(T.statusTone(status: .accepted) == .success, true, "tono aceptada"); eq(T.statusTone(status: .failed) == .error, true, "tono fallida")
eq(T.initials("ana pérez gómez"), "AP", "dos palabras"); eq(T.initials("juan"), "J", "una"); eq(T.initials("  "), "?", "vacío")
eq(T.logo(brand: .banmet), "bank_banmet", "logo"); eq(T.logo(brand: .unknown), "avatar_placeholder", "banco desconocido")
eq(T.invalidQuery(.empty), "Escribe el nombre de usuario de la cuenta.", "búsqueda vacía")
eq(T.invalidQuery(.hasWhitespace), "El nombre de usuario no lleva espacios.", "búsqueda con espacios")
eq(T.invalidQuery(.yourself).contains("A una de mis tarjetas"), true, "búsqueda de uno mismo")
eq(T.displayDay(iso: "2026-10-01"), "01/10/2026", "día"); eq(T.displayDay(iso: "raro"), "raro", "día ilegible")
var cal = Calendar(identifier: .gregorian); cal.timeZone = TimeZone(identifier: "Pacific/Kiritimati")!
let d = T.date(iso: "2026-10-01", calendar: cal)!
eq(T.isoDay(d, calendar: cal), "2026-10-01", "ida y vuelta del día en zona extrema")
let rows = TransferRowModel.samples
eq(rows[0].spoken, "BPA(1234), desde BANMET(6856), -1400.00 CUP, Aceptada, 14/10/2026 04:30 PM", "VoiceOver")
eq(rows[5].logoName, "avatar_placeholder", "persona sin logo")

// ---- Operaciones de tarjeta ----
let P = OperationPresentation.self
eq(P.title(kind: .transfer, merchantAlias: "no debe usarse"), "Transferencia", "alias ignorado")
eq(P.title(kind: .merchantPayment, merchantAlias: "Tienda La Esquina"), "Tienda La Esquina", "alias de comercio")
eq(P.title(kind: .merchantPayment, merchantAlias: nil), "Pago a comercio", "comercio sin alias")
eq(P.title(kind: .unknown, merchantAlias: nil), "Operación", "tipo desconocido")
for kind in EzOperationKind.allCases { eq(P.icons[kind] != nil, true, "icono de \(kind.rawValue)") }
eq(P.icon(kind: .reversal), "op_transfer", "reverso como transferencia")
eq(P.cardDetailLabel(skin: .bandecUsd, bankName: "x", last4: "4912"), "BANDEC 4912", "bandec usd")
eq(P.cardDetailLabel(skin: .clasica, bankName: "x", last4: "3130"), "Clásica 3130", "clásica")
eq(P.cardDetailLabel(skin: .generic, bankName: "Banco Desconocido", last4: "0042"), "Banco Desconocido 0042", "banco desconocido")
eq(P.cardDetailLabel(skin: .generic, bankName: " ", last4: "0042"), "Tarjeta 0042", "sin nombre")

// ---- A una de mis tarjetas ----
let O = OwnCardPresentation.self
eq(O.moneyShown(minorUnits: 1_050, currency: "cup"), "$10.50 CUP", "importe con símbolo")
eq(O.amountPlain(minorUnits: 1_050, currency: "cup"), "10.50 CUP", "importe sin símbolo")
eq(O.sourceSuffix(last4: "6856", currency: "cup"), "(6856) CUP", "línea de la tarjeta de origen")
eq(O.amountProblem(.zero), "El monto no puede ser $0.00", "monto cero")
eq(O.amountProblem(.hasWhitespace), "El monto no puede contener espacios en blanco", "monto con espacios")
eq(O.amountProblem(.invalid), "Introduzca un monto válido", "monto inválido"); eq(O.amountProblem(.empty), "Introduzca un monto válido", "monto vacío")
eq(O.confirmation(amount: "$10.50 CUP", source: "BANMET 6856", destination: "BANDEC 1234", destinationCurrency: "cup", exchangeApplies: false),
   "¿Está seguro que desea transferir $10.50 CUP de la tarjeta BANMET 6856 a la tarjeta BANDEC 1234?", "confirmación")
eq(O.confirmation(amount: "$10.50 CUP", source: "BANMET 6856", destination: "BANDEC 9021", destinationCurrency: "usd", exchangeApplies: true),
   "¿Desea transferir $10.50 CUP de la tarjeta BANMET 6856 a una tarjeta en USD aplicando la tasa de cambio vigente?", "confirmación con cambio")
eq(O.notSent(.passwordEmpty), "Escribe tu contraseña de pago.", "contraseña vacía")
eq(O.notSent(.anotherTransferInProgress), "Hay otra transferencia en curso. Espera a que termine.", "otra en curso")
for reason in EzNotSentReason.allCases { eq(O.notSent(reason).isEmpty, false, "mensaje de \(reason.rawValue)") }
func res(_ outcome: EzOutcome, _ kind: EzResultKind) -> OwnCardPresentation.Result { O.result(outcome: outcome, kind: kind) }
eq(res(.accepted(status: .accepted, statusText: nil), .success).title, "Transferencia confirmada", "título confirmada")
eq(res(.accepted(status: .pending, statusText: nil), .pending).title, "Transferencia pendiente", "título pendiente")
eq(res(.accepted(status: .other, statusText: "Procesando"), .pending).detail?.contains("Procesando"), true, "estado desconocido visible")
eq(res(.accepted(status: .accepted, statusText: nil), .success).kind, .success, "el tipo es el que da el dominio")
eq(res(.rejected(cause: .refused, httpStatus: 400, serverMessage: "Saldo insuficiente"), .failed).detail, "Saldo insuficiente\nNo se movió dinero.", "rechazo con texto")
eq(res(.rejected(cause: .refused, httpStatus: 403, serverMessage: nil), .failed).detail?.contains("403"), true, "rechazo con código")
eq(res(.rejected(cause: .sessionExpired, httpStatus: 401, serverMessage: nil), .failed).detail?.contains("sesión"), true, "sesión expirada")
eq(res(.rejected(cause: .refused, httpStatus: 400, serverMessage: nil), .failed).title, "Transferencia fallida", "título fallida")
eq(res(.unknown, .unknown).detail?.contains("No repitas"), true, "no repetir")
eq(res(.unknown, .unknown).title.lowercased().contains("fallida"), false, "desconocido no dice fallida")
eq(res(.notSent(.passwordEmpty), .notSent).title, "No se envió", "no enviado")
eq(O.rows(operationCode: "kf1", description: "Ahorro"), [OwnCardPresentation.Row(label: "No.Operación", value: "kf1"), OwnCardPresentation.Row(label: "Descripción", value: "Ahorro")], "filas completas")
eq(O.rows(operationCode: nil, description: "  "), [], "sin filas")
eq(O.dateText(wallClock: (2026, 9, 11, 11, 30), receivedText: "x"), "11/09/2026 11:30 AM", "fecha de reloj")
eq(O.dateText(wallClock: nil, receivedText: "ayer"), "ayer", "fecha recibida"); eq(O.dateText(wallClock: nil, receivedText: nil), nil, "sin fecha")
let accepted = TransferDetailData.accepted
eq(accepted.kind, .success, "comprobante confirmado"); eq(accepted.amountText, "10.50 CUP", "importe del comprobante")
eq(accepted.dateText, "11/09/2026 11:30 AM", "fecha del comprobante"); eq(accepted.qrContent, "SIG-INVENTADA-0001", "QR")
eq(accepted.rows.map { $0.label }, ["No.Operación", "Descripción"], "filas del comprobante")
eq(TransferDetailData.pending.kind, .pending, "comprobante pendiente")
eq(TransferDetailData.rejected.qrContent, nil, "fallida sin QR"); eq(TransferDetailData.rejected.dateText, nil, "fallida sin fecha")
eq(TransferDetailData.unknown.rows.map { $0.label }, ["Descripción"], "desconocida sin número de operación")
print(failures == 0 ? "OK: todos los casos pasan" : "\(failures) fallos"); exit(failures == 0 ? 0 : 1)
