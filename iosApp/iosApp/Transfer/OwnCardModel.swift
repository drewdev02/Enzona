import Foundation
import SharedLogicSwift

private extension OwnCardTransferUiStateEditing {
    func formData() -> OwnCardFormData {
        OwnCardFormData(
            cards: cards.map { $0.choice() }, sourceId: sourceId, destinationId: destinationId,
            amountText: amountText, description: descriptionText,
            amountError: problems.amount.map { OwnCardPresentation.amountProblem(EzAmountProblem($0)) },
            destinationError: problems.destinationMissing, descriptionError: problems.descriptionMissing
        )
    }
}

private extension OwnCardTransferUiStateFinished {
    /// El comprobante: el destino es la tarjeta (banco y número), como `toDetail()` de Android.
    func detailData() -> TransferDetailData {
        TransferDetailBuilder.make(
            outcome: outcome, kind: kind, request: request, source: source,
            mainText: destination.label, destinationLogo: destination.logo)
    }
}

/// Envuelve el OwnCardTransferViewModel de Kotlin para SwiftUI (observa `state` con `asAsyncSequence()` de Swift Export).
/// Un ViewModel por pantalla: `deinit` lo limpia. La contraseña no pasa por aquí más que hacia `submit`.
@MainActor
final class OwnCardModel: ObservableObject {
    @Published var phase: OwnCardPhase = .loading

    private let holder: OwnCardTransferViewModelHolder
    private var vm: OwnCardTransferViewModel { holder.viewModel }
    private var observation: Task<Void, Never>?

    init() {
        holder = KoinHelper.shared.ownCardTransferViewModelHolder()
        let state = holder.viewModel.state
        observation = Task { [weak self] in
            do {
                for try await value in state.asAsyncSequence() { self?.apply(value) }
            } catch {}
        }
    }

    deinit {
        observation?.cancel()
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
                passwordError: confirming.notSent.map { OwnCardPresentation.notSent(EzNotSentReason($0)) }
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
