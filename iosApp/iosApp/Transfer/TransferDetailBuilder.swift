import Foundation
import SharedLogicSwift

// Frontera Kotlin -> Swift que comparten los formularios que mueven dinero: cómo se muestra una tarjeta y cómo se
// arma el comprobante de cualquier transferencia.

extension BankCard {
    /// `BANMET 6856`.
    var label: String {
        OperationPresentation.cardDetailLabel(skin: EzCardSkin(skin), bankName: bankName, last4: last4)
    }

    /// Logo del banco, o el avatar neutro si no se reconoce.
    var logo: String { TransferPresentation.logo(brand: EzBankBrand(CardsDomain.bankBrandOf(bank: bankCode))) }

    /// La tarjeta como se ofrece en los selectores: su texto, su moneda y el logo de su banco.
    func choice() -> CardChoice { CardChoice(id: id, label: label, currency: currency, logoName: logo) }
}

extension TransferRecipient {
    /// El destinatario tal cual lo dio el servidor, sin reinterpretar nada, para llevarlo por la navegación.
    func identity() -> RecipientIdentity {
        RecipientIdentity(accountUuid: accountUuid, username: username, displayName: displayName, avatar: avatar, currency: currency)
    }
}

extension RecipientIdentity {
    /// La conversión inversa, para crear el ViewModel de la pantalla con el destinatario ya verificado.
    func kotlin() -> TransferRecipient {
        TransferRecipient(accountUuid: accountUuid, username: username, displayName: displayName, avatar: avatar, currency: currency)
    }
}

enum TransferDetailBuilder {
    /// El comprobante de lo que pasó, con los mismos hechos que `buildDetail` de Android. [mainText] y
    /// [destinationLogo] son del destino: una tarjeta propia muestra su banco y número; una persona, su nombre.
    static func make(
        outcome: TransferOutcome, kind: TransferResultKind, request: TransferRequest, source: BankCard,
        mainText: String, destinationLogo: String
    ) -> TransferDetailData {
        let receipt = (outcome as? TransferOutcomeAccepted)?.receipt
        // El desenlace se convierte una vez; cómo se clasifica (`kind`) lo decide el dominio, no esta capa.
        let result = OwnCardPresentation.result(outcome: EzOutcome(outcome), kind: EzResultKind(kind))
        let wallClock = receipt?.createdAt.map {
            (year: Int($0.year), month: Int($0.monthNumber), day: Int($0.dayOfMonth), hour: Int($0.hour), minute: Int($0.minute))
        }
        return TransferDetailData(
            kind: result.kind, statusTitle: result.title, note: result.detail,
            mainText: mainText, destinationLogo: destinationLogo,
            amountText: OwnCardPresentation.amountPlain(minorUnits: request.amount.minorUnits, currency: request.amount.currency),
            dateText: receipt.flatMap { OwnCardPresentation.dateText(wallClock: wallClock, receivedText: $0.createdAtText) },
            rows: OwnCardPresentation.rows(operationCode: receipt?.operationCode, description: request.descriptionText),
            sourceLogo: source.logo, sourceHolder: source.holder,
            sourceSuffix: OwnCardPresentation.sourceSuffix(last4: source.last4, currency: source.currency),
            qrContent: receipt?.signature
        )
    }
}
