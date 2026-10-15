import Foundation
import SharedLogic

extension TransferSummary {
    /// Traduce el dominio a lo que pinta una fila. Aquí viven los textos y el formato (el dominio solo guarda hechos).
    func toRow() -> TransferRowModel {
        func label(_ party: TransferParty) -> String {
            TransferPresentation.partyLabel(name: party.name, username: party.username, bank: party.bank, last4: party.last4)
        }
        let title = label(recipient)
        let subtitle = label(source)
        let directionKind = EzTransferDirection(direction)
        let statusKind = EzTransferStatus(status)
        let amount = TransferPresentation.amountText(direction: directionKind, minorUnits: self.amount.minorUnits)
        let currency = OperationPresentation.displayCurrency(self.amount.currency)
        let dateText: String
        if let at = createdAt {
            dateText = TransferPresentation.wallClock(
                year: Int(at.year), month: Int(at.monthNumber), day: Int(at.dayOfMonth),
                hour: Int(at.hour), minute: Int(at.minute))
        } else {
            dateText = createdAtText ?? ""
        }
        let statusLabel = TransferPresentation.statusLabel(status: statusKind, statusText: statusText)
        return TransferRowModel(
            id: id, title: title, subtitle: subtitle,
            logoName: TransferPresentation.logo(brand: EzBankBrand(BankBrandKt.bankBrandOf(bank: recipient.bank))),
            amountText: amount, currency: currency, amountTone: TransferPresentation.amountTone(direction: directionKind),
            dateText: dateText, statusText: statusLabel, statusTone: TransferPresentation.statusTone(status: statusKind),
            spoken: TransferPresentation.spoken(title: title, subtitle: subtitle, amount: amount, currency: currency,
                                                status: statusLabel, date: dateText)
        )
    }
}

extension TransferFilterModel {
    var domain: TransferFilter {
        TransferFilter.companion.fromIso(startDay: startDay, endDay: endDay, status: status?.kotlin)
    }

    init(_ filter: TransferFilter) {
        self.init(
            startDay: filter.startDate.map { "\($0)" }, endDay: filter.endDate.map { "\($0)" },
            status: filter.status.flatMap { TransferStatusFilter(kotlin: $0) })
    }
}

/// La única traducción entre el estado filtrable de la UI y el `TransferStatus` de Kotlin.
private extension TransferStatusFilter {
    var kotlin: TransferStatus {
        switch self {
        case .accepted: return TransferStatus.accepted
        case .pending: return TransferStatus.pending
        case .failed: return TransferStatus.failed
        }
    }

    /// `nil` si el estado de Kotlin no se puede filtrar (`OTHER`).
    init?(kotlin status: TransferStatus) {
        guard let match = TransferStatusFilter.allCases.first(where: { $0.kotlin == status }) else { return nil }
        self = match
    }
}

/// Envuelve el TransferMenuViewModel de Kotlin para SwiftUI (puente `watchState`, en lugar de SKIE).
/// Un ViewModel por pantalla: `deinit` lo limpia y cancela sus cargas en vuelo.
@MainActor
final class TransferMenuModel: ObservableObject {
    @Published var phase: TransferMenuPhase = .loading

    private let holder: ViewModelHolder<TransferMenuViewModel>
    private var subscription: Cancellable?

    init() {
        holder = KoinHelper.shared.transferMenuViewModelHolder()
        subscription = holder.viewModel.watchState { [weak self] state in self?.apply(state) }
    }

    deinit {
        subscription?.cancel()
        holder.clear()
    }

    private func apply(_ state: TransferMenuUiState) {
        switch state {
        case let content as TransferMenuUiStateContent:
            phase = .content(content.recent.map { $0.toRow() }, skipped: Int(content.skipped))
        case let error as TransferMenuUiStateError: phase = .error(error.message)
        default: phase = .loading
        }
    }

    func load() { holder.viewModel.load() }
}

@MainActor
final class TransferHistoryModel: ObservableObject {
    @Published var phase: TransferHistoryPhase = .loading
    /// El filtro vigente (el del último estado recibido, o el pedido mientras carga).
    @Published var filter = TransferFilterModel.none

    private let holder: ViewModelHolder<TransferHistoryViewModel>
    private var subscription: Cancellable?

    init() {
        holder = KoinHelper.shared.transferHistoryViewModelHolder()
        subscription = holder.viewModel.watchState { [weak self] state in self?.apply(state) }
    }

    deinit {
        subscription?.cancel()
        holder.clear()
    }

    private func apply(_ state: TransferHistoryUiState) {
        switch state {
        case let content as TransferHistoryUiStateContent:
            filter = TransferFilterModel(content.filter)
            phase = .content(TransferHistoryData(
                rows: content.items.map { $0.toRow() }, skipped: Int(content.skipped), hasMore: content.hasMore,
                isLoadingMore: content.isLoadingMore, moreError: content.moreError))
        case let error as TransferHistoryUiStateError:
            filter = TransferFilterModel(error.filter)
            phase = .error(error.message)
        default: phase = .loading
        }
    }

    func load() { holder.viewModel.load() }
    func loadMore() { holder.viewModel.loadMore() }

    func apply(filter newFilter: TransferFilterModel) {
        filter = newFilter
        holder.viewModel.applyFilter(newFilter: newFilter.domain)
    }
}

@MainActor
final class FindAccountModel: ObservableObject {
    @Published var phase: FindAccountPhase = .idle

    private let holder: ViewModelHolder<FindRecipientViewModel>
    private var subscription: Cancellable?

    init() {
        holder = KoinHelper.shared.findRecipientViewModelHolder()
        subscription = holder.viewModel.watchState { [weak self] state in self?.apply(state) }
    }

    deinit {
        subscription?.cancel()
        holder.clear()
    }

    private func apply(_ state: FindRecipientUiState) {
        switch state {
        case let found as FindRecipientUiStateFound:
            phase = .found(PersonModel(
                id: found.recipient.accountUuid, displayName: found.recipient.displayName,
                username: found.recipient.username))
        case is FindRecipientUiStateNotFound: phase = .notFound
        case let invalid as FindRecipientUiStateInvalid: phase = .invalid(Self.message(invalid.reason))
        case let error as FindRecipientUiStateError: phase = .error(error.message)
        case is FindRecipientUiStateSearching: phase = .searching
        default: phase = .idle
        }
    }

    private static func message(_ reason: InvalidQuery) -> String {
        TransferPresentation.invalidQuery(EzInvalidQuery(reason))
    }

    func search(_ query: String) { holder.viewModel.search(query: query) }
    func reset() { holder.viewModel.reset() }
}

@MainActor
final class FriendsModel: ObservableObject {
    @Published var phase: FriendsPhase = .loading

    private let holder: ViewModelHolder<FriendsViewModel>
    private var subscription: Cancellable?

    init() {
        holder = KoinHelper.shared.friendsViewModelHolder()
        subscription = holder.viewModel.watchState { [weak self] state in self?.apply(state) }
    }

    deinit {
        subscription?.cancel()
        holder.clear()
    }

    private func apply(_ state: FriendsUiState) {
        switch state {
        case let content as FriendsUiStateContent:
            phase = .content(content.friends.map {
                PersonModel(id: $0.uuid, displayName: $0.displayName, username: $0.name == nil ? nil : $0.username)
            })
        case is FriendsUiStateEmpty: phase = .empty
        case let error as FriendsUiStateError: phase = .error(error.message)
        default: phase = .loading
        }
    }

    func load() { holder.viewModel.load() }
}
