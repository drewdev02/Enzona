import Foundation
import SharedLogic

/// Envuelve el LoginViewModel de Kotlin para la pantalla de login (puente `watchState`,
/// en lugar de SKIE). La sesión en sí es de `SessionModel`; esto solo es el intento de login.
@MainActor
final class LoginModel: ObservableObject {
    enum Phase { case idle, authenticating, error(String) }

    @Published var phase: Phase = .idle
    @Published var authorizeURL: URL?
    @Published var showSheet = false

    /// Un ViewModel por pantalla: `deinit` lo limpia. El PKCE pendiente vive en el repositorio.
    private let holder = KoinHelper.shared.loginViewModelHolder()
    private var vm: LoginViewModel { holder.viewModel }
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

    private func apply(_ state: LoginUiState) {
        switch state {
        case is LoginUiStateAuthenticating: phase = .authenticating
        case let e as LoginUiStateError: phase = .error(e.message)
        default: phase = .idle
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
