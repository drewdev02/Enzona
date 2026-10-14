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

    private let vm = KoinHelper.shared.accountViewModel
    private var subscription: Cancellable?

    init() {
        subscription = vm.watchState { [weak self] state in
            self?.apply(state)
        }
    }

    deinit { subscription?.cancel() }

    private func apply(_ state: AccountUiState) {
        switch state {
        case let content as AccountUiStateContent: phase = .content(content.account)
        case let error as AccountUiStateError: phase = .error(error.message)
        default: phase = .loading
        }
    }

    func load() { vm.load() }
}
