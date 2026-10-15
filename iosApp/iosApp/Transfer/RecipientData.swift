import Foundation

/// El destinatario ya verificado por el servidor, tal cual lo dio (no se reinterpreta por el camino): es lo que viaja
/// en la navegación y lo que el ViewModel envía. `currency` es la moneda de su cuenta si el servidor la dio.
struct RecipientIdentity: Hashable {
    let accountUuid: String
    let username: String
    let displayName: String
    let avatar: String?
    let currency: String?
}

/// "A una cuenta" / "A un amigo": formulario con el destinatario fijo, la tarjeta de origen, el importe y una
/// descripción opcional. Modelo propio de la UI (sin el framework Kotlin) para poder previsualizarlo.
struct RecipientFormData: Equatable {
    let recipient: RecipientIdentity
    let cards: [CardChoice]
    let sourceId: String
    let amountText: String
    let description: String
    let amountError: String?

    var source: CardChoice? { cards.first { $0.id == sourceId } }
}

struct RecipientConfirmData: Equatable {
    let form: RecipientFormData
    let question: String
    /// Por qué no salió el último intento (contraseña vacía o inutilizable, otra transferencia en curso).
    let passwordError: String?
}

enum RecipientPhase: Equatable {
    case loading
    case loadError(String)
    case noCards
    case editing(RecipientFormData)
    case confirming(RecipientConfirmData)
    case submitting
    case finished(TransferDetailData)
}

extension RecipientIdentity {
    /// Datos INVENTADOS para previews.
    static let sample = RecipientIdentity(
        accountUuid: "acc-uuid-ana", username: "ana.perez", displayName: "Ana Pérez Gómez", avatar: nil, currency: "cup")
}

extension RecipientFormData {
    static let sample = RecipientFormData(
        recipient: .sample, cards: OwnCardFormData.sampleCards, sourceId: "c1", amountText: "25", description: "Cena",
        amountError: nil)

    static let withProblem = RecipientFormData(
        recipient: .sample, cards: OwnCardFormData.sampleCards, sourceId: "c1", amountText: "0", description: "",
        amountError: OwnCardPresentation.amountProblem(.zero))
}

extension RecipientConfirmData {
    static let sample = RecipientConfirmData(
        form: .sample,
        question: RecipientPresentation.confirmation(
            amount: OwnCardPresentation.moneyShown(minorUnits: 2_500, currency: "cup"), source: "BANMET 6856",
            recipientName: "Ana Pérez Gómez", recipientCurrency: "cup", exchangeApplies: false),
        passwordError: nil)

    /// La cuenta de la otra persona es en USD y la tarjeta de origen en CUP: el servidor aplicará su tasa.
    static let withExchange = RecipientConfirmData(
        form: .sample,
        question: RecipientPresentation.confirmation(
            amount: OwnCardPresentation.moneyShown(minorUnits: 2_500, currency: "cup"), source: "BANMET 6856",
            recipientName: "Ana Pérez Gómez", recipientCurrency: "usd", exchangeApplies: true),
        passwordError: nil)
}
