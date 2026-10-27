import Foundation
import SharedLogicSwift

/// Observa la sesión de la app (`SessionRepository`, vive todo el proceso) para decidir
/// qué se muestra: login o la zona autenticada. No es dueño de la sesión, solo la refleja.
@MainActor
final class SessionModel: ObservableObject {
    enum Phase { case loggedOut(expired: Bool), loggedIn }

    @Published private(set) var phase: Phase

    private var observation: Task<Void, Never>?

    init() {
        let state = KoinHelper.shared.session.state
        phase = Self.phase(of: state.value)
        observation = Task { [weak self] in
            do {
                for try await value in state.asAsyncSequence() { self?.phase = Self.phase(of: value) }
            } catch {}
        }
    }

    deinit { observation?.cancel() }

    private static func phase(of state: any SessionState) -> Phase {
        switch state.sealedType() {
        case .loggedOut(let out): return .loggedOut(expired: out.value.expired)
        case .loggedIn: return .loggedIn
        }
    }

    func logout() { KoinHelper.shared.session.logout() }
}
