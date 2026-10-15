import Foundation

/// Presentación de una transferencia. Los textos, formatos e imágenes son de la UI (el dominio solo guarda
/// hechos). Sin dependencias del framework Kotlin: se verifica con un script y se puede previsualizar. Mismas
/// reglas que `TransferPresentation.kt` (Android): título = destinatario, subtítulo = origen, `dd/MM/yyyy hh:mm a`.
/// Los tipos son los espejos de `EzKinds.swift` (`EzTransferDirection`, `EzTransferStatus`, `EzBankBrand`).
enum TransferPresentation {
    /// `BANMET(6856)`; sin banco o sin últimos dígitos, lo que haya (usuario, nombre...). Nunca vacío.
    static func partyLabel(name: String?, username: String?, bank: String?, last4: String?) -> String {
        func clean(_ text: String?) -> String? {
            guard let text, !text.trimmingCharacters(in: .whitespaces).isEmpty else { return nil }
            return text
        }
        let bankText = clean(bank)
        let digits = clean(last4)
        if let bankText, let digits { return "\(bankText)(\(digits))" }
        if let digits { return "Tarjeta (\(digits))" }
        return clean(name) ?? clean(username) ?? bankText ?? "—"
    }

    /// El importe llega como magnitud; el signo sale del sentido. Sin sentido conocido no se inventa signo.
    static func amountText(direction: EzTransferDirection, minorUnits: Int64) -> String {
        let sign: String
        switch direction {
        case .outgoing: sign = "-"
        case .incoming: sign = "+"
        case .unknown: sign = ""
        }
        var magnitude = OperationPresentation.formatMinorUnits(minorUnits)
        if magnitude.hasPrefix("-") { magnitude.removeFirst() }
        return sign + magnitude
    }

    /// Hora de reloj tal cual la dio el servidor (no se convierte a la zona del teléfono).
    static func wallClock(year: Int, month: Int, day: Int, hour: Int, minute: Int) -> String {
        func two(_ n: Int) -> String { n < 10 ? "0\(n)" : "\(n)" }
        let hour12 = hour % 12 == 0 ? 12 : hour % 12
        let suffix = hour < 12 ? "AM" : "PM"
        var yearText = String(year)
        while yearText.count < 4 { yearText = "0" + yearText }
        return "\(two(day))/\(two(month))/\(yearText) \(two(hour12)):\(two(minute)) \(suffix)"
    }

    static func statusLabel(status: EzTransferStatus, statusText: String?) -> String {
        switch status {
        case .accepted: return "Aceptada"
        case .pending: return "Pendiente"
        case .failed: return "Fallida"
        case .other:
            let text = statusText?.trimmingCharacters(in: .whitespaces) ?? ""
            return text.isEmpty ? "Sin estado" : text
        }
    }

    static func statusTone(status: EzTransferStatus) -> Ez.Tone {
        switch status {
        case .accepted: return .success
        case .pending: return .warning
        case .failed: return .error
        case .other: return .neutral
        }
    }

    static func amountTone(direction: EzTransferDirection) -> EzAmountTone {
        switch direction {
        case .outgoing: return .negative
        case .incoming: return .positive
        case .unknown: return .neutral
        }
    }

    /// Texto para lectores de pantalla: toda la fila en una frase.
    static func spoken(title: String, subtitle: String, amount: String, currency: String, status: String, date: String) -> String {
        [title, "desde \(subtitle)", "\(amount) \(currency)", status, date].filter { !$0.isEmpty }.joined(separator: ", ")
    }

    /// Por qué no se hizo la búsqueda de una cuenta (mismos textos que `InvalidQuery.message()` de Android).
    static func invalidQuery(_ reason: EzInvalidQuery) -> String {
        switch reason {
        case .empty: return "Escribe el nombre de usuario de la cuenta."
        case .hasWhitespace: return "El nombre de usuario no lleva espacios."
        case .yourself: return "Esa es tu cuenta. Para mover dinero entre tus tarjetas usa \"A una de mis tarjetas\"."
        }
    }

    /// Iniciales para el avatar de una persona (sin foto): hasta dos palabras.
    static func initials(_ displayName: String) -> String {
        let letters = displayName.split(whereSeparator: { $0.isWhitespace }).prefix(2).compactMap { $0.first }
        let text = String(letters).uppercased()
        return text.isEmpty ? "?" : text
    }

    /// Banco -> logo. Un banco que no conocemos (`.unknown`) no tiene entrada: no se adivina. Debe coincidir con
    /// `BankLogos.kt` (Android); `tools/check_assets.py` lo comprueba.
    static let bankLogos: [EzBankBrand: String] = [
        .banmet: "bank_banmet",
        .bandec: "bank_bandec",
        .bpa: "bank_bpa",
        .bicsa: "bank_bicsa",
        .clasica: "bank_clasica",
        .ais: "bank_ais",
    ]

    /// Logo del banco, o el avatar neutro si no se reconoce.
    static func logo(brand: EzBankBrand) -> String { bankLogos[brand] ?? "avatar_placeholder" }

    /// `yyyy-MM-dd` del día de calendario elegido (sin pasar por la zona horaria).
    static func isoDay(_ date: Date, calendar: Calendar = .current) -> String {
        let c = calendar.dateComponents([.year, .month, .day], from: date)
        func two(_ n: Int) -> String { n < 10 ? "0\(n)" : "\(n)" }
        var year = String(c.year ?? 0)
        while year.count < 4 { year = "0" + year }
        return "\(year)-\(two(c.month ?? 0))-\(two(c.day ?? 0))"
    }

    /// `dd/MM/yyyy` a partir de `yyyy-MM-dd`; si no tiene esa forma se devuelve tal cual.
    static func displayDay(iso: String) -> String {
        let parts = iso.split(separator: "-")
        guard parts.count == 3, parts[0].count == 4, parts[1].count == 2, parts[2].count == 2 else { return iso }
        return "\(parts[2])/\(parts[1])/\(parts[0])"
    }

    /// El día elegido como `Date` al mediodía local (para inicializar el selector sin saltos de día).
    static func date(iso: String, calendar: Calendar = .current) -> Date? {
        let parts = iso.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        return calendar.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2], hour: 12))
    }
}
