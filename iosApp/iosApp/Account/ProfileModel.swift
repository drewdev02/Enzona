import Foundation
import SharedLogicSwift

/// Envuelve el AccountViewModel de Kotlin para SwiftUI (observa `state` con `asAsyncSequence()` de Swift Export).
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
    private var observation: Task<Void, Never>?

    init() {
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

    private func apply(_ state: AccountUiState) {
        switch state.sealedType() {
        case .content(let content): phase = .content(content.value.account)
        case .error(let error): phase = .error(error.value.message)
        case .loading: phase = .loading
        }
    }

    func load() { vm.load() }
}
