import Foundation
import SharedLogic

private extension RecipientTransferUiStateEditing {
    func formData() -> RecipientFormData {
        RecipientFormData(
            recipient: recipient.identity(), cards: cards.map { $0.choice() }, sourceId: sourceId,
            amountText: amountText, description: description_,
            amountError: amountProblem.map { OwnCardPresentation.amountProblem(EzAmountProblem($0)) }
        )
    }
}

private extension RecipientTransferUiStateFinished {
    /// El comprobante: el destino es la persona (su nombre y el avatar neutro), como `toDetail()` de Android.
    func detailData() -> TransferDetailData {
        TransferDetailBuilder.make(
            outcome: outcome, kind: kind, request: request, source: source,
            mainText: request.recipient.displayName, destinationLogo: TransferPresentation.logo(brand: .unknown))
    }
}

/// Envuelve el RecipientTransferViewModel de Kotlin para SwiftUI (puente `watchState`, en lugar de SKIE). Un ViewModel
/// por pantalla, creado con el destinatario ya verificado: `deinit` lo limpia. La contraseña solo pasa hacia `submit`.
@MainActor
final class RecipientModel: ObservableObject {
    @Published var phase: RecipientPhase = .loading

    private let holder: ViewModelHolder<RecipientTransferViewModel>
    private var vm: RecipientTransferViewModel { holder.viewModel }
    private var subscription: Cancellable?

    init(recipient: RecipientIdentity) {
        holder = KoinHelper.shared.recipientTransferViewModelHolder(recipient: recipient.kotlin())
        subscription = holder.viewModel.watchState { [weak self] state in self?.apply(state) }
    }

    deinit {
        subscription?.cancel()
        holder.clear()
    }

    private func apply(_ state: RecipientTransferUiState) {
        switch state {
        case let editing as RecipientTransferUiStateEditing:
            phase = .editing(editing.formData())
        case let confirming as RecipientTransferUiStateConfirming:
            let request = confirming.request
            let amount = OwnCardPresentation.moneyShown(minorUnits: request.amount.minorUnits, currency: request.amount.currency)
            phase = .confirming(RecipientConfirmData(
                form: confirming.form.formData(),
                question: RecipientPresentation.confirmation(
                    amount: amount, source: confirming.source.label, recipientName: request.recipient.displayName,
                    recipientCurrency: request.recipient.currency, exchangeApplies: confirming.exchangeApplies),
                passwordError: confirming.notSent.map { OwnCardPresentation.notSent(EzNotSentReason($0)) }
            ))
        case is RecipientTransferUiStateSubmitting:
            phase = .submitting
        case let finished as RecipientTransferUiStateFinished:
            phase = .finished(finished.detailData())
        case let error as RecipientTransferUiStateLoadError:
            phase = .loadError(error.message)
        case is RecipientTransferUiStateNoCards:
            phase = .noCards
        default:
            phase = .loading
        }
    }

    func load() { vm.load() }
    func selectSource(_ id: String) { vm.selectSource(cardId: id) }
    func setAmount(_ text: String) { vm.onAmountChange(text: text) }
    func setDescription(_ text: String) { vm.onDescriptionChange(text: text) }
    func review() { vm.review() }
    func back() { vm.back() }
    func submit(_ password: String) { vm.submit(password: password) }
    func editAgain() { vm.editAgain() }
}
