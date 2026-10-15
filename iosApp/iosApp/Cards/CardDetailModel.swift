import Foundation
import SharedLogic

extension CardActivity {
    /// Traduce el dominio a lo que pinta la pantalla. Aquí viven los textos y el formato (el dominio
    /// solo guarda hechos).
    func toData() -> CardActivityData {
        func symbol(_ money: Money?) -> String? {
            money.map { "$" + OperationPresentation.formatMinorUnits($0.minorUnits) }
        }
        let currencyText = OperationPresentation.displayCurrency(currency)
        let rows: [OperationRowModel] = operations.enumerated().map { index, operation in
            let isDebit = operation.direction == OperationDirection.debit
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

/// Envuelve el CardDetailViewModel de Kotlin para SwiftUI (puente `watchState`, en lugar de SKIE).
/// Un ViewModel por pantalla: `deinit` lo limpia y cancela sus cargas en vuelo.
@MainActor
final class CardDetailModel: ObservableObject {
    @Published var phase: CardDetailPhase = .loading

    private let holder: ViewModelHolder<CardDetailViewModel>
    private var vm: CardDetailViewModel { holder.viewModel }
    private var subscription: Cancellable?

    init(cardId: String) {
        holder = KoinHelper.shared.cardDetailViewModelHolder(cardId: cardId)
        subscription = holder.viewModel.watchState { [weak self] state in
            self?.apply(state)
        }
    }

    deinit {
        subscription?.cancel()
        holder.clear()
    }

    private func apply(_ state: CardDetailUiState) {
        switch state {
        case let content as CardDetailUiStateContent: phase = .content(content.activity.toData())
        case let error as CardDetailUiStateError: phase = .error(error.message)
        default: phase = .loading
        }
    }

    func load() { vm.load() }
}
