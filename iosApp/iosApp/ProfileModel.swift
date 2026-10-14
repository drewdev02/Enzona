import Foundation
import SharedLogic

/// Envuelve el AccountViewModel de Kotlin para SwiftUI (puente `watchState`, en lugar de SKIE).
@MainActor
final class ProfileModel: ObservableObject {
    enum Phase {
        case loading
        case content(Account)
        case error(String)
    }

    @Published var phase: Phase = .loading

    /// Un ViewModel por pantalla: `deinit` lo limpia y cancela sus cargas en vuelo.
    private let holder = KoinHelper.shared.accountViewModelHolder()
    private var vm: AccountViewModel { holder.viewModel }
    private var subscription: Cancellable?

    init() {
        subscription = holder.viewModel.watchState { [weak self] state in
            self?.apply(state)
        }
    }

    deinit {
        subscription?.cancel()
        holder.clear()
    }

    private func apply(_ state: AccountUiState) {
        switch state {
        case let content as AccountUiStateContent: phase = .content(content.account)
        case let error as AccountUiStateError: phase = .error(error.message)
        default: phase = .loading
        }
    }

    func load() { vm.load() }
}
