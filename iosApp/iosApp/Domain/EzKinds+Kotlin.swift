import Foundation
import SharedLogicSwift

// LA frontera Kotlin -> Swift de los enums del dominio. Es el único sitio que lee `.description` (el nombre del caso): el resto del código de iOS
// recibe los espejos tipados de `EzKinds.swift`.

extension EzTransferDirection { init(_ kotlin: TransferDirection) { self.init(kotlinName: kotlin.description) } }
extension EzTransferStatus { init(_ kotlin: TransferStatus) { self.init(kotlinName: kotlin.description) } }
extension EzBankBrand { init(_ kotlin: BankBrand) { self.init(kotlinName: kotlin.description) } }
extension EzOperationKind { init(_ kotlin: OperationKind) { self.init(kotlinName: kotlin.description) } }
extension EzCardSkin { init(_ kotlin: CardSkin) { self.init(kotlinName: kotlin.description) } }
extension EzResultKind { init(_ kotlin: TransferResultKind) { self.init(kotlinName: kotlin.description) } }
extension EzRejectionCause { init(_ kotlin: RejectionCause) { self.init(kotlinName: kotlin.description) } }
extension EzNotSentReason { init(_ kotlin: NotSentReason) { self.init(kotlinName: kotlin.description) } }
extension EzAmountProblem { init(_ kotlin: AmountProblem) { self.init(kotlinName: kotlin.description) } }
extension EzInvalidQuery { init(_ kotlin: InvalidQuery) { self.init(kotlinName: kotlin.description) } }

extension EzOutcome {
    init(_ outcome: TransferOutcome) {
        switch outcome.sealedType() {
        case .accepted(let accepted):
            let receipt = accepted.value.receipt
            self = .accepted(status: EzTransferStatus(receipt.status), statusText: receipt.statusText)
        case .rejected(let rejected):
            self = .rejected(
                cause: EzRejectionCause(rejected.value.cause), httpStatus: rejected.value.httpStatus.map { Int($0) },
                serverMessage: rejected.value.serverMessage)
        case .notSent(let notSent):
            self = .notSent(EzNotSentReason(notSent.value.reason))
        case .unknown:
            // No se afirma nada del dinero.
            self = .unknown
        }
    }
}
