import Foundation
import SharedLogic

/// Envuelve el AuthViewModel de Kotlin para SwiftUI, observando el StateFlow
/// con el puente `watchState` (reemplaza a SKIE mientras no haya soporte de Kotlin 2.4).
@MainActor
final class AuthModel: ObservableObject {
    enum Phase { case loggedOut, authenticating, loggedIn, error(String) }

    @Published var phase: Phase = .loggedOut
    @Published var authorizeURL: URL?
    @Published var showSheet = false

    private let vm = KoinHelper.shared.authViewModel
    private var subscription: Cancellable?

    init() {
        subscription = vm.watchState { [weak self] state in
            self?.apply(state)
        }
        vm.restore()
    }

    deinit { subscription?.cancel() }

    private func apply(_ state: AuthUiState) {
        switch state {
        case is AuthUiStateLoggedIn: phase = .loggedIn; showSheet = false
        case is AuthUiStateAuthenticating: phase = .authenticating
        case let e as AuthUiStateError: phase = .error(e.message)
        default: phase = .loggedOut
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

    func logout() { vm.logout() }
}
