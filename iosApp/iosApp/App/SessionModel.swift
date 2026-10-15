import Foundation
import SharedLogic

/// Observa la sesión de la app (`SessionRepository`, vive todo el proceso) para decidir
/// qué se muestra: login o la zona autenticada. No es dueño de la sesión, solo la refleja.
@MainActor
final class SessionModel: ObservableObject {
    enum Phase { case loggedOut(expired: Bool), loggedIn }

    @Published private(set) var phase: Phase

    private var subscription: Cancellable?

    init() {
        phase = Self.phase(of: KoinHelper.shared.session.state.value)
        subscription = KoinHelper.shared.watchSession { [weak self] state in
            self?.phase = Self.phase(of: state)
        }
    }

    deinit { subscription?.cancel() }

    private static func phase(of state: Any?) -> Phase {
        if let out = state as? SessionStateLoggedOut { return .loggedOut(expired: out.expired) }
        return .loggedIn
    }

    func logout() { KoinHelper.shared.session.logout() }
}
