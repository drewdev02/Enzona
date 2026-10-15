import Foundation

/// Una tarjeta elegible en los selectores, ya lista para pintar. Modelo propio de la UI (sin el framework Kotlin)
/// para que las vistas se puedan previsualizar.
struct CardChoice: Identifiable, Equatable {
    let id: String
    /// `BANMET 6856`.
    let label: String
    let currency: String
    let logoName: String
}

struct OwnCardFormData: Equatable {
    let cards: [CardChoice]
    let sourceId: String
    let destinationId: String?
    let amountText: String
    let description: String
    let amountError: String?
    let destinationError: Bool
    let descriptionError: Bool

    var source: CardChoice? { cards.first { $0.id == sourceId } }
    var destination: CardChoice? { cards.first { $0.id == destinationId } }
}

struct OwnCardConfirmData: Equatable {
    let form: OwnCardFormData
    let question: String
    /// Por qué no salió el último intento (contraseña vacía o inutilizable, otra transferencia en curso).
    let passwordError: String?
}

/// El comprobante "Detalles de la transferencia", ya resuelto: logo y texto del destino, estado, importe, fecha, filas,
/// la tarjeta de origen y el QR.
struct TransferDetailData: Equatable {
    let kind: EzResultKind
    let statusTitle: String
    /// Aviso bajo el estado (pendiente, fallida, desconocida). La transferencia confirmada no lleva.
    let note: String?
    let mainText: String
    let destinationLogo: String
    /// `10.50 CUP`.
    let amountText: String
    let dateText: String?
    let rows: [OwnCardPresentation.Row]
    let sourceLogo: String
    let sourceHolder: String
    /// `(6856) CUP`.
    let sourceSuffix: String
    /// Lo que se dibuja como QR (la firma que emite el servidor), o nil si no vino.
    let qrContent: String?
}

enum OwnCardPhase: Equatable {
    case loading
    case loadError(String)
    case needsTwoCards
    case editing(OwnCardFormData)
    case confirming(OwnCardConfirmData)
    case submitting
    case finished(TransferDetailData)
}

extension OwnCardFormData {
    /// Datos INVENTADOS para previews.
    static let sampleCards = [
        CardChoice(id: "c1", label: "BANMET 6856", currency: "CUP", logoName: "bank_banmet"),
        CardChoice(id: "c2", label: "BANDEC 1234", currency: "CUP", logoName: "bank_bandec"),
        CardChoice(id: "c3", label: "BANDEC 9021", currency: "USD", logoName: "bank_bandec"),
    ]

    static let sample = OwnCardFormData(
        cards: sampleCards, sourceId: "c1", destinationId: "c2", amountText: "10.50", description: "Ahorro",
        amountError: nil, destinationError: false, descriptionError: false)

    static let withProblems = OwnCardFormData(
        cards: sampleCards, sourceId: "c1", destinationId: nil, amountText: "0", description: "",
        amountError: OwnCardPresentation.amountProblem(.zero), destinationError: true, descriptionError: true)
}

extension TransferDetailData {
    private static func make(outcome: EzOutcome, kind: EzResultKind, operation: String? = nil,
                             date: String? = nil, qr: String? = nil) -> TransferDetailData {
        let result = OwnCardPresentation.result(outcome: outcome, kind: kind)
        return TransferDetailData(
            kind: result.kind, statusTitle: result.title, note: result.detail, mainText: "BANDEC 1234",
            destinationLogo: "bank_bandec", amountText: OwnCardPresentation.amountPlain(minorUnits: 1_050, currency: "cup"),
            dateText: date, rows: OwnCardPresentation.rows(operationCode: operation, description: "Ahorro mensual"),
            sourceLogo: "bank_banmet", sourceHolder: "TITULAR DE PRUEBA",
            sourceSuffix: OwnCardPresentation.sourceSuffix(last4: "6856", currency: "cup"), qrContent: qr)
    }

    static let accepted = make(
        outcome: .accepted(status: .accepted, statusText: "Aceptada"), kind: .success, operation: "kf1jldsUqZ1o",
        date: OwnCardPresentation.dateText(wallClock: (2026, 9, 11, 11, 30), receivedText: nil), qr: "SIG-INVENTADA-0001")
    static let pending = make(
        outcome: .accepted(status: .pending, statusText: nil), kind: .pending, operation: "kf1jldsUqZ2p",
        date: OwnCardPresentation.dateText(wallClock: (2026, 9, 11, 11, 31), receivedText: nil))
    static let rejected = make(
        outcome: .rejected(cause: .refused, httpStatus: 400, serverMessage: "Saldo insuficiente"), kind: .failed)
    static let unknown = make(outcome: .unknown, kind: .unknown)
}
