import Foundation

/// Una transferencia en una lista, ya lista para pintar. Modelo propio de la UI (sin el framework Kotlin) para que
/// las vistas se puedan previsualizar.
struct TransferRowModel: Identifiable, Equatable {
    let id: String
    let title: String
    let subtitle: String
    let logoName: String
    /// Con signo, sin moneda ("-1400.00").
    let amountText: String
    let currency: String
    let amountTone: EzAmountTone
    let dateText: String
    let statusText: String
    let statusTone: Ez.Tone
    /// Lo que leen los lectores de pantalla: la fila entera como UN elemento.
    let spoken: String
}

/// Filtros del historial tal como los edita la UI: días `yyyy-MM-dd` y `status_code` (2222/2242/2224).
struct TransferFilterModel: Equatable {
    var startDay: String?
    var endDay: String?
    var statusCode: Int?

    static let none = TransferFilterModel()
}

struct PersonModel: Identifiable, Equatable {
    let id: String
    let displayName: String
    let username: String?
}

enum TransferMenuPhase {
    case loading
    case error(String)
    case content([TransferRowModel], skipped: Int)
}

struct TransferHistoryData {
    let rows: [TransferRowModel]
    let skipped: Int
    let hasMore: Bool
    let isLoadingMore: Bool
    let moreError: String?
}

enum TransferHistoryPhase {
    case loading
    case error(String)
    case content(TransferHistoryData)
}

enum FindAccountPhase {
    case idle
    case searching
    case found(PersonModel)
    case notFound
    case invalid(String)
    case error(String)
}

enum FriendsPhase {
    case loading
    case empty
    case error(String)
    case content([PersonModel])
}

extension TransferRowModel {
    /// Datos INVENTADOS para previews.
    static let samples: [TransferRowModel] = {
        func row(_ id: String, outgoing: Bool, minor: Int64, status: String, statusText: String? = nil,
                 from: String, to: String, toBrand: String, date: (Int, Int, Int, Int, Int)) -> TransferRowModel {
            let direction = outgoing ? "OUTGOING" : "INCOMING"
            let amount = TransferPresentation.amountText(direction: direction, minorUnits: minor)
            let statusLabel = TransferPresentation.statusLabel(status: status, statusText: statusText)
            let dateText = TransferPresentation.wallClock(year: date.0, month: date.1, day: date.2, hour: date.3, minute: date.4)
            return TransferRowModel(
                id: id, title: to, subtitle: from, logoName: TransferPresentation.logo(brand: toBrand),
                amountText: amount, currency: "CUP", amountTone: TransferPresentation.amountTone(direction: direction),
                dateText: dateText, statusText: statusLabel, statusTone: TransferPresentation.statusTone(status: status),
                spoken: TransferPresentation.spoken(title: to, subtitle: from, amount: amount, currency: "CUP",
                                                    status: statusLabel, date: dateText)
            )
        }
        return [
            row("1", outgoing: true, minor: 140_000, status: "ACCEPTED", from: "BANMET(6856)", to: "BPA(1234)", toBrand: "BPA", date: (2026, 10, 14, 16, 30)),
            row("2", outgoing: false, minor: 50_000, status: "ACCEPTED", from: "BANDEC(9021)", to: "BANMET(6856)", toBrand: "BANMET", date: (2026, 10, 12, 9, 5)),
            row("3", outgoing: true, minor: 25_000, status: "PENDING", from: "BANMET(6856)", to: "BICSA(4410)", toBrand: "BICSA", date: (2026, 10, 10, 0, 15)),
            row("4", outgoing: true, minor: 1_200_000, status: "FAILED", from: "BANMET(6856)", to: "CLASICA(7733)", toBrand: "CLASICA", date: (2026, 10, 9, 12, 0)),
            row("5", outgoing: true, minor: 9_950, status: "OTHER", statusText: "Reversada", from: "BANMET(6856)", to: "AIS(5502)", toBrand: "AIS", date: (2026, 10, 8, 18, 45)),
            row("6", outgoing: true, minor: 300_000, status: "ACCEPTED", from: "BANMET(6856)", to: "Ana Pérez", toBrand: "UNKNOWN", date: (2026, 10, 7, 11, 20)),
        ]
    }()
}

extension PersonModel {
    static let samples = [
        PersonModel(id: "u1", displayName: "Ana Pérez Gómez", username: "ana.perez"),
        PersonModel(id: "u2", displayName: "carlos87", username: nil),
        PersonModel(id: "u3", displayName: "María López", username: "maria.lopez"),
    ]
}
