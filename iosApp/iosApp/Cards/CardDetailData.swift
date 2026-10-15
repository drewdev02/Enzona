import Foundation

/// Una fila de "Últimas operaciones", ya lista para pintar. Modelo propio de la UI (sin el framework
/// Kotlin) para que las vistas se puedan previsualizar.
struct OperationRowModel: Identifiable {
    let id: Int
    let title: String
    let subtitle: String
    let iconName: String
    /// Con signo, sin moneda ("-950.00").
    let amountText: String
    let currency: String
    let dateText: String
    /// Lo que leen los lectores de pantalla: la fila entera como UN elemento.
    let spoken: String
}

/// Saldos y operaciones de una tarjeta. Los textos de saldo son `nil` si el backend no los trajo legibles.
struct CardActivityData {
    /// "$3395.15" y su moneda por separado (la moneda va más pequeña).
    let availableText: String?
    let availableCurrency: String
    let bookText: String?
    let operations: [OperationRowModel]
    /// Operaciones que no se pudieron leer: no se ocultan, la lista puede estar incompleta.
    let skipped: Int
}

enum CardDetailPhase {
    case loading
    case error(String)
    case content(CardActivityData)
}

extension CardActivityData {
    /// Datos INVENTADOS para previews: una operación de cada tipo, con la forma de la respuesta real.
    static let sample: CardActivityData = {
        func row(_ id: Int, _ kind: EzOperationKind, debit: Bool, minor: Int64, date: String, alias: String? = nil) -> OperationRowModel {
            let title = OperationPresentation.title(kind: kind, merchantAlias: alias)
            let label = OperationPresentation.label(isDebit: debit)
            let amount = OperationPresentation.sign(isDebit: debit)
                + OperationPresentation.formatMinorUnits(minor)
            return OperationRowModel(
                id: id, title: title, subtitle: label,
                iconName: OperationPresentation.icon(kind: kind),
                amountText: amount, currency: "CUP", dateText: date,
                spoken: [title, label, "\(amount) CUP", date].joined(separator: ", ")
            )
        }
        return CardActivityData(
            availableText: "$3395.15", availableCurrency: "CUP", bookText: "$3395.15 CUP",
            operations: [
                row(0, .transfer, debit: true, minor: 95_000, date: "14/10/26"),
                row(1, .mobileTopup, debit: true, minor: 32_400, date: "14/10/26"),
                row(2, .payroll, debit: false, minor: 1_708_800, date: "08/10/26"),
                row(3, .posPayment, debit: true, minor: 12_550, date: "07/10/26"),
                row(4, .withdrawal, debit: true, minor: 500_000, date: "06/10/26"),
                row(5, .merchantPayment, debit: true, minor: 8_975, date: "05/10/26", alias: "Tienda La Esquina"),
                row(6, .merchantPayment, debit: true, minor: 4_000, date: "04/10/26"),
                row(7, .etecsaPayment, debit: true, minor: 21_000, date: "03/10/26"),
                row(8, .cardActivation, debit: false, minor: 0, date: "01/10/26"),
            ],
            skipped: 0
        )
    }()

    static let noOperations = CardActivityData(
        availableText: sample.availableText, availableCurrency: sample.availableCurrency,
        bookText: sample.bookText, operations: [], skipped: 0)

    /// Saldos ilegibles y operaciones omitidas: la pantalla debe seguir siendo útil.
    static let partial = CardActivityData(
        availableText: nil, availableCurrency: "CUP", bookText: nil,
        operations: Array(sample.operations.prefix(2)), skipped: 2)
}
