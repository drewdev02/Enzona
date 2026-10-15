import Foundation
import SharedLogic

// LA frontera Kotlin -> Swift de los enums del dominio. Es el único sitio que lee `.name`: el resto del código de iOS
// recibe los espejos tipados de `EzKinds.swift`.

extension EzTransferDirection { init(_ kotlin: TransferDirection) { self.init(kotlinName: kotlin.name) } }
extension EzTransferStatus { init(_ kotlin: TransferStatus) { self.init(kotlinName: kotlin.name) } }
extension EzBankBrand { init(_ kotlin: BankBrand) { self.init(kotlinName: kotlin.name) } }
extension EzOperationKind { init(_ kotlin: OperationKind) { self.init(kotlinName: kotlin.name) } }
extension EzCardSkin { init(_ kotlin: CardSkin) { self.init(kotlinName: kotlin.name) } }
extension EzResultKind { init(_ kotlin: TransferResultKind) { self.init(kotlinName: kotlin.name) } }
extension EzRejectionCause { init(_ kotlin: RejectionCause) { self.init(kotlinName: kotlin.name) } }
extension EzNotSentReason { init(_ kotlin: NotSentReason) { self.init(kotlinName: kotlin.name) } }
extension EzAmountProblem { init(_ kotlin: AmountProblem) { self.init(kotlinName: kotlin.name) } }
extension EzInvalidQuery { init(_ kotlin: InvalidQuery) { self.init(kotlinName: kotlin.name) } }

extension EzOutcome {
    init(_ outcome: TransferOutcome) {
        switch outcome {
        case let accepted as TransferOutcomeAccepted:
            self = .accepted(status: EzTransferStatus(accepted.receipt.status), statusText: accepted.receipt.statusText)
        case let rejected as TransferOutcomeRejected:
            self = .rejected(
                cause: EzRejectionCause(rejected.cause), httpStatus: rejected.httpStatus.map { $0.intValue },
                serverMessage: rejected.serverMessage)
        case let notSent as TransferOutcomeNotSent:
            self = .notSent(EzNotSentReason(notSent.reason))
        default:
            // `TransferOutcomeUnknown` (y cualquier variante futura): no se afirma nada del dinero.
            self = .unknown
        }
    }
}
