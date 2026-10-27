import Foundation
import SharedLogicSwift

extension CardActivity {
    /// Traduce el dominio a lo que pinta la pantalla. Aquí viven los textos y el formato (el dominio
    /// solo guarda hechos).
    func toData() -> CardActivityData {
        func symbol(_ money: Money?) -> String? {
            money.map { "$" + OperationPresentation.formatMinorUnits($0.minorUnits) }
        }
        let currencyText = OperationPresentation.displayCurrency(currency)
        let rows: [OperationRowModel] = operations.enumerated().map { index, operation in
            let isDebit = operation.direction == .DEBIT
            let kind = EzOperationKind(operation.kind)
            let title = OperationPresentation.title(kind: kind, merchantAlias: operation.merchantAlias)
            let label = OperationPresentation.label(isDebit: isDebit)
            // El importe llega como magnitud; el signo sale del sentido de la operación.
            var magnitude = OperationPresentation.formatMinorUnits(operation.amount.minorUnits)
            if magnitude.hasPrefix("-") { magnitude.removeFirst() }
            let amount = OperationPresentation.sign(isDebit: isDebit) + magnitude
            let shownCurrency = OperationPresentation.displayCurrency(operation.amount.currency)
            var spokenParts = [title, label, "\(amount) \(shownCurrency)"]
            if !operation.dateText.isEmpty { spokenParts.append(operation.dateText) }
            return OperationRowModel(
                id: index, title: title, subtitle: label,
                iconName: OperationPresentation.icon(kind: kind),
                amountText: amount, currency: shownCurrency, dateText: operation.dateText,
                spoken: spokenParts.joined(separator: ", ")
            )
        }
        return CardActivityData(
            availableText: symbol(available),
            availableCurrency: available.map { OperationPresentation.displayCurrency($0.currency) } ?? currencyText,
            bookText: balance.flatMap { money in
                symbol(money).map { $0 + " " + OperationPresentation.displayCurrency(money.currency) }
            },
            operations: rows,
            skipped: Int(skippedOperations)
        )
    }
}

/// Envuelve el CardDetailViewModel de Kotlin para SwiftUI (observa `state` con `asAsyncSequence()` de Swift Export).
/// Un ViewModel por pantalla: `deinit` lo limpia y cancela sus cargas en vuelo.
@MainActor
final class CardDetailModel: ObservableObject {
    @Published var phase: CardDetailPhase = .loading

    private let holder: CardDetailViewModelHolder
    private var vm: CardDetailViewModel { holder.viewModel }
    private var observation: Task<Void, Never>?

    init(cardId: String) {
        holder = KoinHelper.shared.cardDetailViewModelHolder(cardId: cardId)
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

    private func apply(_ state: CardDetailUiState) {
        switch state.sealedType() {
        case .content(let content): phase = .content(content.value.activity.toData())
        case .error(let error): phase = .error(error.value.message)
        case .loading: phase = .loading
        }
    }

    func load() { vm.load() }
}
