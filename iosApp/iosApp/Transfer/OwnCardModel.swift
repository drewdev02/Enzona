import Foundation
import SharedLogic

private extension BankCard {
    /// La tarjeta como se ofrece en los selectores: `BANMET 6856`, su moneda y el logo de su banco.
    func choice() -> CardChoice {
        CardChoice(id: id, label: label, currency: currency, logoName: logo)
    }

    /// `BANMET 6856`.
    var label: String {
        OperationPresentation.cardDetailLabel(skinName: skin.name, bankName: bankName, last4: last4)
    }

    /// Logo del banco, o el avatar neutro si no se reconoce.
    var logo: String { TransferPresentation.logo(brand: BankBrandKt.bankBrandOf(bank: bankCode).name) }
}

private extension OwnCardTransferUiStateEditing {
    func formData() -> OwnCardFormData {
        OwnCardFormData(
            cards: cards.map { $0.choice() }, sourceId: sourceId, destinationId: destinationId,
            amountText: amountText, description: description_,
            amountError: problems.amount.map { OwnCardPresentation.amountProblem($0.name) },
            destinationError: problems.destinationMissing, descriptionError: problems.descriptionMissing
        )
    }
}

private extension OwnCardTransferUiStateFinished {
    /// El comprobante de lo que pasó, con los mismos hechos que `toDetail()` de Android.
    func detailData() -> TransferDetailData {
        var outcomeName = "NOT_SENT"
        var receipt: TransferReceipt?
        var cause: String?
        var httpStatus: Int?
        var serverMessage: String?
        switch outcome {
        case let accepted as TransferOutcomeAccepted:
            outcomeName = "ACCEPTED"
            receipt = accepted.receipt
        case let rejected as TransferOutcomeRejected:
            outcomeName = "REJECTED"
            cause = rejected.cause.name
            httpStatus = rejected.httpStatus.map { $0.intValue }
            serverMessage = rejected.serverMessage
        case is TransferOutcomeUnknown:
            outcomeName = "UNKNOWN"
        case let notSent as TransferOutcomeNotSent:
            cause = notSent.reason.name
        default:
            break
        }
        let result = OwnCardPresentation.result(
            outcome: outcomeName, receiptStatus: receipt?.status.name, statusText: receipt?.statusText, cause: cause,
            httpStatus: httpStatus, serverMessage: serverMessage)
        let wallClock = receipt?.createdAt.map {
            (year: Int($0.year), month: Int($0.monthNumber), day: Int($0.dayOfMonth), hour: Int($0.hour), minute: Int($0.minute))
        }
        return TransferDetailData(
            kind: result.kind, statusTitle: result.title, note: result.detail,
            mainText: destination.label, destinationLogo: destination.logo,
            amountText: OwnCardPresentation.amountPlain(minorUnits: request.amount.minorUnits, currency: request.amount.currency),
            dateText: receipt.flatMap { OwnCardPresentation.dateText(wallClock: wallClock, receivedText: $0.createdAtText) },
            rows: OwnCardPresentation.rows(operationCode: receipt?.operationCode, description: request.description_),
            sourceLogo: source.logo, sourceHolder: source.holder,
            sourceSuffix: OwnCardPresentation.sourceSuffix(last4: source.last4, currency: source.currency),
            qrContent: receipt?.signature
        )
    }
}

/// Envuelve el OwnCardTransferViewModel de Kotlin para SwiftUI (puente `watchState`, en lugar de SKIE).
/// Un ViewModel por pantalla: `deinit` lo limpia. La contraseña no pasa por aquí más que hacia `submit`.
@MainActor
final class OwnCardModel: ObservableObject {
    @Published var phase: OwnCardPhase = .loading

    private let holder: ViewModelHolder<OwnCardTransferViewModel>
    private var vm: OwnCardTransferViewModel { holder.viewModel }
    private var subscription: Cancellable?

    init() {
        holder = KoinHelper.shared.ownCardTransferViewModelHolder()
        subscription = holder.viewModel.watchState { [weak self] state in self?.apply(state) }
    }

    deinit {
        subscription?.cancel()
        holder.clear()
    }

    private func apply(_ state: OwnCardTransferUiState) {
        switch state {
        case let editing as OwnCardTransferUiStateEditing:
            phase = .editing(editing.formData())
        case let confirming as OwnCardTransferUiStateConfirming:
            let request = confirming.request
            let amount = OwnCardPresentation.moneyShown(minorUnits: request.amount.minorUnits, currency: request.amount.currency)
            phase = .confirming(OwnCardConfirmData(
                form: confirming.form.formData(),
                question: OwnCardPresentation.confirmation(
                    amount: amount, source: confirming.source.label, destination: confirming.destination.label,
                    destinationCurrency: confirming.destination.currency, exchangeApplies: confirming.exchangeApplies),
                passwordError: confirming.notSent.map { OwnCardPresentation.notSent($0.name) }
            ))
        case is OwnCardTransferUiStateSubmitting:
            phase = .submitting
        case let finished as OwnCardTransferUiStateFinished:
            phase = .finished(finished.detailData())
        case let error as OwnCardTransferUiStateLoadError:
            phase = .loadError(error.message)
        case is OwnCardTransferUiStateNeedsTwoCards:
            phase = .needsTwoCards
        default:
            phase = .loading
        }
    }

    func load() { vm.load() }
    func selectSource(_ id: String) { vm.selectSource(cardId: id) }
    func selectDestination(_ id: String) { vm.selectDestination(cardId: id) }
    func setAmount(_ text: String) { vm.onAmountChange(text: text) }
    func setDescription(_ text: String) { vm.onDescriptionChange(text: text) }
    func review() { vm.review() }
    func back() { vm.back() }
    func submit(_ password: String) { vm.submit(password: password) }
    func editAgain() { vm.editAgain() }
}
