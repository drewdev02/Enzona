import Foundation
var failures = 0
func eq<T: Equatable>(_ a: T, _ b: T, _ what: String) { if a != b { failures += 1; print("FALLA \(what): esperaba \(b), obtuve \(a)") } }
let T = TransferPresentation.self
eq(T.partyLabel(name: nil, username: nil, bank: "BANMET", last4: "6856"), "BANMET(6856)", "banco y dígitos")
eq(T.partyLabel(name: nil, username: nil, bank: nil, last4: "6856"), "Tarjeta (6856)", "solo dígitos")
eq(T.partyLabel(name: nil, username: "ana", bank: "BPA", last4: nil), "ana", "usuario antes que banco")
eq(T.partyLabel(name: "Ana Pérez", username: "ana", bank: nil, last4: nil), "Ana Pérez", "nombre")
eq(T.partyLabel(name: nil, username: nil, bank: "BPA", last4: nil), "BPA", "solo banco")
eq(T.partyLabel(name: " ", username: nil, bank: nil, last4: nil), "—", "nada")
eq(T.amountText(direction: "OUTGOING", minorUnits: 140_000), "-1400.00", "saliente")
eq(T.amountText(direction: "INCOMING", minorUnits: 140_000), "+1400.00", "entrante")
eq(T.amountText(direction: "UNKNOWN", minorUnits: 140_000), "1400.00", "sin sentido")
eq(T.wallClock(year: 2026, month: 3, day: 5, hour: 0, minute: 7), "05/03/2026 12:07 AM", "medianoche")
eq(T.wallClock(year: 2026, month: 3, day: 5, hour: 11, minute: 59), "05/03/2026 11:59 AM", "11:59")
eq(T.wallClock(year: 2026, month: 3, day: 5, hour: 12, minute: 0), "05/03/2026 12:00 PM", "mediodía")
eq(T.wallClock(year: 2026, month: 10, day: 14, hour: 16, minute: 30), "14/10/2026 04:30 PM", "tarde")
eq(T.statusLabel(status: "ACCEPTED", statusText: nil), "Aceptada", "aceptada")
eq(T.statusLabel(status: "PENDING", statusText: nil), "Pendiente", "pendiente")
eq(T.statusLabel(status: "FAILED", statusText: nil), "Fallida", "fallida")
eq(T.statusLabel(status: "OTHER", statusText: "Reversada"), "Reversada", "otro con texto")
eq(T.statusLabel(status: "OTHER", statusText: nil), "Sin estado", "otro sin texto")
eq(T.initials("ana pérez gómez"), "AP", "dos palabras"); eq(T.initials("juan"), "J", "una"); eq(T.initials("  "), "?", "vacío")
eq(T.logo(brand: "BANMET"), "bank_banmet", "logo"); eq(T.logo(brand: "UNKNOWN"), "avatar_placeholder", "banco desconocido")
eq(T.displayDay(iso: "2026-10-01"), "01/10/2026", "día"); eq(T.displayDay(iso: "raro"), "raro", "día ilegible")
var cal = Calendar(identifier: .gregorian); cal.timeZone = TimeZone(identifier: "Pacific/Kiritimati")!
let d = T.date(iso: "2026-10-01", calendar: cal)!
eq(T.isoDay(d, calendar: cal), "2026-10-01", "ida y vuelta del día en zona extrema")
let rows = TransferRowModel.samples
eq(rows[0].spoken, "BPA(1234), desde BANMET(6856), -1400.00 CUP, Aceptada, 14/10/2026 04:30 PM", "VoiceOver")
eq(rows[5].logoName, "avatar_placeholder", "persona sin logo")
print(failures == 0 ? "OK: todos los casos pasan" : "\(failures) fallos"); exit(failures == 0 ? 0 : 1)
