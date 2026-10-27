import Foundation
import SharedLogicSwift

/// Envuelve el LoginViewModel de Kotlin para la pantalla de login (observa `state` con `asAsyncSequence()` de Swift Export). La sesión en sí es de `SessionModel`; esto solo es el intento de login.
@MainActor
final class LoginModel: ObservableObject {
    enum Phase { case idle, authenticating, error(String) }

    @Published var phase: Phase = .idle
    @Published var authorizeURL: URL?
    @Published var showSheet = false

    /// Un ViewModel por pantalla: `deinit` lo limpia. El PKCE pendiente vive en el repositorio.
    private let holder = KoinHelper.shared.loginViewModelHolder()
    private var vm: LoginViewModel { holder.viewModel }
    private var observation: Task<Void, Never>?

    init() {
        let state = holder.viewModel.state
        observation = Task { [weak self] in
            do {
                for try await value in state.asAsyncSequence() {
                    self?.apply(value)
                }
            } catch {}
        }
    }

    deinit {
        observation?.cancel()
        holder.clear()
    }

    private func apply(_ state: LoginUiState) {
        switch state.sealedType() {
        case .authenticating: phase = .authenticating
        case .error(let e): phase = .error(e.value.message)
        case .idle: phase = .idle
        }
    }

    func login() {
        if let url = URL(string: vm.buildAuthorizeUrl()) {
            authorizeURL = url
            showSheet = true
        }
    }

    func onCode(_ code: String) {
        showSheet = false
        vm.onRedirectCode(code: code)
    }

    func onError(_ message: String) {
        showSheet = false
        vm.onAuthFailed(message: message)
    }
}
